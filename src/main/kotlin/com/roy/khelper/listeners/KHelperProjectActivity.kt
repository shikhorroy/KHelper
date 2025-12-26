package com.roy.khelper.listeners

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.Disposer
import com.roy.khelper.server.CompetitiveCompanionServer
import com.roy.khelper.server.listeners.ProjectProblemListener

class KHelperProjectActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val server = CompetitiveCompanionServer.getInstance()
        val listener = ProjectProblemListener(project)
        server.addListener(listener)

        // Unregister listener when project is closed
        // We create a disposable that removes the listener, and register it to the project
        // so it gets disposed (and thus the listener removed) when the project is closed.
        Disposer.register(project, Disposable {
            server.removeListener(listener)
        })
    }
}

