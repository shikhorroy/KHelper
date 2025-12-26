package com.roy.khelper.ui.toolwindow

import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.roy.khelper.generator.MainGenerator
import com.roy.khelper.services.ProblemManagerService
import java.awt.BorderLayout
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.datatransfer.StringSelection
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel

class MetadataPanel(private val project: Project) : JPanel(BorderLayout()) {

    private val nameLabel = JBLabel().apply { font = font.deriveFont(Font.BOLD, 14f) }
    private val onlineJudgeLabel = JBLabel()
    private val timeLimitLabel = JBLabel()
    private val memoryLimitLabel = JBLabel()
    private val testCaseCountLabel = JBLabel()
    private val linkPanel =
        JPanel(BorderLayout().apply { vgap = 0 }) // Dedicated panel for the link

    init {
        border = JBUI.Borders.empty(10)

        val contentPanel =
            JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                border =
                    BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder(
                            BorderFactory.createEtchedBorder(),
                            "Problem Information",
                            javax.swing.border.TitledBorder.LEFT,
                            javax.swing.border.TitledBorder.TOP,
                            JBUI.Fonts.label().deriveFont(Font.BOLD, 12f)
                        ),
                        JBUI.Borders.empty(12)
                    )
            }

        // Problem name section
        val namePanel = createInfoRow("Problem:", nameLabel)

        // Online Judge section
        val onlineJudgePanel = createInfoRow("Online Judge:", onlineJudgeLabel)

        // Constraints section
        val constraintsPanel =
            JPanel(GridBagLayout()).apply {
                val gbc =
                    GridBagConstraints().apply {
                        anchor = GridBagConstraints.WEST
                        fill = GridBagConstraints.HORIZONTAL
                        insets = JBUI.insets(4, 0)
                    }

                // Time limit
                gbc.gridx = 0
                gbc.gridy = 0
                gbc.weightx = 0.0
                add(createLabel("Time Limit:", true), gbc)

                gbc.gridx = 1
                gbc.weightx = 0.5
                gbc.insets = JBUI.insets(4, 8, 4, 16)
                add(timeLimitLabel, gbc)

                // Memory limit
                gbc.gridx = 2
                gbc.weightx = 0.0
                gbc.insets = JBUI.insets(4, 0)
                add(createLabel("Memory Limit:", true), gbc)

                gbc.gridx = 3
                gbc.weightx = 0.5
                gbc.insets = JBUI.insets(4, 8, 4, 0)
                add(memoryLimitLabel, gbc)
            }

        // Test cases section
        val testCasesPanel = createInfoRow("Test Cases:", testCaseCountLabel)

        contentPanel.add(namePanel)
        contentPanel.add(Box.createVerticalStrut(8))
        contentPanel.add(onlineJudgePanel)
        contentPanel.add(Box.createVerticalStrut(8))
        contentPanel.add(constraintsPanel)
        contentPanel.add(Box.createVerticalStrut(8))
        contentPanel.add(testCasesPanel)

        // Add a horizontal separator and link panel at the bottom
        val separator =
            javax.swing.JSeparator(javax.swing.SwingConstants.HORIZONTAL).apply {
                preferredSize = java.awt.Dimension(1, 8)
            }

        contentPanel.add(Box.createVerticalStrut(8))
        contentPanel.add(separator)
        contentPanel.add(Box.createVerticalStrut(6))
        contentPanel.add(linkPanel)

        add(contentPanel, BorderLayout.NORTH)
    }

    private fun createInfoRow(labelText: String, valueLabel: JBLabel): JPanel {
        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(4, 0)
            add(createLabel(labelText, true), BorderLayout.WEST)
            add(valueLabel, BorderLayout.CENTER)
        }
    }

    private fun createLabel(text: String, bold: Boolean = false): JBLabel {
        return JBLabel(text).apply {
            if (bold) {
                font = font.deriveFont(Font.BOLD)
            }
            border = JBUI.Borders.emptyRight(8)
        }
    }

    fun updateProblem(problemId: String?, language: String?) {
        linkPanel.removeAll() // Clear previous links

        if (problemId == null || language == null) {
            clear()
            return
        }

        val problemManager = ProblemManagerService.getInstance(project)
        val problemData =
            problemManager.getProblem(problemId, language)
                ?: run {
                    clear()
                    return
                }

        val problem = problemData.problem

        nameLabel.text = problem.name
        onlineJudgeLabel.text = problem.group.ifEmpty { "Unknown" }
        timeLimitLabel.text = "${problem.timeLimit} ms"
        memoryLimitLabel.text = "${problem.memoryLimit} MB"
        testCaseCountLabel.text = "${problemData.testCases.size}"

        // Update URL link and action links
        val urlLink =
            ActionLink("🔗 Open Problem in Browser") { BrowserUtil.browse(problem.url) }.apply {
                border = JBUI.Borders.emptyTop(8)
            }

        // Open Source link
        val openSourceLink =
            ActionLink("📂 Open Source") {
                try {
                    val filePath = problemData.filePath
                    val virtualFile =
                        LocalFileSystem.getInstance().refreshAndFindFileByPath(filePath)
                    if (virtualFile != null) {
                        FileEditorManager.getInstance(project).openFile(virtualFile, true)
                        showNotification(
                            project,
                            "Source opened in editor",
                            NotificationType.INFORMATION
                        )
                    } else {
                        showNotification(
                            project,
                            "Source file not found: $filePath",
                            NotificationType.WARNING
                        )
                    }
                } catch (ex: Exception) {
                    showNotification(
                        project,
                        "Error opening source: ${ex.message}",
                        NotificationType.ERROR
                    )
                }
            }
                .apply { border = JBUI.Borders.emptyTop(8) }

        // Copy Submission link
        val copySubmissionLink =
            ActionLink("📋 Copy Submission") {
                try {
                    val mainContent =
                        MainGenerator.getMainContent(project, problemData.filePath)
                    CopyPasteManager.getInstance().setContents(StringSelection(mainContent))
                    showNotification(
                        project,
                        "Submission code for '${problemData.problem.name}' copied to clipboard!",
                        NotificationType.INFORMATION
                    )
                } catch (ex: Exception) {
                    showNotification(
                        project,
                        "Error copying submission: ${ex.message}",
                        NotificationType.ERROR
                    )
                }
            }
                .apply { border = JBUI.Borders.emptyTop(8) }

        // Add links inline on a single row
        val inlinePanel = JPanel(java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0))
        inlinePanel.add(urlLink)
        inlinePanel.add(openSourceLink)
        inlinePanel.add(copySubmissionLink)

        linkPanel.add(inlinePanel, BorderLayout.WEST)

        revalidate()
        repaint()
    }

    private fun clear() {
        nameLabel.text = "No problem selected"
        onlineJudgeLabel.text = "-"
        timeLimitLabel.text = "-"
        memoryLimitLabel.text = "-"
        testCaseCountLabel.text = "-"

        linkPanel.removeAll()

        revalidate()
        repaint()
    }

    private fun showNotification(project: Project, message: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("KHelper Notifications")
            .createNotification("KHelper", message, type)
            .notify(project)
    }
}
