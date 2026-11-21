package com.roy.khelper.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.roy.khelper.server.CompetitiveCompanionServer
import com.roy.khelper.settings.PluginSettings.Language
import java.awt.Font
import javax.swing.JComponent
import javax.swing.JPanel

class PluginConfigurable : Configurable {

    private val languageComboBox = ComboBox(Language.entries.toTypedArray())
    private val templateTextArea = JBTextArea(15, 60).apply {
        font = Font(Font.MONOSPACED, Font.PLAIN, font.size)
    }
    private val outputDirectoryField = JBTextField()
    private val serverPortField = JBTextField(6)
    private val autoGenerateMainCheckbox = JBCheckBox("Generate Main after all tests pass")

    //~ Edited templates per language, so switching the combo box does not lose unsaved text
    private val editedTemplates = mutableMapOf<Language, String>()
    private var shownLanguage: Language = Language.KOTLIN

    override fun getDisplayName(): String = "KHelper"

    override fun createComponent(): JComponent {
        languageComboBox.addActionListener {
            val selected = languageComboBox.selectedItem as Language
            if (selected != shownLanguage) {
                editedTemplates[shownLanguage] = templateTextArea.text
                templateTextArea.text = editedTemplates.getValue(selected)
                shownLanguage = selected
            }
        }

        val resetLink = ActionLink("Reset to Default") {
            templateTextArea.text = PluginSettings.defaultTemplateFor(shownLanguage)
        }

        return FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Preferred language:"), languageComboBox)
            .addSeparator()
            .addLabeledComponent(JBLabel("Solution template:"), JBScrollPane(templateTextArea), true)
            .addComponent(resetLink)
            .addComponent(hint($$"Variables: ${PROBLEM_ID}, ${PROBLEM_NAME}, ${CLASS_NAME}, ${TIME_LIMIT}, ${MEMORY_LIMIT}"))
            .addSeparator()
            .addLabeledComponent(JBLabel("Output directory:"), outputDirectoryField)
            .addComponent(hint("Folder for the generated Main file, relative to the project root"))
            .addComponent(autoGenerateMainCheckbox)
            .addSeparator()
            .addLabeledComponent(JBLabel("Competitive Companion port:"), serverPortField)
            .addComponent(hint("Must match a custom port in the Competitive Companion extension settings"))
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    override fun isModified(): Boolean {
        val state = PluginSettings.getInstance().state
        syncShownTemplate()
        return editedTemplates[Language.KOTLIN] != state.solutionTemplate ||
                editedTemplates[Language.JAVA] != state.javaSolutionTemplate ||
                languageComboBox.selectedItem != state.preferredLanguage ||
                outputDirectoryField.text != state.outputDirectory ||
                autoGenerateMainCheckbox.isSelected != state.autoGenerateMain ||
                serverPortField.text.trim() != state.serverPort.toString()
    }

    override fun apply() {
        val port = serverPortField.text.trim().toIntOrNull()
        if (port == null || port !in 1..65535) {
            throw ConfigurationException("The port must be a number from 1 to 65535.")
        }

        val state = PluginSettings.getInstance().state
        syncShownTemplate()
        state.solutionTemplate = editedTemplates.getValue(Language.KOTLIN)
        state.javaSolutionTemplate = editedTemplates.getValue(Language.JAVA)
        state.preferredLanguage = languageComboBox.selectedItem as Language
        state.outputDirectory = outputDirectoryField.text.trim()
        state.autoGenerateMain = autoGenerateMainCheckbox.isSelected

        if (port != state.serverPort) {
            state.serverPort = port
            CompetitiveCompanionServer.getInstance().restart()
        }
    }

    override fun reset() {
        val state = PluginSettings.getInstance().state
        editedTemplates[Language.KOTLIN] = state.solutionTemplate
        editedTemplates[Language.JAVA] = state.javaSolutionTemplate
        shownLanguage = state.preferredLanguage
        languageComboBox.selectedItem = state.preferredLanguage
        templateTextArea.text = state.templateFor(state.preferredLanguage)
        templateTextArea.caretPosition = 0
        outputDirectoryField.text = state.outputDirectory
        autoGenerateMainCheckbox.isSelected = state.autoGenerateMain
        serverPortField.text = state.serverPort.toString()
    }

    private fun syncShownTemplate() {
        editedTemplates[shownLanguage] = templateTextArea.text
    }

    private fun hint(text: String) = JBLabel(text).apply {
        componentStyle = UIUtil.ComponentStyle.SMALL
        fontColor = UIUtil.FontColor.BRIGHTER
        border = JBUI.Borders.emptyBottom(4)
    }
}
