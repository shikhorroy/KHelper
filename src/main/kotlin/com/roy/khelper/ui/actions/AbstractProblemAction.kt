package com.roy.khelper.ui.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.roy.khelper.services.ProblemManagerService
import com.roy.khelper.services.ProblemManagerService.ProblemData

/**
 * An action on the problem of the selected KHelper run configuration. It is disabled when no such configuration
 * is selected. Text, description and icon come from plugin.xml.
 */
abstract class AbstractProblemAction : DumbAwareAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabled = project != null && ProblemManagerService.getInstance(project).selectedProblem() != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val problemData = ProblemManagerService.getInstance(project).selectedProblem() ?: return
        perform(project, problemData)
    }

    protected abstract fun perform(project: Project, problemData: ProblemData)
}
