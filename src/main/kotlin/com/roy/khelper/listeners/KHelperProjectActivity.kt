package com.roy.khelper.listeners

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.roy.khelper.server.CompetitiveCompanionServer

/** Starts the Competitive Companion server when the first project opens. Later calls do nothing. */
class KHelperProjectActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        CompetitiveCompanionServer.getInstance().ensureStarted(project)
    }
}
