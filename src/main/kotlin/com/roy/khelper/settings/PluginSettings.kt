package com.roy.khelper.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
@State(name = "KHelperSettings", storages = [Storage("khelper.xml")])
class PluginSettings : PersistentStateComponent<PluginSettings.State> {

    enum class Language {
        KOTLIN,
        JAVA
    }

    data class State(
            var serverPort: Int = 10043,
            var preferredLanguage: Language = Language.KOTLIN,
            var solutionTemplate: String = DEFAULT_KOTLIN_TEMPLATE,
            var javaSolutionTemplate: String = DEFAULT_JAVA_TEMPLATE,
            var outputDirectory: String = "output",
            var autoGenerateMain: Boolean = true,
            var useFastIO: Boolean = false
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    companion object {
        fun getInstance(): PluginSettings {
            return ApplicationManager.getApplication().getService(PluginSettings::class.java)
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
