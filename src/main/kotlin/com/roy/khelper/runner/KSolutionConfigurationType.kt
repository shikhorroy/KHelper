package com.roy.khelper.runner

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.roy.khelper.icons.KHelperIcons
import javax.swing.Icon

class KSolutionConfigurationType : ConfigurationType {

    //~ Created once. The platform expects the same factory instance on every call.
    val factory: ConfigurationFactory = KSolutionConfigurationFactory(this)

    override fun getDisplayName(): String = "KHelper Solution"

    override fun getConfigurationTypeDescription(): String =
        "Run configuration for testing competitive programming solutions"

    override fun getIcon(): Icon = KHelperIcons.RunConfiguration

    //~ Stored in workspace.xml. Never change it, or existing run configurations are lost.
    override fun getId(): String = "KHELPER_PROBLEM_TEST"

    override fun getConfigurationFactories(): Array<ConfigurationFactory> = arrayOf(factory)

    companion object {
        fun getInstance(): KSolutionConfigurationType =
            ConfigurationTypeUtil.findConfigurationType(KSolutionConfigurationType::class.java)
    }
}
