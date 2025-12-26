package com.roy.khelper.server.listeners

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.roy.khelper.generator.KSolutionGenerator
import com.roy.khelper.generator.RunConfigurationGenerator
import com.roy.khelper.model.Problem
import com.roy.khelper.services.ProblemManagerService

class ProjectProblemListener(private val project: Project) : ProblemListener {

    override fun onProblemReceived(problem: Problem) {
        ApplicationManager.getApplication().invokeLater {
            try {
                // Check if file exists
                val filePath = KSolutionGenerator.resolveSolutionPath(project, problem)
                val file = filePath.toFile()

                if (file.exists()) {
                    val result =
                            com.intellij.openapi.ui.Messages.showYesNoDialog(
                                    project,
                                    "Solution file for '${problem.name}' already exists. Do you want to overwrite it?",
                                    "File Already Exists",
                                    com.intellij.openapi.ui.Messages.getQuestionIcon()
                            )

                    if (result == com.intellij.openapi.ui.Messages.NO) {
                        return@invokeLater
                    }
                }

                // Generate KSolution file
                val generatedPathString = KSolutionGenerator.generate(project, problem)

                // Store problem in manager
                val settings = com.roy.khelper.settings.PluginSettings.getInstance()
                val language = settings.state.preferredLanguage.name
                val problemManager = ProblemManagerService.getInstance(project)
                problemManager.addProblem(problem, generatedPathString, language)

                // Create Run Configuration
                RunConfigurationGenerator.createRunConfiguration(
                        project,
                        problem,
                        generatedPathString,
                        language
                )

                // Open file in editor
                val virtualFile =
                        com.intellij.openapi.vfs.LocalFileSystem.getInstance()
                                .refreshAndFindFileByPath(generatedPathString)
                if (virtualFile != null) {
                    com.intellij
                            .openapi
                            .fileEditor
                            .FileEditorManager
                            .getInstance(project)
                            .openFile(virtualFile, true)
                }

                // Show notification
                NotificationGroupManager.getInstance()
                        .getNotificationGroup("KHelper Notifications")
                        .createNotification(
                                "Problem Received",
                                "Successfully parsed: ${problem.name} (${problem.tests.size} test cases)",
                                NotificationType.INFORMATION
                        )
                        .notify(project)
            } catch (e: Exception) {
                NotificationGroupManager.getInstance()
                        .getNotificationGroup("KHelper Notifications")
                        .createNotification(
                                "Error",
                                "Failed to process problem: ${e.message}",
                                NotificationType.ERROR
                        )
                        .notify(project)
            }
        }
    }
}
