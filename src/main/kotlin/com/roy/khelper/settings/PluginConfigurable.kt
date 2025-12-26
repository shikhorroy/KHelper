package com.roy.khelper.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel

class PluginConfigurable : Configurable {

    private var mainPanel: JPanel? = null
    private val serverPortField = JBTextField()
    private val templateTextArea = JBTextArea(15, 60)
    private val outputDirectoryField = JBTextField()
    private val autoGenerateMainCheckbox =
        JCheckBox("Auto-generate Main.kt when all tests pass")
    private val useFastIOCheckbox = JCheckBox("Use Fast I/O template")
    private val languageComboBox = ComboBox(PluginSettings.Language.entries.toTypedArray())

    private var currentKotlinTemplate = ""
    private var currentJavaTemplate = ""
    private var lastSelectedLanguage: PluginSettings.Language? = null

    override fun getDisplayName(): String = "KHelper"

    override fun createComponent(): JComponent {
        reset()

        languageComboBox.addActionListener {
            val selected = languageComboBox.selectedItem as PluginSettings.Language
            if (selected != lastSelectedLanguage) {
                // Save current text to the previous language's variable
                if (lastSelectedLanguage == PluginSettings.Language.KOTLIN) {
                    currentKotlinTemplate = templateTextArea.text
                } else {
                    currentJavaTemplate = templateTextArea.text
                }

                // Switch to a new language's template
                templateTextArea.text =
                    if (selected == PluginSettings.Language.KOTLIN) currentKotlinTemplate else currentJavaTemplate
                lastSelectedLanguage = selected
            }
        }

        mainPanel =
            FormBuilder.createFormBuilder()
                .addLabeledComponent(JBLabel("Preferred Language:"), languageComboBox, 1, false)
                .addSeparator()
                .addLabeledComponent(
                    JBLabel("Solution Template:"),
                    templateTextArea,
                    1,
                    true
                )
                .addComponent(
                    com.intellij.ui.components.ActionLink("Reset to Default") {
                        val selected = languageComboBox.selectedItem as PluginSettings.Language
                        templateTextArea.text = if (selected == PluginSettings.Language.KOTLIN)
                            PluginSettings.DEFAULT_KOTLIN_TEMPLATE
                        else
                            PluginSettings.DEFAULT_JAVA_TEMPLATE
                    }
                )
                .addComponent(
                    JBLabel(
                        "Available variables: \${PROBLEM_ID}, \${PROBLEM_NAME}, \${CLASS_NAME}, \${TIME_LIMIT}, \${MEMORY_LIMIT}"
                    )
                )
                .addSeparator()
                .addLabeledComponent(
                    JBLabel("Output Directory:"),
                    outputDirectoryField,
                    1,
                    false
                )
                .addComponent(
                    JBLabel(
                        "Directory where Main.kt will be generated (relative to project root)"
                    )
                )
                .addSeparator()
                .addComponentFillVertically(JPanel(), 0)
                .panel

        return mainPanel!!
    }

    override fun isModified(): Boolean {
        val settings = PluginSettings.getInstance()

        // Sync current text to temporary variables before checking
        val selected = languageComboBox.selectedItem as PluginSettings.Language
        if (selected == PluginSettings.Language.KOTLIN) {
            currentKotlinTemplate = templateTextArea.text
        } else {
            currentJavaTemplate = templateTextArea.text
        }

        return currentKotlinTemplate != settings.state.solutionTemplate ||
                currentJavaTemplate != settings.state.javaSolutionTemplate ||
                languageComboBox.selectedItem != settings.state.preferredLanguage ||
                outputDirectoryField.text != settings.state.outputDirectory
    }

    override fun apply() {
        val settings = PluginSettings.getInstance()

        // Sync current text to temporary variables before applying
        val selected = languageComboBox.selectedItem as PluginSettings.Language
        if (selected == PluginSettings.Language.KOTLIN) {
            currentKotlinTemplate = templateTextArea.text
        } else {
            currentJavaTemplate = templateTextArea.text
        }

        settings.state.solutionTemplate = currentKotlinTemplate
        settings.state.javaSolutionTemplate = currentJavaTemplate
        settings.state.preferredLanguage = selected
        settings.state.outputDirectory = outputDirectoryField.text
    }

    override fun reset() {
        val settings = PluginSettings.getInstance()

        currentKotlinTemplate = settings.state.solutionTemplate
        currentJavaTemplate = settings.state.javaSolutionTemplate
        lastSelectedLanguage = settings.state.preferredLanguage

        languageComboBox.selectedItem = lastSelectedLanguage
        templateTextArea.text =
            if (lastSelectedLanguage == PluginSettings.Language.KOTLIN) currentKotlinTemplate else currentJavaTemplate
        outputDirectoryField.text = settings.state.outputDirectory
    }
}
