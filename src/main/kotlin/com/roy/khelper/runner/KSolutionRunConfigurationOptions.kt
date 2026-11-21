package com.roy.khelper.runner

import com.intellij.execution.configurations.RunConfigurationOptions

/** Stored in workspace.xml. The property names are the stored option names, so never rename them. */
class KSolutionRunConfigurationOptions : RunConfigurationOptions() {
    var problemId by string("")
    var solutionFilePath by string("")
    var language by string("")
}
