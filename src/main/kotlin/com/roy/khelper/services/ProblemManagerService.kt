package com.roy.khelper.services

import com.intellij.execution.RunManagerEx
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.roy.khelper.model.Problem
import com.roy.khelper.model.TestCase
import com.roy.khelper.runner.KSolutionConfigurationType
import com.roy.khelper.runner.KSolutionRunConfiguration
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
@State(name = "KHelperProblems", storages = [Storage("khelper-problems.xml")])
class ProblemManagerService(private val project: Project) :
    PersistentStateComponent<ProblemManagerService.State> {

    private val problems = ConcurrentHashMap<String, ProblemData>()

    // State class for XML serialization - must be mutable for XStream
    data class State(var problemsMap: MutableMap<String, ProblemData> = mutableMapOf())

    data class ProblemData(
        var problem: Problem = Problem("", "", "", 0, 0, emptyList()),
        var filePath: String = "",
        var testCases: MutableList<TestCase> = mutableListOf(),
        var language: String = ""
    ) {
        // No-arg constructor for XML deserialization
        constructor() : this(Problem("", "", "", 0, 0, emptyList()), "", mutableListOf(), "")
    }

    override fun getState(): State {
        return State(problemsMap = problems.toMutableMap())
    }

    override fun loadState(state: State) {
        problems.clear()
        problems.putAll(state.problemsMap)
    }

    private fun generateKey(problemId: String, language: String): String {
        return "${language.uppercase()}:$problemId"
    }

    fun addProblem(problem: Problem, filePath: String, language: String) {
        val problemId = problem.generateId()
        val key = generateKey(problemId, language)
        problems[key] =
            ProblemData(
                problem = problem,
                filePath = filePath,
                testCases = problem.tests.toMutableList(),
                language = language
            )
        project.messageBus.syncPublisher(ProblemNotifier.TOPIC).problemAdded(problem)
    }

    fun getProblem(problemId: String, language: String): ProblemData? {
        val key = generateKey(problemId, language)
        return problems[key]
    }

    fun getAllProblems(): List<ProblemData> {
        return problems.values.toList()
    }

    fun deleteProblem(problemId: String, language: String) {
        val key = generateKey(problemId, language)
        val problemData = problems[key] ?: return

        // Delete solution file and directory
        WriteAction.runAndWait<RuntimeException> {
            val file = File(problemData.filePath)
            if (file.exists()) {
                file.delete()
            }

            // Delete the parent directory if empty
            val parentDir = file.parentFile
            if (parentDir != null && parentDir.exists() && parentDir.listFiles()?.isEmpty() == true
            ) {
                parentDir.delete()
            }

            // Refresh VFS
            LocalFileSystem.getInstance().refresh(false)
        }

        // Remove run configurations
        val runManager = RunManagerEx.getInstanceEx(project)
        val configurationsToRemove =
            runManager.allSettings.filter { settings ->
                settings.type is KSolutionConfigurationType &&
                        (settings.configuration as? KSolutionRunConfiguration)?.problemId == problemId &&
                        (settings.configuration as? KSolutionRunConfiguration)?.language == language
            }

        configurationsToRemove.forEach { settings -> runManager.removeConfiguration(settings) }

        // Remove from a map
        problems.remove(key)

        // Notify listeners
        project.messageBus.syncPublisher(ProblemNotifier.TOPIC).problemDeleted(problemId)
    }

    fun addTestCase(problemId: String, language: String, testCase: TestCase) {
        val key = generateKey(problemId, language)
        problems[key]?.testCases?.add(testCase)
        project.messageBus.syncPublisher(ProblemNotifier.TOPIC).testCaseUpdated(problemId)
    }

    fun removeTestCase(problemId: String, language: String, index: Int) {
        val key = generateKey(problemId, language)
        problems[key]?.testCases?.removeAt(index)
        project.messageBus.syncPublisher(ProblemNotifier.TOPIC).testCaseUpdated(problemId)
    }

    fun updateTestCase(problemId: String, language: String, index: Int, testCase: TestCase) {
        val key = generateKey(problemId, language)
        problems[key]?.testCases?.set(index, testCase)
        project.messageBus.syncPublisher(ProblemNotifier.TOPIC).testCaseUpdated(problemId)
    }

    fun getTestCases(problemId: String, language: String): List<TestCase> {
        val key = generateKey(problemId, language)
        return problems[key]?.testCases ?: emptyList()
    }

    companion object {
        fun getInstance(project: Project): ProblemManagerService = project.service()
    }
}
