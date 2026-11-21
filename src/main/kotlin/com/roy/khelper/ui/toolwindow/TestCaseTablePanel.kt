package com.roy.khelper.ui.toolwindow

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
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
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer

/**
 * The test cases of the shown problem. Changes go through [ProblemManagerService]. The service publishes them and
 * the tool window calls [updateProblem] again.
 */
class TestCaseTablePanel(private val project: Project) : JPanel(BorderLayout()) {

    private var problemId: String? = null
    private var language: String? = null
    private val tableModel = TestCaseTableModel()

    private val table = JBTable(tableModel).apply {
        setShowGrid(true)
        rowHeight = JBUI.scale(28)
        columnModel.getColumn(COLUMN_ACTIVE).apply {
            preferredWidth = JBUI.scale(30)
            maxWidth = JBUI.scale(30)
        }
        columnModel.getColumn(COLUMN_NUMBER).apply {
            preferredWidth = JBUI.scale(40)
            maxWidth = JBUI.scale(60)
        }
        columnModel.getColumn(COLUMN_INPUT).preferredWidth = JBUI.scale(285)
        columnModel.getColumn(COLUMN_OUTPUT).preferredWidth = JBUI.scale(285)
        setDefaultRenderer(String::class.java, PreviewCellRenderer())

        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2 && selectedRow >= 0) editTestCase()
            }
        })
    }

    private val headerLabel = JBLabel("Test Cases").apply {
        font = font.deriveFont(Font.BOLD)
        border = JBUI.Borders.empty(8, 10)
    }

    init {
        border = JBUI.Borders.empty(10)

        val decorator = ToolbarDecorator.createDecorator(table)
            .disableAddAction()
            .disableRemoveAction()
            .addExtraActions(
                tableAction("Enable All", "Enable all test cases", KHelperIcons.CheckAll) { setAllActive(true) },
                tableAction("Disable All", "Disable all test cases", KHelperIcons.UncheckAll) { setAllActive(false) },
                tableAction("Add Test Case", "Add new test case", KHelperIcons.Add) { addTestCase() },
                tableAction("Remove Test Case", "Remove selected test case", KHelperIcons.Delete, true) {
                    removeTestCase()
                },
                tableAction("Edit Test Case", "Edit selected test case", KHelperIcons.Edit, true) { editTestCase() },
            )

        val contentPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createEtchedBorder()
            add(headerLabel, BorderLayout.NORTH)
            add(decorator.createPanel(), BorderLayout.CENTER)
        }
        add(contentPanel, BorderLayout.CENTER)
    }

    fun updateProblem(problemId: String?, language: String?) {
        this.problemId = problemId
        this.language = language

        val testCases = if (problemId != null && language != null) {
            service().getTestCases(problemId, language)
        } else {
            emptyList()
        }
        tableModel.setTestCases(testCases)
        headerLabel.text = if (problemId != null) "Test Cases (${testCases.size})" else "Test Cases"
    }

    private fun addTestCase() {
        val problemId = problemId ?: return
        val language = language ?: return
        val dialog = AddTestCaseDialog(project)
        if (dialog.showAndGet()) {
            service().addTestCase(problemId, language, dialog.getTestCase())
        }
    }

    private fun removeTestCase() {
        val problemId = problemId ?: return
        val language = language ?: return
        val row = table.selectedRow.takeIf { it >= 0 } ?: return
        service().removeTestCase(problemId, language, row)
    }

    private fun editTestCase() {
        val problemId = problemId ?: return
        val language = language ?: return
        val row = table.selectedRow.takeIf { it >= 0 } ?: return
        val testCase = tableModel.testCaseAt(row)

        val dialog = AddTestCaseDialog(project, testCase)
        if (dialog.showAndGet()) {
            service().updateTestCase(problemId, language, row, dialog.getTestCase().copy(active = testCase.active))
        }
    }

    private fun setAllActive(active: Boolean) {
        val problemId = problemId ?: return
        val language = language ?: return
        service().setAllActive(problemId, language, active)
    }

    private fun service() = ProblemManagerService.getInstance(project)

    private fun tableAction(
        text: String,
        description: String,
        icon: Icon,
        needsSelection: Boolean = false,
        perform: () -> Unit,
    ) = object : DumbAwareAction(text, description, icon) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = problemId != null && (!needsSelection || table.selectedRow >= 0)
        }

        override fun actionPerformed(e: AnActionEvent) = perform()
    }

    private inner class TestCaseTableModel : AbstractTableModel() {
        private var testCases = listOf<TestCase>()

        fun testCaseAt(row: Int): TestCase = testCases[row]

        fun setTestCases(testCases: List<TestCase>) {
            val selected = table.selectedRow
            this.testCases = testCases
            fireTableDataChanged()
            if (selected in testCases.indices) table.setRowSelectionInterval(selected, selected)
        }

        override fun getRowCount(): Int = testCases.size

        override fun getColumnCount(): Int = 4

        override fun getColumnName(column: Int): String = when (column) {
            COLUMN_NUMBER -> "#"
            COLUMN_INPUT -> "Input"
            COLUMN_OUTPUT -> "Expected Output"
            else -> ""
        }

        override fun getColumnClass(columnIndex: Int): Class<*> = when (columnIndex) {
            COLUMN_ACTIVE -> java.lang.Boolean::class.java
            COLUMN_NUMBER -> Integer::class.java
            else -> String::class.java
        }

        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = columnIndex == COLUMN_ACTIVE

        override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
            val problemId = problemId ?: return
            val language = language ?: return
            if (columnIndex == COLUMN_ACTIVE && aValue is Boolean) {
                service().updateTestCase(problemId, language, rowIndex, testCases[rowIndex].copy(active = aValue))
            }
        }

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val testCase = testCases[rowIndex]
            return when (columnIndex) {
                COLUMN_ACTIVE -> testCase.active
                COLUMN_NUMBER -> rowIndex + 1
                COLUMN_INPUT -> testCase.input
                COLUMN_OUTPUT -> testCase.output
                else -> ""
            }
        }
    }

    /** Shows the first characters of long input or output, and the start of the full text as a tooltip. */
    private class PreviewCellRenderer : DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(
            table: JTable,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int,
        ): Component {
            val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
            if (value is String) {
                val long = value.length > PREVIEW_CHARS
                text = if (long) value.take(PREVIEW_CHARS) + "..." else value
                toolTipText = if (long) {
                    "<html><pre>${StringUtil.escapeXmlEntities(value.take(TOOLTIP_CHARS))}</pre></html>"
                } else {
                    null
                }
            }
            return component
        }
    }

    private companion object {
        const val COLUMN_ACTIVE = 0
        const val COLUMN_NUMBER = 1
        const val COLUMN_INPUT = 2
        const val COLUMN_OUTPUT = 3
        const val PREVIEW_CHARS = 50
        const val TOOLTIP_CHARS = 500
    }
}
