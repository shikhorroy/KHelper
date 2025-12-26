package com.roy.khelper.runner

import com.intellij.execution.configurations.RunConfigurationOptions

class KSolutionRunConfigurationOptions : RunConfigurationOptions() {

    private val problemId = string("").provideDelegate(this, "problemId")
    private val solutionFilePath = string("").provideDelegate(this, "solutionFilePath")
    private val language = string("").provideDelegate(this, "language")

    fun getProblemId(): String = problemId.getValue(this) ?: ""

    fun setProblemId(value: String) {
        problemId.setValue(this, value)
    }

    fun getSolutionFilePath(): String = solutionFilePath.getValue(this) ?: ""

    fun setSolutionFilePath(value: String) {
        solutionFilePath.setValue(this, value)
    }

    fun getLanguage(): String = language.getValue(this) ?: ""

    fun setLanguage(value: String) {
        language.setValue(this, value)
    }
}
