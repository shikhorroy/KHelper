package com.roy.khelper.runner

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.project.Project

class KSolutionConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {

    override fun getId(): String = "KHELPER_PROBLEM_TEST_FACTORY"

    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        val config = KSolutionRunConfiguration(project, this, "Solution")

        // Add "Build" task to Before Launch list
        val compileProvider =
            com.intellij.execution.BeforeRunTaskProvider.getProvider(
                project,
                com.intellij.compiler.options.CompileStepBeforeRun.ID
            )
        if (compileProvider != null) {
            val compileTask = compileProvider.createTask(config)
            if (compileTask != null) {
                config.beforeRunTasks = listOf(compileTask)
            }
        }

        return config
    }

    override fun getOptionsClass(): Class<out BaseState> {
        return KSolutionRunConfigurationOptions::class.java
    }
}
