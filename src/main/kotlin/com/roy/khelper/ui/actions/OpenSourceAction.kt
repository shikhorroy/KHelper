package com.roy.khelper.ui.actions

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.LocalFileSystem

class OpenSourceAction : AbstractProblemAction("Open Source Code") {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        try {
            val problemData = findProblemData(project)

            val pd = problemData ?: run {
                showNotification(project, "No problem selected or found", NotificationType.WARNING)
                return
            }

            // Get the file path
            val filePath = pd.filePath

            // Open the file in the editor
            val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(filePath)
            if (virtualFile != null) {
                FileEditorManager.getInstance(project).openFile(virtualFile, true)
            } else {
                showNotification(
                    project,
                    "Source file not found: $filePath",
                    NotificationType.ERROR
                )
            }
        } catch (ex: Exception) {
            showNotification(project, "Error: ${ex.message}", NotificationType.ERROR)
        }
    }
}