package com.roy.khelper.runner

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import javax.swing.Icon

class KSolutionConfigurationType : ConfigurationType {

    override fun getDisplayName(): String = "KHelper Solution"

    override fun getConfigurationTypeDescription(): String =
        "Run configuration for testing competitive programming solutions"

    override fun getIcon(): Icon = com.roy.khelper.icons.KHelperIcons.PluginIcon

    override fun getId(): String = "KHELPER_PROBLEM_TEST"

    override fun getConfigurationFactories(): Array<ConfigurationFactory> =
        arrayOf(KSolutionConfigurationFactory(this))
}
