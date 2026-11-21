package com.roy.khelper.runner

import com.intellij.debugger.impl.GenericDebuggerRunner
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.RunContentBuilder
import com.intellij.execution.ui.RunContentDescriptor

/** Runs and debugs KHelper run configurations. */
class KSolutionProgramRunner : GenericDebuggerRunner() {
    override fun getRunnerId(): String = "KSolutionProgramRunner"

    override fun canRun(executorId: String, profile: RunProfile): Boolean =
        profile is KSolutionRunConfiguration &&
                (executorId == DefaultRunExecutor.EXECUTOR_ID || executorId == DefaultDebugExecutor.EXECUTOR_ID)

    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        if (environment.executor.id != DefaultRunExecutor.EXECUTOR_ID) {
            return super.doExecute(state, environment)
        }
        val result = state.execute(environment.executor, this) ?: return null
        return RunContentBuilder(result, environment).showRunContent(environment.contentToReuse)
    }
}
