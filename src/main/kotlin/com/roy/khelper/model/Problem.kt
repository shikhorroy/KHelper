package com.roy.khelper.model

import kotlinx.serialization.Serializable

/**
 * A problem as sent by Competitive Companion. The class is also stored with the IntelliJ XML serializer, so every
 * property stays mutable, keeps a default value and keeps its name, or saved problems from older versions are lost.
 */
@Serializable
data class Problem(
    var name: String = "",
    var group: String = "",
    var url: String = "",
    //~ Time limit in milliseconds
    var timeLimit: Int = 0,
    //~ Memory limit in megabytes
    var memoryLimit: Int = 0,
    var tests: List<TestCase> = emptyList(),
    var input: InputOutput? = null,
    var output: InputOutput? = null,
    var languages: Languages? = null,
    var batch: Batch? = null,
) {
    @Serializable
    data class InputOutput(
        //~ "stdin", "stdout", "file", ...
        var type: String = "",
    )

    @Serializable
    data class Languages(var java: JavaLanguage? = null)

    @Serializable
    data class JavaLanguage(var mainClass: String? = null, var taskClass: String? = null)

    @Serializable
    data class Batch(var id: String = "", var size: Int = 0)

    /**
     * A sanitized id made from the problem name. It is used as a package name, so it never starts with a digit.
     * Saved problems and run configurations are keyed by it, so the rules must not change.
     */
    fun generateId(): String {
        var id = name
            .trim()
            .replace(NON_ALPHANUMERIC, "_")
            .trim('_')
            .lowercase()

        if (id.isEmpty()) {
            id = "problem_${name.hashCode().toUInt()}"
        }
        if (id.first().isDigit()) {
            id = "p$id"
        }
        return id
    }
}

//~ Not in a companion object: kotlinx.serialization puts the generated serializer() there
private val NON_ALPHANUMERIC = Regex("[^A-Za-z0-9]+")
