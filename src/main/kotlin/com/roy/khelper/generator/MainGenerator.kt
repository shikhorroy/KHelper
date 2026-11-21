package com.roy.khelper.generator

import com.intellij.codeInsight.actions.OptimizeImportsProcessor
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiPackage
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.concurrency.AppExecutorUtil
import com.roy.khelper.services.KHelperNotifications
import com.roy.khelper.settings.PluginSettings
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtReferenceExpression
import java.io.IOException
import java.nio.file.Path
import java.util.concurrent.CancellationException

/**
 * Builds one submission file (Main.kt or Main.java) from a solution and every project file it uses.
 *
 * Reference resolving needs indexes and can be slow, so the merge runs in a background read action in smart mode.
 * Only the final file write runs on the EDT.
 */
object MainGenerator {

    private val log = logger<MainGenerator>()

    private val SOLUTION_CLASS_NAME = Regex("""\b(JSolution|KSolution)\b""")
    private val PUBLIC_TYPE_MODIFIER = Regex(
        """\bpublic\s+(?=(?:(?:final|abstract|static|sealed|non-sealed|strictfp)\s+)*(?:class|interface|enum|record|@interface)\b)"""
    )

    /**
     * Writes Main into the output directory, then calls [onDone] on the EDT with its text. Errors are shown as a
     * notification.
     */
    fun generate(project: Project, solutionFilePath: String, onDone: (String) -> Unit = {}) {
        ReadAction.nonBlocking<Result<String>> { buildContentOrError(project, solutionFilePath) }
            .inSmartMode(project)
            .withDocumentsCommitted(project)
            .expireWhen { project.isDisposed }
            .finishOnUiThread(ModalityState.nonModal()) { result ->
                result
                    .mapCatching { content -> writeMain(project, solutionFilePath, content) }
                    .onSuccess(onDone)
                    .onFailure { e ->
                        log.warn("Failed to generate Main for $solutionFilePath", e)
                        val fileName = PluginSettings.Language.fromFilePath(solutionFilePath).mainFileName
                        KHelperNotifications.error(project, "Failed to generate $fileName: ${e.message}")
                    }
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    /**
     * Wraps real errors so they reach the notification. Cancellation is rethrown: the platform cancels a
     * non-blocking read action when a write action starts, and then runs it again.
     */
    private fun buildContentOrError(project: Project, solutionFilePath: String): Result<String> =
        try {
            val file = LocalFileSystem.getInstance().findFileByPath(solutionFilePath)
                ?: throw IOException("Solution file not found: $solutionFilePath")
            val psiFile = PsiManager.getInstance(project).findFile(file)
                ?: throw IOException("Cannot read solution file: $solutionFilePath")
            Result.success(buildContent(project, psiFile))
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    /** The merged submission text. Call in a read action in smart mode. */
    internal fun buildContent(project: Project, solution: PsiFile): String = when (solution) {
        is PsiJavaFile -> mergeJava(project, solution)
        is KtFile -> mergeKotlin(project, solution)
        else -> throw IOException("Not a Kotlin or Java file: ${solution.name}")
    }

    private fun writeMain(project: Project, solutionFilePath: String, content: String): String {
        val basePath = project.basePath ?: throw IOException("Project has no base directory")
        val settings = PluginSettings.getInstance().state
        val mainFileName = PluginSettings.Language.fromFilePath(solutionFilePath).mainFileName
        val mainDir = Path.of(basePath, settings.outputDirectoryOrDefault())

        val mainFile = WriteCommandAction.writeCommandAction(project)
            .withName("Generate $mainFileName")
            .compute<VirtualFile, IOException> {
                val dir = VfsUtil.createDirectoryIfMissing(mainDir.toString())
                    ?: throw IOException("Failed to create directory: $mainDir")
                val file = dir.findChild(mainFileName) ?: dir.createChildData(this, mainFileName)
                VfsUtil.saveText(file, content)
                file
            }

        PsiManager.getInstance(project).findFile(mainFile)?.let { psi ->
            try {
                OptimizeImportsProcessor(project, psi).run()
            } catch (e: Exception) {
                // Unused imports do not break the submission, so a failed cleanup is not an error.
                log.info("Optimize imports failed for ${mainFile.path}", e)
            }
        }
        return FileDocumentManager.getInstance().getDocument(mainFile)?.text ?: content
    }

    /** Collects the solution file and, breadth first, every project file it references. */
    private inline fun <reified F : PsiFile> collectProjectFiles(
        project: Project,
        root: F,
        crossinline referencedFiles: (F) -> Sequence<PsiFile?>,
    ): List<F> {
        val basePath = project.basePath ?: return listOf(root)
        val visited = hashSetOf(root.virtualFile.path)
        val queue = ArrayDeque(listOf(root))
        val result = mutableListOf<F>()
        while (queue.isNotEmpty()) {
            val file = queue.removeFirst()
            result += file
            referencedFiles(file).forEach { target ->
                val path = target?.virtualFile?.path ?: return@forEach
                if (target is F && path.startsWith(basePath) && visited.add(path)) {
                    queue.addLast(target)
                }
            }
        }
        return result
    }

    private fun mergeJava(project: Project, solution: PsiJavaFile): String {
        val files = collectProjectFiles(project, solution) { file ->
            PsiTreeUtil.findChildrenOfType(file, PsiJavaCodeReferenceElement::class.java)
                .asSequence()
                .map { (it.resolve() as? PsiClass)?.containingFile }
        }
        val mergedClassNames = files.flatMap { file -> file.classes.mapNotNull { it.name } }.toSet()
        val mergedPackages = files.map { it.packageName }.toSet()

        val imports = sortedSetOf<String>()
        val classes = mutableListOf<String>()
        files.forEach { file ->
            file.importList?.importStatements?.forEach { statement ->
                val imported = statement.importReference?.resolve() as? PsiClass
                val qualifiedName = statement.qualifiedName
                //~ "import a.b.*" names the package itself, "import a.b.C" names a class in package a.b
                val importedPackage = if (statement.isOnDemand) qualifiedName else qualifiedName?.substringBeforeLast('.')
                val fromMergedPackage = importedPackage in mergedPackages
                if (!fromMergedPackage && imported?.name !in mergedClassNames) {
                    imports += statement.text
                }
            }
            file.classes.forEach { psiClass ->
                val isSolution = file == solution && psiClass.name == PluginSettings.Language.JAVA.className
                classes += if (isSolution) {
                    //~ The judge runs the public class Main. References to the old name are renamed too.
                    val text = psiClass.text.replace(SOLUTION_CLASS_NAME, "Main")
                    if (text.trimStart().startsWith("public")) text else "public $text"
                } else {
                    // Only one public top-level class is allowed in Main.java.
                    psiClass.text.replaceFirst(PUBLIC_TYPE_MODIFIER, "").replace(SOLUTION_CLASS_NAME, "Main")
                }
            }
        }

        return buildString {
            imports.forEach { appendLine(it) }
            if (imports.isNotEmpty()) appendLine()
            classes.forEach {
                appendLine(it)
                appendLine()
            }
        }
    }

    private fun mergeKotlin(project: Project, solution: KtFile): String {
        val files = collectProjectFiles(project, solution) { file ->
            PsiTreeUtil.findChildrenOfType(file, KtReferenceExpression::class.java)
                .asSequence()
                // Import and package lines name packages, not code. Real uses of imported code are found anyway.
                .filter { PsiTreeUtil.getParentOfType(it, KtImportDirective::class.java, KtPackageDirective::class.java) == null }
                .map { it.references.firstOrNull()?.resolve() }
                // A package has no file. In K2, asking for one throws.
                .map { target -> target?.takeIf { it !is PsiPackage && it !is PsiDirectory }?.containingFile }
        }
        val dependencies = files.filter { it != solution }
        val mergedNames = dependencies.flatMap { file ->
            file.declarations.filterIsInstance<KtNamedDeclaration>().mapNotNull { it.name }
        }.toSet()
        val mergedPackages = files.map { it.packageFqName.asString() }.toSet()

        val imports = sortedSetOf<String>()
        files.forEach { file ->
            PsiTreeUtil.findChildrenOfType(file, KtImportDirective::class.java).forEach { directive ->
                val fqName = directive.importedFqName
                //~ "import a.b.*" names the package itself, "import a.b.C" names a declaration in package a.b
                val importedPackage = (if (directive.isAllUnder) fqName else fqName?.parent())?.asString()
                val importedName = directive.importedName?.identifier
                if (importedPackage !in mergedPackages && importedName !in mergedNames) {
                    imports += directive.text
                }
            }
        }

        val (mainBody, solutionHelpers) = splitKotlinSolution(solution)

        return buildString {
            imports.forEach { appendLine(it) }
            if (imports.isNotEmpty()) appendLine()

            dependencies.forEach { file ->
                file.declarations.filterIsInstance<KtNamedDeclaration>().forEach {
                    appendLine(it.text)
                    appendLine()
                }
            }
            if (solutionHelpers.isNotBlank()) {
                appendLine(solutionHelpers)
                appendLine()
            }

            appendLine("fun main() {")
            appendLine(mainBody.trim().removeSurrounding("{", "}").trimIndent().prependIndent("    "))
            appendLine("}")
        }
    }

    /**
     * Returns the body of the entry point and the other declarations of the solution file. Supports a top-level
     * main() and the older style with a KSolution class and a solve() method.
     */
    private fun splitKotlinSolution(solution: KtFile): Pair<String, String> {
        val legacyClass = solution.declarations
            .filterIsInstance<KtClass>()
            .find { it.name == PluginSettings.Language.KOTLIN.className }

        if (legacyClass != null) {
            val functions = legacyClass.declarations.filterIsInstance<KtFunction>()
            val solve = functions.find { it.name == "solve" }
                ?: throw IOException("solve() method not found inside KSolution class")
            val helpers = functions.filter { it != solve }.joinToString("\n\n") { it.text }
            return (solve.bodyExpression?.text.orEmpty()) to helpers
        }

        val main = solution.declarations.filterIsInstance<KtFunction>().find { it.name == "main" }
            ?: throw IOException("Neither a KSolution class nor a main() function found in ${solution.name}")
        val helpers = solution.declarations
            .filter { it != main && (it is KtFunction || it is KtClassOrObject || it is KtProperty) }
            .joinToString("\n\n") { it.text }
        return (main.bodyExpression?.text.orEmpty()) to helpers
    }
}
