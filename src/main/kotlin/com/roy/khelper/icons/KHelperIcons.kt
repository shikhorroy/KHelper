package com.roy.khelper.icons

import com.intellij.openapi.util.IconLoader

object KHelperIcons {
    @JvmField
    val PluginIcon = IconLoader.getIcon("/icons/pluginIcon.svg", javaClass)

    @JvmField
    val Add = IconLoader.getIcon("/icons/add.svg", javaClass)

    @JvmField
    val Delete = IconLoader.getIcon("/icons/delete.svg", javaClass)

    @JvmField
    val Run = IconLoader.getIcon("/icons/run.svg", javaClass)

    @JvmField
    val Copy = IconLoader.getIcon("/icons/copy.svg", KHelperIcons::class.java)
    val Browser = IconLoader.getIcon("/icons/browser.svg", KHelperIcons::class.java)
    val Edit = IconLoader.getIcon("/icons/edit.svg", KHelperIcons::class.java)
    val CheckAll = IconLoader.getIcon("/icons/check_all.svg", KHelperIcons::class.java)
    val UncheckAll = IconLoader.getIcon("/icons/uncheck_all.svg", KHelperIcons::class.java)
}
