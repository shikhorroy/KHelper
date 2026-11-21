package com.roy.khelper.listeners

import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.roy.khelper.server.CompetitiveCompanionServer

/**
 * Starts the server when the plugin is installed or updated without an IDE restart. Startup activities do not run
 * for projects that are already open in that case.
 */
class KHelperDynamicPluginListener : DynamicPluginListener {
    override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
        if (pluginDescriptor.pluginId.idString == PLUGIN_ID) {
            CompetitiveCompanionServer.getInstance().ensureStarted()
        }
    }

    private companion object {
        const val PLUGIN_ID = "com.roy.khelper"
    }
}
