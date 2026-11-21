package com.roy.khelper.icons

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/** Icons used from code. The "_dark" variants next to each file are picked for dark themes automatically. */
object KHelperIcons {
    //~ The 40x40 plugin logo, also shown large in the empty tool window
    @JvmField
    val Logo: Icon = load("/META-INF/pluginIcon.svg")

    @JvmField
    val RunConfiguration: Icon = load("/icons/runConfiguration.svg")

    @JvmField
    val Add: Icon = load("/icons/add.svg")

    @JvmField
    val Delete: Icon = load("/icons/delete.svg")

    @JvmField
    val Edit: Icon = load("/icons/edit.svg")

    @JvmField
    val Duplicate: Icon = load("/icons/duplicate.svg")

    @JvmField
    val CheckAll: Icon = load("/icons/check_all.svg")

    @JvmField
    val UncheckAll: Icon = load("/icons/uncheck_all.svg")

    private fun load(path: String): Icon = IconLoader.getIcon(path, KHelperIcons::class.java)
}
