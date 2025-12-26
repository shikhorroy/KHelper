package com.roy.khelper.runner

import com.intellij.openapi.options.SettingsEditor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class KSolutionSettingsEditor : SettingsEditor<KSolutionRunConfiguration>() {

    private val problemIdField = JBTextField()
    private val solutionFilePathField = JBTextField()

    override fun resetEditorFrom(configuration: KSolutionRunConfiguration) {
        problemIdField.text = configuration.problemId
        solutionFilePathField.text = configuration.solutionFilePath
    }

    override fun applyEditorTo(configuration: KSolutionRunConfiguration) {
        configuration.problemId = problemIdField.text
        configuration.solutionFilePath = solutionFilePathField.text
    }

    override fun createEditor(): JComponent {
        return FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Problem ID:"), problemIdField, 1, false)
            .addLabeledComponent(JBLabel("Solution File:"), solutionFilePathField, 1, false)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }
}
