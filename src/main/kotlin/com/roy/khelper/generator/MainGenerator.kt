package com.roy.khelper.generator

import com.intellij.codeInsight.actions.OptimizeImportsProcessor
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.roy.khelper.settings.PluginSettings
import java.nio.file.Paths
import org.jetbrains.kotlin.psi.*

object MainGenerator {

    fun generate(project: Project, solutionFilePath: String): String {
        val settings = PluginSettings.getInstance()
        val outputDirName = settings.state.outputDirectory.takeIf { it.isNotBlank() } ?: "output"

        val solutionPath = Paths.get(solutionFilePath)
        val isJava = solutionFilePath.endsWith(".java")
        val mainFileName = if (isJava) "Main.java" else "Main.kt"

        val basePath = Paths.get(project.basePath!!)
        val mainPath = basePath.resolve(outputDirName).resolve(mainFileName)

        return WriteAction.computeAndWait<String, RuntimeException> {
            val solutionFile =
                    VfsUtil.findFile(solutionPath, true)
                            ?: throw RuntimeException("Solution file not found: $solutionFilePath")

            val psiFile =
                    PsiManager.getInstance(project).findFile(solutionFile)
                            ?: throw RuntimeException(
                                    "Could not find PSI for file: $solutionFilePath"
                            )

            val mergedContent =
                    if (isJava) {
                        generateJavaContent(psiFile as com.intellij.psi.PsiJavaFile)
                    } else {
                        generateKotlinContent(project, psiFile as KtFile)
                    }

            // Write to a Main file
            val mainDir =
                    VfsUtil.createDirectoryIfMissing(mainPath.parent.toString())
                            ?: throw RuntimeException(
                                    "Failed to create directory: ${mainPath.parent}"
                            )

            val existingMain = mainDir.findChild(mainFileName)
            val targetFile =
                    if (existingMain != null) {
                        existingMain.setBinaryContent(mergedContent.toByteArray())
                        existingMain
                    } else {
                        val newFile = mainDir.createChildData(this, mainFileName)
                        newFile.setBinaryContent(mergedContent.toByteArray())
                        newFile
                    }

            // Access the PsiFile for the newly written file to run optimization
            val writtenPsiFile = PsiManager.getInstance(project).findFile(targetFile)
            if (writtenPsiFile != null) {
                // Optimize Imports
                try {
                    OptimizeImportsProcessor(project, writtenPsiFile).run()
                } catch (e: Exception) {
                    // Ignore optimization errors
                }
            }

            targetFile.path
        }
    }

    private fun generateJavaContent(psiFile: com.intellij.psi.PsiJavaFile): String {
        val project = psiFile.project
        val visitedFiles = HashSet<String>()
        val mergedFiles = ArrayList<com.intellij.psi.PsiJavaFile>()
        val mergedClassNames = HashSet<String>()
        val collectedImports = HashSet<String>()
        val collectedClasses = ArrayList<String>()

        // Pass 1: Recursive Dependency Discovery
        val queue = ArrayDeque<com.intellij.psi.PsiJavaFile>()
        queue.add(psiFile)
        visitedFiles.add(psiFile.virtualFile.path)

        while (queue.isNotEmpty()) {
            val currentFile = queue.removeFirst()
            mergedFiles.add(currentFile)

            // Collect top-level class names to filter imports later
            currentFile.classes.forEach { it.name?.let { name -> mergedClassNames.add(name) } }

            // Scan for dependencies via references (types, variables, etc.)
            PsiTreeUtil.collectElements(currentFile) {
                it is com.intellij.psi.PsiJavaCodeReferenceElement
            }
                    .forEach { elem ->
                        val ref = (elem as com.intellij.psi.PsiJavaCodeReferenceElement).resolve()
                        if (ref is com.intellij.psi.PsiClass) {
                            val containingFile = ref.containingFile
                            if (containingFile is com.intellij.psi.PsiJavaFile &&
                                            containingFile != currentFile &&
                                            containingFile.virtualFile?.path?.startsWith(
                                                    project.basePath!!
                                            ) == true &&
                                            !visitedFiles.contains(containingFile.virtualFile.path)
                            ) {
                                visitedFiles.add(containingFile.virtualFile.path)
                                queue.add(containingFile)
                            }
                        }
                    }
        }

        // Pass 2: Harvest Imports and Classes
        mergedFiles.forEach { file ->
            // Collect imports
            file.importList?.importStatements?.forEach {
                val importText = it.text
                if (importText.contains("problems.")) return@forEach

                // Filter out imports of classes that are being merged
                val ref = it.importReference?.resolve()
                if (ref is com.intellij.psi.PsiClass && mergedClassNames.contains(ref.name)) {
                    return@forEach
                }

                collectedImports.add(importText)
            }

            // Collect classes
            file.classes.forEach { psiClass ->
                var classText = psiClass.text

                if (file == psiFile &&
                                (psiClass.name == "JSolution" || psiClass.name == "KSolution")
                ) {
                    // Rename the main solution class to Main
                    // Use regex to be more robust with spacing and modifiers
                    classText =
                            classText.replace(
                                    Regex("""\bclass\s+(JSolution|KSolution)\b"""),
                                    "class Main"
                            )
                    // Ensure it's public
                    if (!classText.trim().startsWith("public")) {
                        classText = "public $classText"
                    }
                } else {
                    // Helper classes MUST NOT be public in the same file as Main
                    // Also handles interfaces, enums, etc.
                    classText = classText.replaceFirst("public class", "class")
                    classText = classText.replaceFirst("public interface", "interface")
                    classText = classText.replaceFirst("public enum", "enum")
                    classText = classText.replaceFirst("public @interface", "@interface")
                }
                collectedClasses.add(classText)
            }
        }

        return buildString {
            // Imports
            collectedImports.sorted().forEach { appendLine(it) }
            if (collectedImports.isNotEmpty()) appendLine()

            // Classes
            collectedClasses.forEach {
                appendLine(it)
                appendLine()
            }
        }
    }

