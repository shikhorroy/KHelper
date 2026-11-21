package com.roy.khelper.services

import com.intellij.execution.RunManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.roy.khelper.model.Problem
import com.roy.khelper.model.TestCase
import com.roy.khelper.runner.KSolutionRunConfiguration
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Stores the imported problems of a project and their test cases.
 *
 * The state is saved with the IntelliJ XML serializer. The state name, file name and property names are the
 * stored format of released versions, so never rename them.
 */
@Service(Service.Level.PROJECT)
@State(name = "KHelperProblems", storages = [Storage("khelper-problems.xml")])
class ProblemManagerService(private val project: Project) : PersistentStateComponent<ProblemManagerService.State> {

    private val problems = ConcurrentHashMap<String, ProblemData>()

    data class State(var problemsMap: MutableMap<String, ProblemData> = mutableMapOf())

    data class ProblemData(
        //~ Test cases are kept in testCases. problem.tests is only the copy sent by Competitive Companion.
        var problem: Problem = Problem(),
        var filePath: String = "",
        var testCases: MutableList<TestCase> = mutableListOf(),
        var language: String = "",
    )

    override fun getState(): State = State(problemsMap = problems.toMutableMap())

    override fun loadState(state: State) {
        problems.clear()
        problems.putAll(state.problemsMap)
    }

    fun addProblem(problem: Problem, filePath: String, language: String) {
        val problemId = problem.generateId()
        problems[key(problemId, language)] = ProblemData(
            //~ Test cases are stored once, in testCases, so the saved state does not hold them twice
            problem = problem.copy(tests = emptyList()),
            filePath = filePath,
            testCases = problem.tests.toMutableList(),
            language = language,
        )
        publisher().problemAdded(problem)
    }

    fun getProblem(problemId: String, language: String): ProblemData? = problems[key(problemId, language)]

    fun getAllProblems(): List<ProblemData> = problems.values.toList()

    /** The problem of the selected KHelper run configuration, or null when another configuration is selected. */
    fun selectedProblem(): ProblemData? {
        val config = RunManager.getInstance(project).selectedConfiguration?.configuration as? KSolutionRunConfiguration
            ?: return null
        return getProblem(config.problemId, config.language)
    }

    /** Deletes the solution file, its folder when empty, the run configurations and the saved data. Runs on EDT. */
    fun deleteProblem(problemId: String, language: String) {
        val problemData = problems[key(problemId, language)] ?: return

        WriteCommandAction.writeCommandAction(project)
            .withName("Delete Problem ${problemData.problem.name}")
            .run<IOException> {
                val file = LocalFileSystem.getInstance().refreshAndFindFileByPath(problemData.filePath)
                val dir = file?.parent
                file?.delete(this)
                if (dir != null && dir.isValid && dir.children.isEmpty()) {
                    dir.delete(this)
                }
            }

        val runManager = RunManager.getInstance(project)
        runManager.allSettings
            .filter { settings ->
                val config = settings.configuration as? KSolutionRunConfiguration
                config != null && config.problemId == problemId && config.language == language
            }
            .forEach { runManager.removeConfiguration(it) }

        problems.remove(key(problemId, language))
        publisher().problemDeleted(problemId)
    }

    fun addTestCase(problemId: String, language: String, testCase: TestCase) =
        updateTestCases(problemId, language) { it.add(testCase) }

    fun removeTestCase(problemId: String, language: String, index: Int) =
        updateTestCases(problemId, language) { if (index in it.indices) it.removeAt(index) }

    /** Inserts a copy of the test case at [index] right after it. */
    fun duplicateTestCase(problemId: String, language: String, index: Int) =
        updateTestCases(problemId, language) { if (index in it.indices) it.add(index + 1, it[index].copy()) }

    fun updateTestCase(problemId: String, language: String, index: Int, testCase: TestCase) =
        updateTestCases(problemId, language) { if (index in it.indices) it[index] = testCase }

    fun setAllActive(problemId: String, language: String, active: Boolean) =
        updateTestCases(problemId, language) { list -> list.replaceAll { it.copy(active = active) } }

    /** A snapshot of the test cases. Change them only through this service, so listeners are told. */
    fun getTestCases(problemId: String, language: String): List<TestCase> {
        val testCases = problems[key(problemId, language)]?.testCases ?: return emptyList()
        return synchronized(testCases) { testCases.toList() }
    }

    private fun updateTestCases(problemId: String, language: String, change: (MutableList<TestCase>) -> Unit) {
        val testCases = problems[key(problemId, language)]?.testCases ?: return
        synchronized(testCases) { change(testCases) }
        publisher().testCaseUpdated(problemId)
    }

    private fun key(problemId: String, language: String): String = "${language.uppercase()}:$problemId"

    private fun publisher(): ProblemNotifier = project.messageBus.syncPublisher(ProblemNotifier.TOPIC)

    companion object {
        fun getInstance(project: Project): ProblemManagerService = project.service()
    }
}
