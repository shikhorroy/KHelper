package com.roy.khelper.runner

import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.JavaCommandLineState
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.PathUtil
import com.roy.khelper.generator.KSolutionGenerator
import com.roy.khelper.generator.MainGenerator
import com.roy.khelper.services.ProblemManagerService
import com.roy.khelper.settings.PluginSettings
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class KSolutionRunProfileState(
    environment: ExecutionEnvironment,
    private val configuration: KSolutionRunConfiguration,
) : JavaCommandLineState(environment) {

    //~ Temp folder with the test files of this run. Deleted when the process ends.
    private var testsDir: Path? = null

    override fun createJavaParameters(): JavaParameters {
        val project = environment.project
        val problemData = ProblemManagerService.getInstance(project)
            .getProblem(configuration.problemId, configuration.language)
            ?: throw ExecutionException(
                "Problem data not found for ID: ${configuration.problemId}, Language: ${configuration.language}"
            )

        if (problemData.filePath.isBlank()) {
            throw ExecutionException("Solution file path is not set for problem: ${problemData.problem.name}")
        }
        val solutionFile = LocalFileSystem.getInstance().findFileByPath(problemData.filePath)
            ?: throw ExecutionException("Solution file not found at path: ${problemData.filePath}")
        val module = ModuleUtilCore.findModuleForFile(solutionFile, project)
            ?: throw ExecutionException(
                "Module not found for file: ${solutionFile.path}. Make sure the file is in a source root of a module."
            )

        val params = JavaParameters()
        params.configureByModule(module, JavaParameters.JDK_AND_CLASSES_AND_TESTS)

        // configureByModule() does not always add the module output in this setup, so add it explicitly.
        CompilerModuleExtension.getInstance(module)?.let { extension ->
            extension.compilerOutputPath?.let { params.classPath.add(it) }
            extension.compilerOutputPathForTests?.let { params.classPath.add(it) }
        }
        // Fallback for Gradle projects that build outside IntelliJ's own output folders.
        project.basePath?.let { base ->
            GRADLE_OUTPUT_DIRS.map { File(base, it) }.filter { it.isDirectory }.forEach { params.classPath.add(it) }
        }

        // The runner class, and the Kotlin stdlib for Kotlin solutions in projects that do not declare it.
        params.classPath.add(PathUtil.getJarPathForClass(RemoteTestRunner::class.java))
        params.classPath.add(PathUtil.getJarPathForClass(KotlinVersion::class.java))

        val language = PluginSettings.Language.fromFilePath(problemData.filePath)
        val dir = writeTests(problemData)
        params.mainClass = RemoteTestRunner::class.java.name
        params.programParametersList.add(KSolutionGenerator.solutionClassName(problemData.problem.generateId(), language))
        params.programParametersList.add(dir.toString())
        params.programParametersList.add(problemData.problem.timeLimit.toString())
        if (environment.executor.id == DefaultDebugExecutor.EXECUTOR_ID) {
            // A test may wait on a breakpoint for minutes, so it must not be stopped as too slow.
            params.programParametersList.add(RemoteTestRunner.NO_HARD_LIMIT)
        }
        return params
    }

    /** Writes each active test as N.in and N.out, where N is the test number shown in the tool window. */
    private fun writeTests(problemData: ProblemManagerService.ProblemData): Path {
        val dir = Files.createTempDirectory("khelper_tests_")
        testsDir = dir
        problemData.testCases.forEachIndexed { index, testCase ->
            if (!testCase.active) return@forEachIndexed
            val number = index + 1
            Files.writeString(dir.resolve("$number.in"), testCase.input)
            Files.writeString(dir.resolve("$number.out"), testCase.output)
        }
        return dir
    }

    override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
        val result = super.execute(executor, runner)
        result.processHandler?.addProcessListener(object : ProcessListener {
            override fun processTerminated(event: ProcessEvent) {
                testsDir?.let { FileUtil.delete(it.toFile()) }
                // Main is generated after every run, whether the tests passed or not.
                ApplicationManager.getApplication().invokeLater {
                    MainGenerator.generate(environment.project, configuration.solutionFilePath)
                }
            }
        })
        return result
    }

    private companion object {
        val GRADLE_OUTPUT_DIRS = listOf(
            "build/classes/kotlin/main",
            "build/classes/java/main",
            "build/classes/kotlin/test",
            "out/production/classes",
            "out/production/resources",
        )
    }
}
