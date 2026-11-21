package com.roy.khelper.services

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.roy.khelper.model.Problem
import com.roy.khelper.model.TestCase

class ProblemManagerServiceTest : BasePlatformTestCase() {

    private val problem = Problem(name = "A. Test", tests = listOf(TestCase("1", "one"), TestCase("2", "two")))

    fun `test duplicate inserts a copy right after the selected test`() {
        val service = ProblemManagerService.getInstance(project)
        service.addProblem(problem, "/p/KSolution.kt", "KOTLIN")
        service.updateTestCase("a_test", "KOTLIN", 0, TestCase("1", "one", active = false))

        service.duplicateTestCase("a_test", "KOTLIN", 0)

        assertEquals(
            listOf(TestCase("1", "one", false), TestCase("1", "one", false), TestCase("2", "two")),
            service.getTestCases("a_test", "KOTLIN"),
        )
    }

    fun `test duplicate with an invalid index changes nothing`() {
        val service = ProblemManagerService.getInstance(project)
        service.addProblem(problem, "/p/KSolution.kt", "KOTLIN")

        service.duplicateTestCase("a_test", "KOTLIN", 5)

        assertEquals(problem.tests, service.getTestCases("a_test", "KOTLIN"))
    }
}
