package com.roy.khelper.runner

import com.intellij.execution.configurations.JavaCommandLineState
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.PathUtil
import com.roy.khelper.services.ProblemManagerService
import java.io.File

class KSolutionRunProfileState(
        environment: ExecutionEnvironment,
        private val configuration: KSolutionRunConfiguration
) : JavaCommandLineState(environment) {

    override fun createJavaParameters(): JavaParameters {
        try {
            val project = environment.project
            val problemManager = ProblemManagerService.getInstance(project)
            val problemData =
                    problemManager.getProblem(configuration.problemId, configuration.language)
                            ?: throw com.intellij.execution.ExecutionException(
                                    "Problem data not found for ID: ${configuration.problemId}, Language: ${configuration.language}"
                            )

            val params = JavaParameters()

            // 1. Setup Classpath
            if (problemData.filePath.isBlank()) {
                throw com.intellij.execution.ExecutionException(
                        "Solution file path is not set for problem: ${problemData.problem.name}"
                )
            }

            val virtualFile =
                    LocalFileSystem.getInstance().findFileByPath(problemData.filePath)
                            ?: throw com.intellij.execution.ExecutionException(
                                    "Solution file not found at path: ${problemData.filePath}"
                            )

            val module =
                    ModuleUtil.findModuleForFile(virtualFile, project)
                            ?: throw com.intellij.execution.ExecutionException(
                                    "Module not found for file: ${virtualFile.path}. Please ensure the file is within a valid module source root."
                            )

            // Use the module's runtime classpath (includes stdlib)
            params.configureByModule(module, JavaParameters.JDK_AND_CLASSES_AND_TESTS)

            // Explicitly add module output paths (Production & Test)
            // configureByModule should do this, but sometimes fails for
            // JavaCommandLineState in the plugin context
            val compilerExtension =
                    com.intellij.openapi.roots.CompilerModuleExtension.getInstance(module)
            compilerExtension?.compilerOutputPath?.let { params.classPath.add(it) }
            compilerExtension?.compilerOutputPathForTests?.let { params.classPath.add(it) }

            // Fallback: Explicitly look for Gradle output paths and IntelliJ paths
            val projectBasePath = project.basePath

            if (projectBasePath != null) {
                val potentialPaths =
                        listOf(
                                "$projectBasePath/build/classes/kotlin/main",
                                "$projectBasePath/build/classes/java/main",
                                "$projectBasePath/build/classes/kotlin/test",
                                "$projectBasePath/out/production/classes",
                                "$projectBasePath/out/production/resources"
                        )

                potentialPaths.forEach { path -> params.classPath.add(path) }
            }

            // Add Plugin JAR and Kotlin Stdlib to the classpath (so we can find
            // RemoteTestRunner and
            // run Kotlin-based classes)
            try {
                val runnerPath = PathUtil.getJarPathForClass(RemoteTestRunner::class.java)
                params.classPath.add(runnerPath)

                // Explicitly add a Kotlin standard library
                val kotlinStdlibPath =
                        PathUtil.getJarPathForClass(kotlin.jvm.internal.Intrinsics::class.java)
                params.classPath.add(kotlinStdlibPath)
            } catch (e: Exception) {
                throw com.intellij.execution.ExecutionException(
                        "Failed to locate required runner classes or Kotlin stdlib: ${e.message}"
                )
            }

            // 2. Main Class
            params.mainClass = "com.roy.khelper.runner.RemoteTestRunner"

            // 3. Program Arguments
            val problemId = problemData.problem.generateId()
            val isJava = problemData.filePath.endsWith(".java")
            val classBaseName = if (isJava) "JSolution" else "KSolution"
            val className = "problems.$problemId.$classBaseName"
            params.programParametersList.add(className)

            val testFile = createTestFile(problemData)
            params.programParametersList.add(testFile.absolutePath)

            return params
        } catch (e: com.intellij.execution.ExecutionException) {
            throw e
        } catch (e: Exception) {
            throw com.intellij.execution.ExecutionException(
                    "Failed to configure run parameters: ${e.message}",
                    e
            )
        }
    }

    private fun createTestFile(problemData: ProblemManagerService.ProblemData): File {
        val tempFile = File.createTempFile("khelper_tests_", ".txt")
        tempFile.deleteOnExit()

        val sb = StringBuilder()
        val activeTests = problemData.testCases.filter { it.active }

        activeTests.forEach { testCase ->
            sb.append(testCase.input)
            sb.append("---SEPARATOR---")
            sb.append(testCase.output)
            sb.append("---TESTCASE---") // Delimiter between cases
        }

        tempFile.writeText(sb.toString())
        return tempFile
    }

    override fun execute(
            executor: com.intellij.execution.Executor,
            runner: com.intellij.execution.runners.ProgramRunner<*>
    ): com.intellij.execution.ExecutionResult {
        val result = super.execute(executor, runner)

        if (com.roy.khelper.settings.PluginSettings.getInstance().state.autoGenerateMain) {
            result.processHandler?.addProcessListener(
                    object : com.intellij.execution.process.ProcessListener {
                        override fun startNotified(
                                event: com.intellij.execution.process.ProcessEvent
                        ) {}

                        override fun processTerminated(
                                event: com.intellij.execution.process.ProcessEvent
                        ) {
                            if (event.exitCode == 0) {
                                com.intellij.openapi.application.ApplicationManager.getApplication()
                                        .invokeLater {
                                            try {
                                                com.roy.khelper.generator.MainGenerator.generate(
                                                        environment.project,
                                                        configuration.solutionFilePath
                                                )
                                            } catch (e: Exception) {
                                                com.intellij.notification.NotificationGroupManager
                                                        .getInstance()
                                                        .getNotificationGroup(
                                                                "KHelper Notifications"
                                                        )
                                                        .createNotification(
                                                                "KHelper",
                                                                "Failed to generate Main.kt: ${e.message}",
                                                                com.intellij.notification
                                                                        .NotificationType.ERROR
                                                        )
                                                        .notify(environment.project)
                                            }
                                        }
                            }
                        }

                        override fun processWillTerminate(
                                event: com.intellij.execution.process.ProcessEvent,
                                willBeDestroyed: Boolean
                        ) {}

                        override fun onTextAvailable(
                                event: com.intellij.execution.process.ProcessEvent,
                                outputType: com.intellij.openapi.util.Key<*>
                        ) {}
                    }
            )
        }
        return result
    }
}
