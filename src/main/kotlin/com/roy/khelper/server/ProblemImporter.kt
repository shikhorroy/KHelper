package com.roy.khelper.server

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.roy.khelper.generator.KSolutionGenerator
import com.roy.khelper.generator.RunConfigurationGenerator
import com.roy.khelper.model.Problem
import com.roy.khelper.services.KHelperNotifications
import com.roy.khelper.services.ProblemManagerService
import com.roy.khelper.settings.PluginSettings
import java.nio.file.Files

/** Turns a received problem into a solution file, stored test cases and a run configuration. Runs on the EDT. */
object ProblemImporter {

    private val log = logger<ProblemImporter>()

    fun import(project: Project, problem: Problem) {
        if (project.isDisposed) return

        try {
            val language = PluginSettings.getInstance().state.preferredLanguage
            val solutionPath = KSolutionGenerator.resolveSolutionPath(project, problem, language)

            if (Files.exists(solutionPath)) {
                val answer = Messages.showYesNoDialog(
                    project,
                    "Solution file for '${problem.name}' already exists. Do you want to overwrite it?",
                    "File Already Exists",
                    Messages.getQuestionIcon(),
                )
                if (answer != Messages.YES) return
            }

            val solutionFile = KSolutionGenerator.generate(project, problem, language)
            ProblemManagerService.getInstance(project).addProblem(problem, solutionFile.path, language.name)
            RunConfigurationGenerator.createRunConfiguration(project, problem, solutionFile.path, language)
            FileEditorManager.getInstance(project).openFile(solutionFile, true)

            KHelperNotifications.info(
                project,
                "Successfully parsed: ${problem.name} (${problem.tests.size} test cases)",
                "Problem Received",
            )
        } catch (e: Exception) {
            log.warn("Failed to import problem '${problem.name}'", e)
            KHelperNotifications.error(project, "Failed to process problem: ${e.message}")
        }
    }
}
