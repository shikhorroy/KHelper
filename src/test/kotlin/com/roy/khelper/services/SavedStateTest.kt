package com.roy.khelper.services

import com.intellij.openapi.util.JDOMUtil
import com.intellij.util.xmlb.XmlSerializer
import com.roy.khelper.model.Problem
import com.roy.khelper.model.TestCase
import com.roy.khelper.settings.PluginSettings
import org.junit.Assert.assertEquals
import org.junit.Test

/** Saved problems and settings from released versions must still load. */
class SavedStateTest {

    @Test
    fun `problem state survives a save and load`() {
        val state = ProblemManagerService.State(
            mutableMapOf(
                "KOTLIN:a_test" to ProblemManagerService.ProblemData(
                    problem = Problem(name = "A. Test", group = "Codeforces", url = "https://x", timeLimit = 1000),
                    filePath = "/p/src/main/kotlin/problems/a_test/KSolution.kt",
                    testCases = mutableListOf(TestCase("1 2", "3"), TestCase("5 5", "10", active = false)),
                    language = "KOTLIN",
                )
            )
        )

        val loaded = XmlSerializer.deserialize(XmlSerializer.serialize(state), ProblemManagerService.State::class.java)

        assertEquals(state, loaded)
    }

    @Test
    fun `settings saved by version 1 load, and the removed options are ignored`() {
        val saved = JDOMUtil.load(
            """
            <State>
              <option name="serverPort" value="12345" />
              <option name="preferredLanguage" value="JAVA" />
              <option name="outputDirectory" value="out" />
              <option name="autoGenerateMain" value="true" />
              <option name="useFastIO" value="true" />
            </State>
            """.trimIndent()
        )

        val state = XmlSerializer.deserialize(saved, PluginSettings.State::class.java)

        assertEquals(12345, state.serverPort)
        assertEquals(PluginSettings.Language.JAVA, state.preferredLanguage)
        assertEquals("out", state.outputDirectory)
        assertEquals(PluginSettings.DEFAULT_KOTLIN_TEMPLATE, state.solutionTemplate)
    }
}
