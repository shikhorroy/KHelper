package com.roy.khelper.services

import com.intellij.util.messages.Topic
import com.roy.khelper.model.Problem

/** Tells the UI that problems or test cases changed. Published by [ProblemManagerService]. */
interface ProblemNotifier {
    fun problemAdded(problem: Problem)
    fun testCaseUpdated(problemId: String)
    fun problemDeleted(problemId: String)

    companion object {
        @JvmField
        @Topic.ProjectLevel
        val TOPIC: Topic<ProblemNotifier> = Topic.create("KHelper Problem Notifier", ProblemNotifier::class.java)
    }
}
