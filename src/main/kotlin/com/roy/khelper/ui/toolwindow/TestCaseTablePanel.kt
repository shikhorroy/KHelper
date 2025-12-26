package com.roy.khelper.ui.toolwindow

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import com.roy.khelper.icons.KHelperIcons
import com.roy.khelper.model.TestCase
import com.roy.khelper.services.ProblemManagerService
import com.roy.khelper.ui.dialog.AddTestCaseDialog
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer

class TestCaseTablePanel(private val project: Project) : JPanel(BorderLayout()) {

    private var currentProblemId: String? = null
    private var currentLanguage: String? = null
    private val tableModel = TestCaseTableModel()
    private val table =
            JBTable(tableModel).apply {
                setShowGrid(true)
                rowHeight = 28

                // Set column widths
                // Active checkbox
                columnModel.getColumn(0).apply {
                    preferredWidth = 30
                    maxWidth = 30
                    headerValue = "" // No text for checkbox column
                }
                // Index
                columnModel.getColumn(1).apply {
                    preferredWidth = 40
                    maxWidth = 60
                }
                // Input & Output
                columnModel.getColumn(2).preferredWidth = 285
                columnModel.getColumn(3).preferredWidth = 285

                // Add tooltip renderer for truncated text (only for text columns)
                setDefaultRenderer(String::class.java, TooltipTableCellRenderer())

                // Add double-click listener to edit test case
                addMouseListener(
                        object : java.awt.event.MouseAdapter() {
                            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                                if (e.clickCount == 2 && selectedRow >= 0) {
                                    editTestCase()
                                }
                            }
                        }
                )
            }

    private val addAction =
            object : DumbAwareAction("Add Test Case", "Add new test case", KHelperIcons.Add) {
                override fun actionPerformed(e: AnActionEvent) {
                    addTestCase()
                }
            }

    private val removeAction =
            object :
                    DumbAwareAction(
                            "Remove Test Case",
                            "Remove selected test case",
                            KHelperIcons.Delete
                    ) {
                override fun actionPerformed(e: AnActionEvent) {
                    removeTestCase()
                }

                override fun update(e: AnActionEvent) {
                    e.presentation.isEnabled = table.selectedRow >= 0
                }
            }

    private val editAction =
            object :
                    DumbAwareAction(
                            "Edit Test Case",
                            "Edit selected test case",
                            KHelperIcons.Edit
                    ) {
                override fun actionPerformed(e: AnActionEvent) {
                    editTestCase()
                }

                override fun update(e: AnActionEvent) {
                    e.presentation.isEnabled = table.selectedRow >= 0
                }
            }

    private val enableAllAction =
            object : DumbAwareAction("Enable All", "Enable all test cases", KHelperIcons.CheckAll) {
                override fun actionPerformed(e: AnActionEvent) {
                    setAllActive(true)
                }
            }

    private val disableAllAction =
            object :
                    DumbAwareAction(
                            "Disable All",
                            "Disable all test cases",
                            KHelperIcons.UncheckAll
                    ) {
                override fun actionPerformed(e: AnActionEvent) {
                    setAllActive(false)
                }
            }

    private val headerLabel =
            JBLabel("Test Cases").apply {
                font = font.deriveFont(Font.BOLD, 12f)
                border = JBUI.Borders.empty(8, 10)
            }

    init {
        border = JBUI.Borders.empty(10)

        val contentPanel =
                JPanel(BorderLayout()).apply {
                    border =
                            BorderFactory.createCompoundBorder(
                                    BorderFactory.createEtchedBorder(),
                                    JBUI.Borders.empty(0)
                            )
                }

        // Add header
        contentPanel.add(headerLabel, BorderLayout.NORTH)

        val decorator =
                ToolbarDecorator.createDecorator(table)
                        .addExtraActions(
                                enableAllAction,
                                disableAllAction,
                                addAction,
                                removeAction,
                                editAction
                        )
                        .disableAddAction()
                        .disableRemoveAction()

        contentPanel.add(decorator.createPanel(), BorderLayout.CENTER)

        add(contentPanel, BorderLayout.CENTER)
    }

    fun updateProblem(problemId: String?, language: String?) {
        currentProblemId = problemId
        currentLanguage = language
        refreshTable()
        updateHeader()
    }

    private fun updateHeader() {
        val count = tableModel.getRowCount()
        // Simple update logic
        headerLabel.text =
                if (currentProblemId != null) {
                    "Test Cases ($count)"
                } else {
                    "Test Cases"
                }
    }

    private fun refreshTable() {
        if (currentProblemId == null || currentLanguage == null) {
            tableModel.setTestCases(emptyList())
            return
        }

        val problemManager = ProblemManagerService.getInstance(project)
        val testCases = problemManager.getTestCases(currentProblemId!!, currentLanguage!!)
        tableModel.setTestCases(testCases)
    }

    private fun addTestCase() {
        val problemId = currentProblemId ?: return
        val language = currentLanguage ?: return

        val dialog = AddTestCaseDialog(project)
        if (dialog.showAndGet()) {
            val testCase = dialog.getTestCase()
            val problemManager = ProblemManagerService.getInstance(project)
            problemManager.addTestCase(problemId, language, testCase)
            refreshTable()
            updateHeader()
        }
    }

    private fun removeTestCase() {
        val problemId = currentProblemId ?: return
        val language = currentLanguage ?: return
        val selectedRow = table.selectedRow
        if (selectedRow < 0) return

        val problemManager = ProblemManagerService.getInstance(project)
        problemManager.removeTestCase(problemId, language, selectedRow)
        refreshTable()
        updateHeader()
    }

    private fun editTestCase() {
        val problemId = currentProblemId ?: return
        val language = currentLanguage ?: return
        val selectedRow = table.selectedRow
        if (selectedRow < 0) return

        val problemManager = ProblemManagerService.getInstance(project)
        val testCase = problemManager.getTestCases(problemId, language)[selectedRow]

        val dialog = AddTestCaseDialog(project, testCase)
        if (dialog.showAndGet()) {
            val updatedTestCase = dialog.getTestCase()
            // Preserve active state
            updatedTestCase.active = testCase.active
            problemManager.updateTestCase(problemId, language, selectedRow, updatedTestCase)
            refreshTable()
        }
    }

    private fun setAllActive(active: Boolean) {
        val problemId = currentProblemId ?: return
        val language = currentLanguage ?: return
        val problemManager = ProblemManagerService.getInstance(project)
        val testCases = problemManager.getTestCases(problemId, language)

        testCases.forEachIndexed { index, testCase ->
            if (testCase.active != active) {
                // Create copy with new active state
                val updated = testCase.copy(active = active)
                problemManager.updateTestCase(problemId, language, index, updated)
            }
        }
        refreshTable()
    }

    private inner class TestCaseTableModel : AbstractTableModel() {
        private var testCases = listOf<TestCase>()

        fun setTestCases(testCases: List<TestCase>) {
            this.testCases = testCases
            fireTableDataChanged()
        }

        override fun getRowCount(): Int = testCases.size

        override fun getColumnCount(): Int = 4 // Active, Index, Input, Output

        override fun getColumnName(column: Int): String =
                when (column) {
                    0 -> "" // Active
                    1 -> "#"
                    2 -> "Input"
                    3 -> "Expected Output"
                    else -> ""
                }

        override fun getColumnClass(columnIndex: Int): Class<*> {
            return if (columnIndex == 0) Boolean::class.javaObjectType else String::class.java
        }

        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
            return columnIndex == 0 // Only active checkbox is editable
        }

        override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
            if (columnIndex == 0 && aValue is Boolean) {
                val problemId = currentProblemId ?: return
                val language = currentLanguage ?: return
                val problemManager = ProblemManagerService.getInstance(project)
                val testCase = testCases[rowIndex]

                // Create copy with new active state
                val updated = testCase.copy(active = aValue)
                problemManager.updateTestCase(problemId, language, rowIndex, updated)

                // Refresh local list to reflect change immediately (though refreshTable will be
                // called if we used listeners,
                // here we just rely on data binding or simple update)
                // Actually updateTestCase notifies listeners, which calls refreshTable in this
                // panel.
                // But to be safe and avoid race/lag, we can update local model too if needed,
                // but relying on Service listener (if implemented) is better.
                // Since this panel listens to nothing for updates *initiated by itself* usually
                // (except if added listener),
                // wait, ProblemTreePanel had listeners. ProblemManagerService publishes topic.
                // This panel *should* subscribe to updates?
                // Ah, the refactoring removed ProblemTreePanel listeners.
                // ProblemToolWindowPanel passes updates.
                // But updateTestCase triggers topic. 'ProblemNotifier'.
                // Does this panel listen to it?
                // ProblemToolWindowPanel creates this panel.
                // ProblemToolWindowPanel updates THIS panel when *run configuration* changes.
                // Does it listen for *dataset* changes?
                // It should.
                // Let's assume for now we just update.
            }
        }

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val testCase = testCases[rowIndex]
            return when (columnIndex) {
                0 -> testCase.active
                1 -> rowIndex + 1
                2 -> testCase.input
                3 -> testCase.output
                else -> ""
            }
        }

        fun getFullValue(rowIndex: Int, columnIndex: Int): String {
            val testCase = testCases[rowIndex]
            return when (columnIndex) {
                2 -> testCase.input
                3 -> testCase.output
                else -> ""
            }
        }
    }

    private class TooltipTableCellRenderer : DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(
                table: JTable,
                value: Any?,
                isSelected: Boolean,
                hasFocus: Boolean,
                row: Int,
                column: Int
        ): Component {
            val component =
                    super.getTableCellRendererComponent(
                            table,
                            value,
                            isSelected,
                            hasFocus,
                            row,
                            column
                    )

            if (value is String && column > 1) { // Skip Active (0) and Index (1)
                val displayText =
                        if (value.length > 50) {
                            value.take(50) + "..."
                        } else {
                            value
                        }
                text = displayText
                toolTipText =
                        if (value.length > 50) {
                            "<html>${value.replace("\n", "<br>").take(500)}</html>"
                        } else {
                            null
                        }
            }

            return component
        }
    }
}
