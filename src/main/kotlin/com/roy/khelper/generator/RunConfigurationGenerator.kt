package com.roy.khelper.generator

import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.RunManagerEx
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.openapi.project.Project
import com.roy.khelper.model.Problem
import com.roy.khelper.runner.KSolutionConfigurationType
import com.roy.khelper.runner.KSolutionRunConfiguration

object RunConfigurationGenerator {

    fun createRunConfiguration(
            project: Project,
            problem: Problem,
            filePath: String,
            language: String
    ) {
        val runManager = RunManagerEx.getInstanceEx(project)
        val configurationType =
                ConfigurationTypeUtil.findConfigurationType(KSolutionConfigurationType::class.java)

        val factory = configurationType.configurationFactories[0]
        val languagePrefix =
                if (language == com.roy.khelper.settings.PluginSettings.Language.KOTLIN.name)
                        "[Kotlin]"
                else "[Java]"
        val configurationName = "$languagePrefix Test: ${problem.name}"

        // Check if configuration already exists
        val existingConfig =
                runManager.allSettings.find {
                    it.name == configurationName && it.type.id == configurationType.id
                }

        if (existingConfig != null) {
            // Update existing configuration
            val runConfig = existingConfig.configuration as KSolutionRunConfiguration
            runConfig.problemId = problem.generateId()
            runConfig.solutionFilePath = filePath
            runConfig.language = language
            runManager.selectedConfiguration = existingConfig
        } else {
            // Create new configuration
            val settings = runManager.createConfiguration(configurationName, factory)
            val runConfig = settings.configuration as KSolutionRunConfiguration
            runConfig.problemId = problem.generateId()
            runConfig.solutionFilePath = filePath
            runConfig.language = language

            // Add "Build" before launch task
            // Find the "Make" provider dynamically to avoid unresolved reference to
            // CompileStepBeforeRun
            val makeProvider =
                    BeforeRunTaskProvider.EP_NAME.getExtensions(project).find {
                        it.id.toString() == "Make"
                    }

            if (makeProvider != null) {
                val task = makeProvider.createTask(runConfig)
                if (task != null) {
                    task.isEnabled = true
                    runManager.setBeforeRunTasks(runConfig, listOf(task))
                }
            }

            runManager.addConfiguration(settings)
            runManager.selectedConfiguration = settings
        }
    }
}
