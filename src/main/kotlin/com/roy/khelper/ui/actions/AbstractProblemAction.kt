package com.roy.khelper.ui.actions

import com.intellij.execution.RunManager
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.roy.khelper.runner.KSolutionRunConfiguration
import com.roy.khelper.services.ProblemManagerService

abstract class AbstractProblemAction(text: String) : AnAction() {

    init {
        templatePresentation.text = text
    }

    protected fun findProblemData(project: Project): ProblemManagerService.ProblemData? {
        val problemManager = ProblemManagerService.getInstance(project)

        val runManager = RunManager.getInstance(project)
        val selectedConfig = runManager.selectedConfiguration?.configuration as? KSolutionRunConfiguration

        return if (selectedConfig != null) {
            problemManager.getProblem(selectedConfig.problemId, selectedConfig.language)
        } else {
            problemManager.getAllProblems().firstOrNull()
        }
    }

    protected fun showNotification(project: Project, message: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("KHelper Notifications")
            .createNotification("KHelper", message, type)
            .notify(project)
    }
}
