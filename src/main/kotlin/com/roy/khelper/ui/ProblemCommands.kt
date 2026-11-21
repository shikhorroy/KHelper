package com.roy.khelper.ui

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.roy.khelper.generator.MainGenerator
import com.roy.khelper.services.KHelperNotifications
import com.roy.khelper.services.ProblemManagerService.ProblemData
import com.roy.khelper.services.ProblemManagerService
import java.awt.datatransfer.StringSelection

/** The user commands on a problem. Shared by the Tools menu actions and the tool window links. Call on the EDT. */
object ProblemCommands {

    fun copySubmission(project: Project, problemData: ProblemData) {
        MainGenerator.generate(project, problemData.filePath) { content ->
            CopyPasteManager.getInstance().setContents(StringSelection(content))
            KHelperNotifications.info(project, "Submission code for '${problemData.problem.name}' copied to clipboard!")
        }
    }

    fun openSource(project: Project, problemData: ProblemData) {
        val file = LocalFileSystem.getInstance().refreshAndFindFileByPath(problemData.filePath)
        if (file != null) {
            FileEditorManager.getInstance(project).openFile(file, true)
        } else {
            KHelperNotifications.warning(project, "Source file not found: ${problemData.filePath}")
        }
    }

    fun openInBrowser(project: Project, problemData: ProblemData) {
        val url = problemData.problem.url
        if (url.isBlank()) {
            KHelperNotifications.warning(project, "'${problemData.problem.name}' has no URL")
        } else {
            BrowserUtil.browse(url)
        }
    }

    fun delete(project: Project, problemData: ProblemData) {
        val name = problemData.problem.name
        val answer = Messages.showYesNoDialog(
            project,
            "Are you sure you want to delete '$name'?\n\n" +
                    "This will remove:\n" +
                    "• Solution file\n" +
                    "• Problem directory\n" +
                    "• Run configurations\n" +
                    "• Test cases\n\n" +
                    "This action cannot be undone.",
            "Delete Problem",
            "Delete",
            "Cancel",
            Messages.getWarningIcon(),
        )
        if (answer != Messages.YES) return

        try {
            ProblemManagerService.getInstance(project)
                .deleteProblem(problemData.problem.generateId(), problemData.language)
            KHelperNotifications.info(project, "Problem '$name' has been deleted successfully.", "Problem Deleted")
        } catch (e: Exception) {
            KHelperNotifications.error(project, "Failed to delete '$name': ${e.message}", "Delete Failed")
        }
    }
}
