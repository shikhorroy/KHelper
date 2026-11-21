package com.roy.khelper.ui.toolwindow

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class ProblemToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val content = ContentFactory.getInstance().createContent(null, "", false)
        content.component = ProblemToolWindowPanel(project, content)
        toolWindow.contentManager.addContent(content)
        toolWindow.setTitleActions(titleActions())
    }

    /**
     * The KHelper actions as icons in the tool window title bar, next to its own menu. They are the same actions as
     * in Tools | KHelper, so they are enabled only while a KHelper run configuration is selected.
     */
    private fun titleActions(): List<AnAction> {
        val actionManager = ActionManager.getInstance()
        val problemActions = PROBLEM_ACTION_IDS.mapNotNull { actionManager.getAction(it) }
        val settings = listOfNotNull(actionManager.getAction(SETTINGS_ACTION_ID))
        return problemActions + Separator.getInstance() + settings
    }

    private companion object {
        //~ Delete is last, so it is not clicked by mistake when reaching for the others
        val PROBLEM_ACTION_IDS = listOf(
            "KHelper.OpenSource",
            "KHelper.OpenProblemUrl",
            "KHelper.CopySubmission",
            "KHelper.DeleteProblem",
        )
        const val SETTINGS_ACTION_ID = "KHelper.OpenSettings"
    }
}
