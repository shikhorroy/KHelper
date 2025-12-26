package com.roy.khelper.runner

import com.intellij.debugger.impl.GenericDebuggerRunner
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor

class KSolutionProgramRunner : GenericDebuggerRunner() {
    override fun getRunnerId(): String = "KSolutionProgramRunner"

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        return profile is KSolutionRunConfiguration &&
                (executorId == DefaultRunExecutor.EXECUTOR_ID ||
                        executorId == DefaultDebugExecutor.EXECUTOR_ID)
    }

    override fun doExecute(
        state: com.intellij.execution.configurations.RunProfileState,
        environment: com.intellij.execution.runners.ExecutionEnvironment
    ): com.intellij.execution.ui.RunContentDescriptor? {
        if (environment.executor.id == DefaultRunExecutor.EXECUTOR_ID) {
            val executionResult =
                state.execute(environment.executor, this) ?: return null
            return com.intellij.execution.runners.RunContentBuilder(
                executionResult,
                environment
            )
                .showRunContent(environment.contentToReuse)
        }
        return super.doExecute(state, environment)
    }
}
