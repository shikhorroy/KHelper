package com.roy.khelper.ui.actions

import com.intellij.openapi.project.Project
import com.roy.khelper.services.ProblemManagerService.ProblemData
import com.roy.khelper.ui.ProblemCommands

class CopySubmissionAction : AbstractProblemAction() {
    override fun perform(project: Project, problemData: ProblemData) = ProblemCommands.copySubmission(project, problemData)
}
