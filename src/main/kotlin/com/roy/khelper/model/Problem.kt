package com.roy.khelper.model

import kotlinx.serialization.Serializable

@Serializable
data class Problem(
    var name: String = "",
    var group: String = "",
    var url: String = "",
    var timeLimit: Int = 0, // in milliseconds
    var memoryLimit: Int = 0, // in megabytes
    var tests: List<TestCase> = emptyList(),
    var input: InputOutput? = null,
    var output: InputOutput? = null,
    var languages: Languages? = null,
    var batch: Batch? = null
) {
    @Serializable
    data class InputOutput(
        var type: String = "" // "stdin", "stdout", "file", etc.
    )

    @Serializable
    data class Languages(var java: JavaLanguage? = null)

    @Serializable
    data class JavaLanguage(var mainClass: String? = null, var taskClass: String? = null)

    @Serializable
    data class Batch(var id: String = "", var size: Int = 0)

    /** Generate a sanitized ID from the problem name */
    fun generateId(): String {
        // Replace any non-alphanumeric characters with underscore, collapse repeats,
        // trim leading/trailing underscores and lowercase the result.
        var id = name
            .trim()
            .replace(Regex("[^A-Za-z0-9]+"), "_")
            .replace(Regex("_+"), "_")
            .replace(Regex("^_+|_+$"), "")
            .lowercase()

        // If sanitization produced an empty string, fall back to a deterministic id
        if (id.isEmpty()) {
            val h = name.hashCode().toUInt().toString()
            id = "problem_$h"
        }

        // Package identifiers must not start with a digit - prefix with 'p' if necessary
        if (id.first().isDigit()) {
            id = "p$id"
        }

        return id
    }

    /** Get a display-friendly name for the problem */
    fun getDisplayName(): String {
        return name
    }

    /** Get the contest/category name */
    fun getContestName(): String {
        return group.substringBefore(" - ").ifEmpty { "Unknown Contest" }
    }
}
