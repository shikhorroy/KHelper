package com.roy.khelper.generator

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiManager
import com.roy.khelper.model.Problem
import com.roy.khelper.settings.PluginSettings
import java.nio.file.Paths

object KSolutionGenerator {

    fun generate(project: Project, problem: Problem): String {
        val problemId = problem.generateId()
        val settings = PluginSettings.getInstance()
        val language = settings.state.preferredLanguage

        val rawTemplate = if (language == PluginSettings.Language.KOTLIN)
            settings.state.solutionTemplate
        else
            settings.state.javaSolutionTemplate

        // Prepare a template with variable substitution
        val className = if (language == PluginSettings.Language.KOTLIN) "KSolution" else "JSolution"
        val template =
            rawTemplate
                .replace($$"${PROBLEM_ID}", problemId)
                .replace($$"${PROBLEM_NAME}", problem.name)
                .replace($$"${TIME_LIMIT}", problem.timeLimit.toString())
                .replace($$"${MEMORY_LIMIT}", problem.memoryLimit.toString())
                .replace($$"${CLASS_NAME}", className)

        val filePath = resolveSolutionPath(project, problem)
        val fileName = if (language == PluginSettings.Language.KOTLIN) "KSolution.kt" else "JSolution.java"

        WriteAction.runAndWait<RuntimeException> {
            // Create directories
            val baseDir =
                VfsUtil.createDirectoryIfMissing(filePath.parent.toString())
                    ?: throw RuntimeException(
                        "Failed to create directory: ${filePath.parent}"
                    )

            // Create a file
            val file = baseDir.createChildData(this, fileName)
            file.setBinaryContent(template.toByteArray())

            // Refresh PSI
            PsiManager.getInstance(project).findFile(file)
        }

        return filePath.toString()
    }

    fun resolveSolutionPath(project: Project, problem: Problem): java.nio.file.Path {
        val problemId = problem.generateId()
        val settings = PluginSettings.getInstance()
        val language = settings.state.preferredLanguage

        val sourceRoot = if (language == PluginSettings.Language.KOTLIN) "kotlin" else "java"
        val extension = if (language == PluginSettings.Language.KOTLIN) "kt" else "java"
        val fileNameBase = if (language == PluginSettings.Language.KOTLIN) "KSolution" else "JSolution"

        return Paths.get(
            project.basePath!!,
            "src",
            "main",
            sourceRoot,
            "problems",
            problemId,
            "$fileNameBase.$extension"
        )
    }

    fun getClassName(problem: Problem): String {
        val settings = PluginSettings.getInstance()
        return if (settings.state.preferredLanguage == PluginSettings.Language.KOTLIN) "KSolution" else "JSolution"
    }

    fun getPackageName(problem: Problem): String {
        return "problems.${problem.generateId()}"
    }
}
