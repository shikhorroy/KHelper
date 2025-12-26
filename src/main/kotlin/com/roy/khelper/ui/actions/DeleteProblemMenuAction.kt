package com.roy.khelper.ui.actions

import com.intellij.execution.RunManager
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.roy.khelper.runner.KSolutionRunConfiguration
import com.roy.khelper.services.ProblemManagerService

class DeleteProblemMenuAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // Get the active run configuration
        val runManager = RunManager.getInstance(project)
        val selectedSettings = runManager.selectedConfiguration

        if (selectedSettings == null || selectedSettings.configuration !is KSolutionRunConfiguration
        ) {
            Messages.showErrorDialog(
                project,
                "Please select a 'Test: Problem' run configuration first.",
                "No Problem Selected"
            )
            return
        }

        val runConfig = selectedSettings.configuration as KSolutionRunConfiguration
        val problemId = runConfig.problemId
        val language = runConfig.language

        if (problemId.isEmpty()) {
            Messages.showErrorDialog(
                project,
                "Selected run configuration has no associated problem.",
                "Invalid Configuration"
            )
            return
        }

        val problemManager = ProblemManagerService.getInstance(project)
        val problemData = problemManager.getProblem(problemId, language) ?: return

        val result =
            Messages.showYesNoDialog(
                project,
                "Are you sure you want to delete '${problemData.problem.name}'?\n\n" +
                        "This will remove:\n" +
                        "• Solution file\n" +
                        "• Problem directory\n" +
                        "• Run configurations\n" +
                        "• Test cases\n\n" +
                        "This action cannot be undone.",
                "Delete Problem",
                "Delete",
                "Cancel",
                Messages.getWarningIcon()
            )

        if (result == Messages.YES) {
            try {
                problemManager.deleteProblem(problemId, language)
                NotificationGroupManager.getInstance()
                    .getNotificationGroup("KHelper Notifications")
                    .createNotification(
                        "Problem Deleted",
                        "Problem '${problemData.problem.name}' has been deleted successfully.",
                        NotificationType.INFORMATION
                    )
                    .notify(project)
            } catch (ex: Exception) {
                NotificationGroupManager.getInstance()
                    .getNotificationGroup("KHelper Notifications")
                    .createNotification(
                        "Delete Failed",
                        "Failed to delete '${problemData.problem.name}': ${ex.message}",
                        NotificationType.ERROR
                    )
                    .notify(project)
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isEnabled = false
            return
        }

        // Only enable if a KHelper configuration is selected
        val runManager = RunManager.getInstance(project)
        val selectedSettings = runManager.selectedConfiguration
        e.presentation.isEnabled = selectedSettings?.configuration is KSolutionRunConfiguration
    }
}
