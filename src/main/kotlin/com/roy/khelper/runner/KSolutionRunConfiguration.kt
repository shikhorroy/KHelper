package com.roy.khelper.runner

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ModuleRunProfile
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.roy.khelper.services.ProblemManagerService

class KSolutionRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    RunConfigurationBase<KSolutionRunConfigurationOptions>(project, factory, name),
    ModuleRunProfile {

    override fun getOptions(): KSolutionRunConfigurationOptions = super.getOptions() as KSolutionRunConfigurationOptions

    var problemId: String
        get() = options.problemId.orEmpty()
        set(value) {
            options.problemId = value
        }

    var solutionFilePath: String
        get() = options.solutionFilePath.orEmpty()
        set(value) {
            options.solutionFilePath = value
        }

    var language: String
        get() = options.language.orEmpty()
        set(value) {
            options.language = value
        }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
        KSolutionRunProfileState(environment, this)

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> = KSolutionSettingsEditor()

    override fun checkConfiguration() {
        if (problemId.isBlank()) throw RuntimeConfigurationError("Problem ID is not set")
        if (ProblemManagerService.getInstance(project).getProblem(problemId, language) == null) {
            throw RuntimeConfigurationError("KHelper has no saved problem '$problemId' ($language)")
        }
    }

    /** Tells the build system which module to compile before the run. */
    override fun getModules(): Array<Module> {
        if (solutionFilePath.isEmpty()) return Module.EMPTY_ARRAY
        val file = LocalFileSystem.getInstance().findFileByPath(solutionFilePath) ?: return Module.EMPTY_ARRAY
        val module = ModuleUtilCore.findModuleForFile(file, project) ?: return Module.EMPTY_ARRAY
        return arrayOf(module)
    }
}
