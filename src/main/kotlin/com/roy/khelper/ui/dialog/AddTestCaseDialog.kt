package com.roy.khelper.ui.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import com.roy.khelper.model.TestCase
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*
import javax.swing.border.TitledBorder

class AddTestCaseDialog(project: Project, private val existingTestCase: TestCase? = null) :
    DialogWrapper(project) {

    private val inputTextArea =
        JBTextArea(12, 60).apply {
            lineWrap = true
            wrapStyleWord = true
            font = font.deriveFont(13f)
        }

    private val outputTextArea =
        JBTextArea(12, 60).apply {
            lineWrap = true
            wrapStyleWord = true
            font = font.deriveFont(13f)
        }

    init {
        title = if (existingTestCase == null) "Add Test Case" else "Edit Test Case"

        existingTestCase?.let {
            inputTextArea.text = it.input
            outputTextArea.text = it.output
        }

        init()
    }

    override fun createCenterPanel(): JComponent {
        val mainPanel =
            JPanel(BorderLayout()).apply {
                preferredSize = Dimension(700, 500)
                border = JBUI.Borders.empty(10)
            }

        val contentPanel = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }

        // Input section
        val inputPanel =
            createTextAreaPanel("Input", "Enter the test case input (stdin)", inputTextArea)

        // Output section
        val outputPanel =
            createTextAreaPanel(
                "Expected Output",
                "Enter the expected output (stdout)",
                outputTextArea
            )

        contentPanel.add(inputPanel)
        contentPanel.add(Box.createVerticalStrut(15))
        contentPanel.add(outputPanel)

        mainPanel.add(contentPanel, BorderLayout.CENTER)

        return mainPanel
    }

    private fun createTextAreaPanel(title: String, hint: String, textArea: JBTextArea): JPanel {
        val panel =
            JPanel(BorderLayout()).apply {
                border =
                    BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder(
                            BorderFactory.createEtchedBorder(),
                            title,
                            TitledBorder.LEFT,
                            TitledBorder.TOP,
                            JBUI.Fonts.label().deriveFont(12f)
                        ),
                        JBUI.Borders.empty(8)
                    )
            }

        val hintLabel =
            JBLabel(hint).apply {
                foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
                font = JBUI.Fonts.smallFont()
                border = JBUI.Borders.emptyBottom(5)
            }

        val scrollPane =
            JBScrollPane(textArea).apply {
                preferredSize = Dimension(650, 180)
                minimumSize = Dimension(500, 150)
            }

        panel.add(hintLabel, BorderLayout.NORTH)
        panel.add(scrollPane, BorderLayout.CENTER)

        return panel
    }

    fun getTestCase(): TestCase {
        return TestCase(input = inputTextArea.text.trim(), output = outputTextArea.text.trim())
    }

    override fun doValidate(): ValidationInfo? {
        if (inputTextArea.text.isBlank()) {
            return ValidationInfo("Input cannot be empty", inputTextArea)
        }
        if (outputTextArea.text.isBlank()) {
            return ValidationInfo("Output cannot be empty", outputTextArea)
        }
        return null
    }
}
