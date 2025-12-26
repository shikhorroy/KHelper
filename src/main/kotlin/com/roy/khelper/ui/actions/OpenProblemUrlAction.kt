package com.roy.khelper.ui.actions

import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent

class OpenProblemUrlAction : AbstractProblemAction("Open Problem in Browser") {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        try {
            val problemData = findProblemData(project)

            val pd = problemData ?: run {
                showNotification(project, "No problem selected or found", NotificationType.WARNING)
                return
            }

            BrowserUtil.browse(pd.problem.url)
        } catch (ex: Exception) {
            showNotification(project, "Error: ${ex.message}", NotificationType.ERROR)
        }
    }
}
