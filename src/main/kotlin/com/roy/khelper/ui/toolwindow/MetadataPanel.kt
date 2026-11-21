package com.roy.khelper.ui.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.roy.khelper.services.ProblemManagerService
import com.roy.khelper.ui.ProblemCommands
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.JSeparator
import javax.swing.SwingConstants
import javax.swing.border.TitledBorder

class MetadataPanel(private val project: Project) : JPanel(BorderLayout()) {

    private val nameLabel = JBLabel().apply { font = font.deriveFont(Font.BOLD, 14f) }
    private val onlineJudgeLabel = JBLabel()
    private val timeLimitLabel = JBLabel()
    private val memoryLimitLabel = JBLabel()
    private val testCaseCountLabel = JBLabel()
    private val linkPanel = JPanel(BorderLayout())

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
                            TitledBorder.LEFT,
                            TitledBorder.TOP,
                            JBUI.Fonts.label().deriveFont(Font.BOLD, 12f)
                        ),
                        JBUI.Borders.empty(12)
                    )
            }

        val namePanel = createInfoRow("Problem:", nameLabel)

        val onlineJudgePanel = createInfoRow("Online Judge:", onlineJudgeLabel)

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

        val testCasesPanel = createInfoRow("Test Cases:", testCaseCountLabel)

        contentPanel.add(namePanel)
        contentPanel.add(Box.createVerticalStrut(8))
        contentPanel.add(onlineJudgePanel)
        contentPanel.add(Box.createVerticalStrut(8))
        contentPanel.add(constraintsPanel)
        contentPanel.add(Box.createVerticalStrut(8))
        contentPanel.add(testCasesPanel)

        val separator =
            JSeparator(SwingConstants.HORIZONTAL).apply {
                preferredSize = Dimension(1, 8)
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
        linkPanel.removeAll()

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

        val links = listOf(
            "Open Problem in Browser" to ProblemCommands::openInBrowser,
            "Open Source" to ProblemCommands::openSource,
            "Copy Submission" to ProblemCommands::copySubmission,
        ).map { (text, command) ->
            ActionLink(text) { command(project, problemData) }.apply { border = JBUI.Borders.emptyTop(8) }
        }

        val inlinePanel = JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(8), 0))
        links.forEach { inlinePanel.add(it) }

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
}
