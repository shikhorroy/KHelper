package com.roy.khelper.server

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.IdeFocusManager
import com.roy.khelper.model.Problem
import com.roy.khelper.services.KHelperNotifications
import com.roy.khelper.settings.PluginSettings
import java.io.IOException

/**
 * Owns the Competitive Companion server for the whole IDE. One IDE has one server, and each received problem goes
 * to the project the user worked in last, never to every open project.
 */
@Service(Service.Level.APP)
class CompetitiveCompanionServer : Disposable {

    private val log = logger<CompetitiveCompanionServer>()

    private val server = CompanionHttpServer(
        onProblem = ::dispatch,
        onBadRequest = { log.warn("Ignored a request that is not a Competitive Companion problem", it) },
    )

    val isRunning: Boolean get() = server.isRunning

    /** Starts the server on the configured port. Safe to call many times. Tells the user when the port is busy. */
    @Synchronized
    fun ensureStarted(project: Project? = null) {
        if (server.isRunning) return

        val port = Integer.getInteger(PORT_PROPERTY) ?: PluginSettings.getInstance().state.serverPort
        try {
            server.start(port)
            log.info("Competitive Companion server started on port $port")
        } catch (e: IOException) {
            log.warn("Could not start the Competitive Companion server on port $port", e)
            KHelperNotifications.error(
                project,
                "Could not listen on port $port: ${e.message}. Another IDE or tool may use it. " +
                        "Change the port in Settings | Tools | KHelper, and in Competitive Companion.",
                "KHelper server not started",
            )
        }
    }

    /** Restarts the server, for example after the port changed in settings. */
    @Synchronized
    fun restart() {
        server.stop()
        ensureStarted()
    }

    override fun dispose() {
        server.stop()
    }

    private fun dispatch(problem: Problem) {
        log.info("Received problem: ${problem.name}")
        ApplicationManager.getApplication().invokeLater {
            val project = activeProject()
            if (project == null) {
                KHelperNotifications.warning(null, "Received '${problem.name}', but no project is open.")
                return@invokeLater
            }
            ProblemImporter.import(project, problem)
        }
    }

    private fun activeProject(): Project? {
        val focused = IdeFocusManager.getGlobalInstance().lastFocusedFrame?.project
        if (focused != null && !focused.isDisposed && !focused.isDefault) return focused
        return ProjectManager.getInstance().openProjects.firstOrNull { !it.isDisposed }
    }

    companion object {
        //~ JVM property that overrides the settings port, for example -Dkhelper.server.port=10044 in runIde
        const val PORT_PROPERTY = "khelper.server.port"

        fun getInstance(): CompetitiveCompanionServer = service()
    }
}
