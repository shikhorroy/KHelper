package com.roy.khelper.runner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import com.intellij.util.PathUtil
import org.junit.Test
import java.io.File
import java.nio.file.Files

class RemoteTestRunnerTest {

    @Test
    fun `passing tests exit with 0 and each test gets fresh static state`() {
        val run = runRunner("fixtures.SumSolution", "1 2" to "3", "10 20" to "30\n")
        assertEquals(run.output, RemoteTestRunner.EXIT_PASSED, run.exitCode)
        assertTrue(run.output, run.output.contains("All tests passed! (2/2)"))
    }

    @Test
    fun `wrong answer exits with 1`() {
        val run = runRunner("fixtures.WrongSolution", "" to "right")
        assertEquals(run.output, RemoteTestRunner.EXIT_FAILED, run.exitCode)
        assertTrue(run.output, run.output.contains("WRONG ANSWER"))
    }

    @Test
    fun `exception exits with 1 and shows the error`() {
        val run = runRunner("fixtures.CrashingSolution", "" to "")
        assertEquals(run.output, RemoteTestRunner.EXIT_FAILED, run.exitCode)
        assertTrue(run.output, run.output.contains("RUNTIME ERROR"))
        assertTrue(run.output, run.output.contains("boom"))
    }

    @Test
    fun `deep recursion works on the large solution stack`() {
        val run = runRunner("fixtures.DeepRecursionSolution", "200000" to "200000")
        assertEquals(run.output, RemoteTestRunner.EXIT_PASSED, run.exitCode)
    }

    @Test
    fun `missing class exits with 2`() {
        val run = runRunner("fixtures.NoSuchSolution", "" to "")
        assertEquals(run.output, RemoteTestRunner.EXIT_ERROR, run.exitCode)
    }

    @Test
    fun `output comparison ignores line endings and trailing whitespace`() {
        assertEquals(RemoteTestRunner.normalize("1 2\n3\n"), RemoteTestRunner.normalize("1 2  \r\n3\r\n\r\n"))
        assertTrue(RemoteTestRunner.normalize("1 2") != RemoteTestRunner.normalize("1  2"))
    }

    private class Run(val exitCode: Int, val output: String)

    /** Starts the runner in a new JVM, like the IDE does, with the runner, the fixtures and the Kotlin stdlib. */
    private fun runRunner(className: String, vararg tests: Pair<String, String>): Run {
        val dir = Files.createTempDirectory("khelper_runner_test")
        tests.forEachIndexed { index, (input, expected) ->
            Files.writeString(dir.resolve("${index + 1}.in"), input)
            Files.writeString(dir.resolve("${index + 1}.out"), expected)
        }
        val classpath = listOf(RemoteTestRunner::class.java, WrongSolutionMarker, KotlinVersion::class.java)
            .map { PathUtil.getJarPathForClass(it) }
            .distinct()
            .joinToString(File.pathSeparator)
        val java = ProcessHandle.current().info().command().orElse("java")

        val process = ProcessBuilder(java, "-cp", classpath, RemoteTestRunner::class.java.name, className, dir.toString(), "2000")
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        dir.toFile().deleteRecursively()
        return Run(exitCode, output)
    }

    private companion object {
        //~ Any class from the test output folder, to find that folder for the child JVM classpath
        val WrongSolutionMarker: Class<*> = fixtures.WrongSolution::class.java
    }
}
