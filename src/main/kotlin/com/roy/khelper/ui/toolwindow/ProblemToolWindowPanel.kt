package com.roy.khelper.ui.toolwindow

import com.intellij.execution.RunManager
import com.intellij.execution.RunManagerListener
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.project.Project
import com.roy.khelper.runner.KSolutionRunConfiguration
import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.JPanel

class ProblemToolWindowPanel(private val project: Project) : JPanel(BorderLayout()) {

    private val metadataPanel = MetadataPanel(project)
    private val testCaseTablePanel = TestCaseTablePanel(project)
    private val emptyStatePanel = EmptyStatePanel()

    init {
        // Layout: use CardLayout to switch between content and empty state
        val cardPanel = JPanel(CardLayout())

        // Normal content: Metadata at North, TestCases at Center
        val contentPanel = JPanel(BorderLayout())
        contentPanel.add(metadataPanel, BorderLayout.NORTH)
        contentPanel.add(testCaseTablePanel, BorderLayout.CENTER)

        cardPanel.add(contentPanel, "CONTENT")
        cardPanel.add(emptyStatePanel, "EMPTY")

        add(cardPanel, BorderLayout.CENTER)

        // Initialize with current run configuration
        updateFromRunManager()

        // Listen for run configuration changes
        project.messageBus
                .connect()
                .subscribe(
                        RunManagerListener.TOPIC,
                        object : RunManagerListener {
                            override fun runConfigurationSelected(
                                    settings: RunnerAndConfigurationSettings?
                            ) {
                                updateFromRunManager()
                            }

                            override fun runConfigurationChanged(
                                    settings: RunnerAndConfigurationSettings
                            ) {
                                updateFromRunManager()
                            }
                        }
                )
    }

    private fun updateFromRunManager() {
        val runManager = RunManager.getInstance(project)
        val selectedSettings = runManager.selectedConfiguration

        val hasSelection =
                selectedSettings != null &&
                        selectedSettings.configuration is KSolutionRunConfiguration

        if (hasSelection) {
            val runConfig = selectedSettings.configuration as KSolutionRunConfiguration
            val problemId = runConfig.problemId
            val language = runConfig.language

            metadataPanel.updateProblem(problemId, language)
            testCaseTablePanel.updateProblem(problemId, language)
        } else {
            // Clear selection if not a KSolution configuration
            metadataPanel.updateProblem(null, null)
            testCaseTablePanel.updateProblem(null, null)
        }
        // Toggle cards
        val parent = this.getComponent(0)
        if (parent is JPanel && parent.layout is CardLayout) {
            val cl = parent.layout as CardLayout
            if (hasSelection) cl.show(parent, "CONTENT") else cl.show(parent, "EMPTY")
        }
    }
}
