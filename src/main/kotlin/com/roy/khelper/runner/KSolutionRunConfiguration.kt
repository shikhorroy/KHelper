package com.roy.khelper.runner

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project

class KSolutionRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    RunConfigurationBase<KSolutionRunConfigurationOptions>(project, factory, name),
    ModuleRunProfile {

    override fun getOptions(): KSolutionRunConfigurationOptions {
        return super.getOptions() as KSolutionRunConfigurationOptions
    }

    var problemId: String
        get() = options.getProblemId()
        set(value) = options.setProblemId(value)

    var solutionFilePath: String
        get() = options.getSolutionFilePath()
        set(value) = options.setSolutionFilePath(value)

    var language: String
        get() = options.getLanguage()
        set(value) = options.setLanguage(value)

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return KSolutionRunProfileState(environment, this)
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return KSolutionSettingsEditor()
    }

    // Crucial: Tell the build system which module to compile
    override fun getModules(): Array<com.intellij.openapi.module.Module> {
        if (solutionFilePath.isEmpty()) return emptyArray()

        val virtualFile =
            com.intellij.openapi.vfs.LocalFileSystem.getInstance()
                .findFileByPath(solutionFilePath)
        if (virtualFile != null) {
            val module =
                com.intellij.openapi.module.ModuleUtil.findModuleForFile(virtualFile, project)
            if (module != null) {
                return arrayOf(module)
            }
        }
        return emptyArray()
    }
}
