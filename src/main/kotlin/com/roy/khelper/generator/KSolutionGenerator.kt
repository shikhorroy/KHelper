package com.roy.khelper.generator

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.roy.khelper.model.Problem
import com.roy.khelper.settings.PluginSettings
import com.roy.khelper.settings.PluginSettings.Language
import java.io.IOException
import java.nio.file.Path

/** Writes the solution file for a problem from the user's template. */
object KSolutionGenerator {

    fun generate(project: Project, problem: Problem, language: Language): VirtualFile {
        val content = applyTemplate(PluginSettings.getInstance().state.templateFor(language), problem, language)
        val filePath = resolveSolutionPath(project, problem, language)

        return WriteCommandAction.writeCommandAction(project)
            .withName("Create Solution for ${problem.name}")
            .compute<VirtualFile, IOException> {
                val dir = VfsUtil.createDirectoryIfMissing(filePath.parent.toString())
                    ?: throw IOException("Failed to create directory: ${filePath.parent}")
                //~ Reuse the file on overwrite. createChildData() fails when the file already exists.
                val file = dir.findChild(language.fileName) ?: dir.createChildData(this, language.fileName)
                VfsUtil.saveText(file, content)
                file
            }
    }

    fun applyTemplate(template: String, problem: Problem, language: Language): String =
        template
            .replace($$"${PROBLEM_ID}", problem.generateId())
            .replace($$"${PROBLEM_NAME}", problem.name)
            .replace($$"${TIME_LIMIT}", problem.timeLimit.toString())
            .replace($$"${MEMORY_LIMIT}", problem.memoryLimit.toString())
            .replace($$"${CLASS_NAME}", language.className)

    fun resolveSolutionPath(project: Project, problem: Problem, language: Language): Path {
        val basePath = project.basePath ?: throw IOException("Project '${project.name}' has no base directory")
        return Path.of(basePath, "src", "main", language.sourceRoot, "problems", problem.generateId(), language.fileName)
    }

    /** The fully qualified class the test runner loads. Kotlin top-level code lives in the matching "Kt" class. */
    fun solutionClassName(problemId: String, language: Language): String = "problems.$problemId.${language.className}"
}
