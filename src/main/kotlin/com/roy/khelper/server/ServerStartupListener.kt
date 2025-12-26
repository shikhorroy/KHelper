package com.roy.khelper.server

import com.intellij.ide.AppLifecycleListener
import com.roy.khelper.settings.PluginSettings

class ServerStartupListener : AppLifecycleListener {

    override fun appFrameCreated(commandLineArgs: MutableList<String>) {
        // Start the HTTP server when the IDE starts
        val settings = PluginSettings.getInstance()
        val server = CompetitiveCompanionServer.getInstance()

        if (!server.isRunning()) {
            server.start(settings.state.serverPort)
        }
    }
}
