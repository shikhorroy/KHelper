package com.roy.khelper.generator

import com.roy.khelper.model.Problem
import com.roy.khelper.settings.PluginSettings
import com.roy.khelper.settings.PluginSettings.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KSolutionGeneratorTest {

    private val problem = Problem(name = "A. Watermelon", timeLimit = 1000, memoryLimit = 64)

    @Test
    fun `template variables are replaced`() {
        val template = $$"${PROBLEM_ID}|${PROBLEM_NAME}|${TIME_LIMIT}|${MEMORY_LIMIT}|${CLASS_NAME}"
        assertEquals(
            "a_watermelon|A. Watermelon|1000|64|JSolution",
            KSolutionGenerator.applyTemplate(template, problem, Language.JAVA),
        )
    }

    @Test
    fun `default templates declare the problem package`() {
        assertTrue(
            KSolutionGenerator.applyTemplate(PluginSettings.DEFAULT_KOTLIN_TEMPLATE, problem, Language.KOTLIN)
                .startsWith("package problems.a_watermelon\n")
        )
        assertTrue(
            KSolutionGenerator.applyTemplate(PluginSettings.DEFAULT_JAVA_TEMPLATE, problem, Language.JAVA)
                .startsWith("package problems.a_watermelon;\n")
        )
    }

    @Test
    fun `solution class name matches the generated file`() {
        assertEquals("problems.a_watermelon.KSolution", KSolutionGenerator.solutionClassName("a_watermelon", Language.KOTLIN))
        assertEquals("problems.a_watermelon.JSolution", KSolutionGenerator.solutionClassName("a_watermelon", Language.JAVA))
    }

    @Test
    fun `language lookup falls back to Kotlin for old or empty values`() {
        assertEquals(Language.JAVA, Language.fromName("JAVA"))
        assertEquals(Language.KOTLIN, Language.fromName(""))
        assertEquals(Language.JAVA, Language.fromFilePath("/p/src/main/java/problems/a/JSolution.java"))
    }
}
