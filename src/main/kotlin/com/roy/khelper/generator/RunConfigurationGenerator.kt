package com.roy.khelper.generator

import com.intellij.compiler.options.CompileStepBeforeRun
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import com.roy.khelper.model.Problem
import com.roy.khelper.runner.KSolutionConfigurationType
import com.roy.khelper.runner.KSolutionRunConfiguration
import com.roy.khelper.settings.PluginSettings.Language

/** Creates or updates the run configuration of a problem and selects it. */
object RunConfigurationGenerator {

    fun createRunConfiguration(project: Project, problem: Problem, filePath: String, language: Language) {
        val runManager = RunManager.getInstance(project)
        val type = KSolutionConfigurationType.getInstance()
        val name = "[${language.displayName}] Test: ${problem.name}"

        val settings = runManager.findConfigurationByTypeAndName(type, name)
            ?: runManager.createConfiguration(name, type.factory).also { runManager.addConfiguration(it) }

        val config = settings.configuration as KSolutionRunConfiguration
        config.problemId = problem.generateId()
        config.solutionFilePath = filePath
        config.language = language.name
        ensureBuildBeforeRun(project, config)

        runManager.selectedConfiguration = settings
    }

    /** Adds the "Build" step so the solution is compiled before each run. */
    fun ensureBuildBeforeRun(project: Project, config: RunConfiguration) {
        if (config.beforeRunTasks.any { it.providerId == CompileStepBeforeRun.ID }) return
        val task = BeforeRunTaskProvider.getProvider(project, CompileStepBeforeRun.ID)?.createTask(config) ?: return
        task.isEnabled = true
        config.beforeRunTasks = config.beforeRunTasks + task
    }
}