    private fun generateKotlinContent(project: Project, psiFile: KtFile): String {
        // --- Dependency Analysis (2-Pass Approach) ---
        val visitedFiles = HashSet<String>() // Path -> Unit
        val mergedFiles = ArrayList<KtFile>()
        val mergedDeclarationNames = HashSet<String>()
        val collectedDeclarations = ArrayList<String>()
        val collectedImports = HashSet<String>()

        // Pass 1: Discovery & Collection of Merged Names
        val queue = ArrayDeque<KtFile>()
        queue.add(psiFile)
        visitedFiles.add(psiFile.virtualFile.path)

        while (queue.isNotEmpty()) {
            val currentFile = queue.removeFirst()
            mergedFiles.add(currentFile)

            // If it's a dependency file (not a solution file), collect its declaration names
            if (currentFile != psiFile) {
                currentFile.declarations.forEach { decl ->
                    if (decl is KtNamedDeclaration && decl.name != null) {
                        mergedDeclarationNames.add(decl.name!!)
                        // Also adding compiled text for merging later
                        collectedDeclarations.add(decl.text)
                    }
                }
            }

            // Scan for next dependencies
            PsiTreeUtil.collectElements(currentFile) { it is KtReferenceExpression }.forEach { elem
                ->
                val ref = elem.references.firstOrNull()?.resolve()
                if (ref != null) {
                    val containingFile = ref.containingFile
                    if (containingFile is KtFile &&
                                    containingFile != currentFile &&
                                    containingFile.virtualFile?.path?.startsWith(
                                            project.basePath!!
                                    ) == true &&
                                    !visitedFiles.contains(containingFile.virtualFile.path)
                    ) {
                        visitedFiles.add(containingFile.virtualFile.path)
                        queue.add(containingFile)
                    }
                }
            }
        }

        // Pass 2: Harvest Imports & Filter
        mergedFiles.forEach { file ->
            PsiTreeUtil.findChildrenOfType(file, KtImportDirective::class.java).forEach {
                val importText = it.text
                if (importText.contains("problems.")) return@forEach

                // Filter: If the imported name matches any merged declaration, skip it.
                val importedName = it.importedName?.identifier

                if (importedName != null && mergedDeclarationNames.contains(importedName)) {
                    // Skip project local ref
                } else {
                    collectedImports.add(importText)
                }
            }
        }

        // --- Construct Main Content ---
        val ktClass = PsiTreeUtil.findChildOfType(psiFile, KtClass::class.java)

        val solveBody: String
        val kSolutionHelpers: String

        if (ktClass != null) {
            // Legacy Mode: Class-based
            val solveMethod =
                    ktClass.declarations.filterIsInstance<KtFunction>().find { it.name == "solve" }
                            ?: throw RuntimeException(
                                    "solve() method not found inside KSolution class"
                            )
            solveBody = solveMethod.bodyExpression?.text ?: ""

            kSolutionHelpers =
                    ktClass.declarations
                            .filterIsInstance<KtFunction>()
                            .filter { it.name != "solve" }
                            .joinToString("\n\n") { it.text }
        } else {
            // New Mode: Top-level functions
            val mainFunction =
                    psiFile.declarations.filterIsInstance<KtFunction>().find { it.name == "main" }

            if (mainFunction != null) {
                solveBody = mainFunction.bodyExpression?.text ?: ""

                kSolutionHelpers =
                        psiFile.declarations
                                .filter {
                                    it != mainFunction &&
                                            (it is KtFunction ||
                                                    it is KtClassOrObject ||
                                                    it is KtProperty)
                                }
                                .joinToString("\n\n") { it.text }
            } else {
                throw RuntimeException(
                        "Neither KSolution class nor main() function found in ${psiFile.name}"
                )
            }
        }

        return buildString {
            // Imports
            collectedImports.sorted().forEach { appendLine(it) }
            if (collectedImports.isNotEmpty()) appendLine()

            // Helper Class/Functions (Dependencies)
            collectedDeclarations.forEach {
                appendLine(it)
                appendLine()
            }

            // KSolution Helpers (or top-level siblings)
            if (kSolutionHelpers.isNotBlank()) {
                appendLine(kSolutionHelpers)
                appendLine()
            }

            // Main Function
            appendLine("fun main() {")
            val bodyContent = solveBody.trim().removeSurrounding("{", "}").trimIndent()
            appendLine(bodyContent.prependIndent("    "))
            appendLine("}")
        }
    }

    fun getMainContent(project: Project, solutionFilePath: String): String {
        val isJava = solutionFilePath.endsWith(".java")
        val mainFileName = if (isJava) "Main.java" else "Main.kt"

        val mainPathStr = generate(project, solutionFilePath)
        val mainFile =
                VfsUtil.findFile(Paths.get(mainPathStr), true)
                        ?: throw RuntimeException("$mainFileName not found at $mainPathStr")

        return String(mainFile.contentsToByteArray())
    }
}
