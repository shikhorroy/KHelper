package com.roy.khelper.ui.actions

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import com.roy.khelper.generator.MainGenerator
import java.awt.datatransfer.StringSelection

class CopySubmissionAction : AbstractProblemAction("Copy Submission Code") {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        try {
            val problemData = findProblemData(project)

            val pd = problemData ?: run {
                showNotification(project, "No problem selected or found", NotificationType.WARNING)
                return
            }

            // Generate and get content
            val mainContent = MainGenerator.getMainContent(project, pd.filePath)

            // Copy to clipboard
            CopyPasteManager.getInstance().setContents(StringSelection(mainContent))

            showNotification(
                project,
                "Submission code for '${pd.problem.name}' copied to clipboard!",
                NotificationType.INFORMATION
            )
        } catch (ex: Exception) {
            showNotification(project, "Error: ${ex.message}", NotificationType.ERROR)
        }
    }
}
