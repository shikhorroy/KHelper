package com.roy.khelper.runner

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.project.Project
import com.roy.khelper.generator.RunConfigurationGenerator

class KSolutionConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {

    //~ Stored in workspace.xml. Never change it.
    override fun getId(): String = "KHELPER_PROBLEM_TEST_FACTORY"

    override fun createTemplateConfiguration(project: Project): RunConfiguration =
        KSolutionRunConfiguration(project, this, "Solution").also {
            RunConfigurationGenerator.ensureBuildBeforeRun(project, it)
        }

    override fun getOptionsClass(): Class<out BaseState> = KSolutionRunConfigurationOptions::class.java
}
