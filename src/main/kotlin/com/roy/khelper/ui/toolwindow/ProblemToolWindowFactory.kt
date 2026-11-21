package com.roy.khelper.ui.toolwindow

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
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
     * The KHelper actions as icons in the tool window title bar. They come from the Tools | KHelper menu group, so
     * both places always show the same actions in the same order. They are enabled only while a KHelper run
     * configuration is selected.
     */
    private fun titleActions(): List<AnAction> {
        val actionManager = ActionManager.getInstance()
        val group = actionManager.getAction(ACTION_GROUP_ID) as? DefaultActionGroup ?: return emptyList()
        return group.getChildren(actionManager).toList()
    }

    private companion object {
        const val ACTION_GROUP_ID = "KHelper.ActionGroup"
    }
}
