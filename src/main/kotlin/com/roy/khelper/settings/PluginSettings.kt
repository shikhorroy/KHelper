package com.roy.khelper.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

@Service(Service.Level.APP)
@State(name = "KHelperSettings", storages = [Storage("khelper.xml", roamingType = RoamingType.DISABLED)])
class PluginSettings : PersistentStateComponent<PluginSettings.State> {

    /** The solution language. The enum name is stored in settings and run configurations, so never rename it. */
    enum class Language(
        val displayName: String,
        val className: String,
        val extension: String,
        val sourceRoot: String,
    ) {
        KOTLIN("Kotlin", "KSolution", "kt", "kotlin"),
        JAVA("Java", "JSolution", "java", "java");

        val fileName: String get() = "$className.$extension"
        val mainFileName: String get() = "Main.$extension"

        override fun toString(): String = displayName

        companion object {
            //~ Falls back to Kotlin for empty or unknown names from old run configurations
            fun fromName(name: String?): Language = entries.firstOrNull { it.name == name } ?: KOTLIN

            fun fromFilePath(path: String): Language = if (path.endsWith(".java")) JAVA else KOTLIN
        }
    }

    data class State(
        var serverPort: Int = DEFAULT_PORT,
        var preferredLanguage: Language = Language.KOTLIN,
        var solutionTemplate: String = DEFAULT_KOTLIN_TEMPLATE,
        var javaSolutionTemplate: String = DEFAULT_JAVA_TEMPLATE,
        var outputDirectory: String = DEFAULT_OUTPUT_DIRECTORY,
        var autoGenerateMain: Boolean = true,
    ) {
        fun templateFor(language: Language): String = when (language) {
            Language.KOTLIN -> solutionTemplate
            Language.JAVA -> javaSolutionTemplate
        }

        fun outputDirectoryOrDefault(): String = outputDirectory.trim().ifEmpty { DEFAULT_OUTPUT_DIRECTORY }
    }

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    companion object {
        const val DEFAULT_PORT = 10043
        const val DEFAULT_OUTPUT_DIRECTORY = "output"

        fun getInstance(): PluginSettings = service()

        fun defaultTemplateFor(language: Language): String = when (language) {
            Language.KOTLIN -> DEFAULT_KOTLIN_TEMPLATE
            Language.JAVA -> DEFAULT_JAVA_TEMPLATE
        }

        const val DEFAULT_KOTLIN_TEMPLATE =
            $$"""package problems.${PROBLEM_ID}

import java.util.Scanner

fun main() {
    val scanner = Scanner(System.`in`)
    if (scanner.hasNext()) {
        val n = scanner.nextInt()
        println(n)
    }
}
"""

        const val DEFAULT_JAVA_TEMPLATE =
            $$"""package problems.${PROBLEM_ID};

import java.util.Scanner;

public class JSolution {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        if (scanner.hasNext()) {
            int n = scanner.nextInt();
            System.out.println(n);
        }
    }
}
"""
    }
}
