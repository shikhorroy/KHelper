package com.roy.khelper.runner

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

object RemoteTestRunner {

    // ANSI Color Codes
    private const val ANSI_RESET = "\u001B[0m"
    private const val ANSI_GREEN = "\u001B[32m"
    private const val ANSI_RED = "\u001B[31m"
    private const val ANSI_RED_BACKGROUND = "\u001B[41m"
    private const val ANSI_GREEN_BACKGROUND = "\u001B[42m"

    data class TestCase(val input: String, val output: String)

    data class RunResult(
        val passed: Boolean,
        val verdict: String,
        val actualOutput: String,
        val executionTime: Long,
        val exception: String? = null
    )

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size < 2) {
            System.err.println("Usage: RemoteTestRunner <SolutionClassName> <TestDataFilePath>")
            return
        }

        val className = args[0]
        val testDataPath = args[1]

        try {
            // Priority 1: Try loading as a standard Class
            val solutionClass = Class.forName(className)
            runTestsForClass(solutionClass, testDataPath)
        } catch (e: ClassNotFoundException) {
            // Priority 2: Try loading as a Top-Level File Class (ClassName + "Kt")
            try {
                val ktClassName = className + "Kt"
                val solutionClass = Class.forName(ktClassName)
                runTestsForClass(solutionClass, testDataPath)
            } catch (e2: ClassNotFoundException) {
                System.err.println(
                    "\n${ANSI_RED}ERROR: Could not load class: $className or ${className}Kt${ANSI_RESET}"
                )
                System.err.println("Please check if the file exists and package name is correct.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun runTestsForClass(solutionClass: Class<*>, testDataPath: String) {
        val testCases = loadTestCases(testDataPath)

        println("Running ${testCases.size} active test cases...")
        println()

        var passedCount = 0

        testCases.forEachIndexed { index, testCase ->
            val result = runSingleTest(solutionClass, testCase)

            val color = if (result.passed) ANSI_GREEN else ANSI_RED

            print("Test ${index + 1}: $color ${result.verdict}$ANSI_RESET")
            print(" (${result.executionTime}ms)")
            println()

            if (!result.passed) {
                println("-".repeat(30))
                if (result.exception != null) {
                    println("${ANSI_RED}Error: ${result.exception}${ANSI_RESET}")
                } else {
                    println("Input:\n${testCase.input.trim().take(100)}\n")
                    println("Expected:\n${testCase.output.trim().take(100)}\n")
                    println(
                        "Got:\n${ANSI_RED}${result.actualOutput.trim().take(100)}${ANSI_RESET}\n"
                    )
                }
            } else {
                println()
            }

            if (result.passed) passedCount++
        }

        println("=".repeat(50))
        if (passedCount == testCases.size) {
            println("${ANSI_GREEN}All tests passed! ✓${ANSI_RESET}")
        } else {
            println("${ANSI_RED}Summary: $passedCount/${testCases.size} passed${ANSI_RESET}")
        }
        println("=".repeat(50))
    }

    private fun runSingleTest(solutionClass: Class<*>, testCase: TestCase): RunResult {
        val originalIn = System.`in`
        val originalOut = System.out

        return try {
            val inputStream = ByteArrayInputStream(testCase.input.toByteArray())
            val outputStream = ByteArrayOutputStream()

            System.setIn(inputStream)
            System.setOut(PrintStream(outputStream))

            val startTime = System.currentTimeMillis()

            // Execution Logic: Support both 'solve' (Instance) and 'main' (Static)
            var invoked = false

            // 1. Try Legacy: solutionClass.solve()
            try {
                val method = solutionClass.getMethod("solve")
                val instance = solutionClass.getDeclaredConstructor().newInstance()
                method.invoke(instance)
                invoked = true
            } catch (e: NoSuchMethodException) {
                // 2. Try Top-Level: solutionClass.main(Array<String>)
                try {
                    val mainMethod = solutionClass.getMethod("main", Array<String>::class.java)
                    mainMethod.invoke(null, emptyArray<String>())
                    invoked = true
                } catch (e2: NoSuchMethodException) {
                    // 3. Try Parameterless main: solutionClass.main()
                    try {
                        val mainMethodNoArgs = solutionClass.getMethod("main")
                        mainMethodNoArgs.invoke(null)
                        invoked = true
                    } catch (e3: NoSuchMethodException) {
                        throw RuntimeException(
                            "No valid entry point found (solve() or main()) in class ${solutionClass.name}"
                        )
                    }
                }
            }

            val executionTime = System.currentTimeMillis() - startTime

            val actualOutput = outputStream.toString().trim()
            val expectedOutput = testCase.output.trim()

            if (actualOutput == expectedOutput) {
                RunResult(true, "ACCEPTED", actualOutput, executionTime)
            } else {
                RunResult(false, "WRONG ANSWER", actualOutput, executionTime)
            }
        } catch (e: Exception) {
            val cause = e.cause ?: e
            RunResult(false, "RUNTIME ERROR", "", 0, cause.message ?: cause.javaClass.simpleName)
        } finally {
            System.setIn(originalIn)
            System.setOut(originalOut)
        }
    }

    private fun loadTestCases(path: String): List<TestCase> {
        val file = File(path)
        if (!file.exists()) return emptyList()

        val testCases = mutableListOf<TestCase>()
        val content = file.readText()
        val cases = content.split("---TESTCASE---").filter { it.isNotBlank() }

        cases.forEach { caseStr ->
            val parts = caseStr.split("---SEPARATOR---")
            if (parts.size >= 2) {
                testCases.add(TestCase(parts[0], parts[1]))
            }
        }

        return testCases
    }
}
