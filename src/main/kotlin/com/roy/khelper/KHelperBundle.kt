package com.roy.khelper

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey

private const val BUNDLE = "messages.KHelperBundle"

internal object KHelperBundle {
    private val instance = DynamicBundle(KHelperBundle::class.java, BUNDLE)

    @Nls
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any?): String =
        instance.getMessage(key, *params)
}
