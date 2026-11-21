package com.roy.khelper.ui.toolwindow

import com.intellij.execution.RunManager
import com.intellij.execution.RunManagerListener
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.roy.khelper.model.Problem
import com.roy.khelper.runner.KSolutionRunConfiguration
import com.roy.khelper.services.ProblemNotifier
import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.JPanel

/**
 * Shows the problem of the selected KHelper run configuration, or an empty state. It follows run configuration
 * selection and problem changes until [parentDisposable] is disposed.
 */
class ProblemToolWindowPanel(private val project: Project, parentDisposable: Disposable) : JPanel(BorderLayout()) {

    private val metadataPanel = MetadataPanel(project)
    private val testCaseTablePanel = TestCaseTablePanel(project)
    private val cardLayout = CardLayout()
    private val cards = JPanel(cardLayout)

    init {
        val contentPanel = JPanel(BorderLayout()).apply {
            add(metadataPanel, BorderLayout.NORTH)
            add(testCaseTablePanel, BorderLayout.CENTER)
        }
        cards.add(contentPanel, CARD_CONTENT)
        cards.add(EmptyStatePanel(), CARD_EMPTY)
        add(cards, BorderLayout.CENTER)

        refresh()

        val connection = project.messageBus.connect(parentDisposable)
        connection.subscribe(RunManagerListener.TOPIC, object : RunManagerListener {
            override fun runConfigurationSelected(settings: RunnerAndConfigurationSettings?) = refreshLater()
            override fun runConfigurationChanged(settings: RunnerAndConfigurationSettings) = refreshLater()
            override fun runConfigurationRemoved(settings: RunnerAndConfigurationSettings) = refreshLater()
        })
        connection.subscribe(ProblemNotifier.TOPIC, object : ProblemNotifier {
            override fun problemAdded(problem: Problem) = refreshLater()
            override fun testCaseUpdated(problemId: String) = refreshLater()
            override fun problemDeleted(problemId: String) = refreshLater()
        })
    }

    //~ Events can come from any thread; Swing must be touched on the EDT only
    private fun refreshLater() {
        ApplicationManager.getApplication().invokeLater({ refresh() }) { project.isDisposed }
    }

    private fun refresh() {
        val config = RunManager.getInstance(project)
            .selectedConfiguration?.configuration as? KSolutionRunConfiguration

        metadataPanel.updateProblem(config?.problemId, config?.language)
        testCaseTablePanel.updateProblem(config?.problemId, config?.language)
        cardLayout.show(cards, if (config != null) CARD_CONTENT else CARD_EMPTY)
    }

    private companion object {
        const val CARD_CONTENT = "CONTENT"
        const val CARD_EMPTY = "EMPTY"
    }
}
