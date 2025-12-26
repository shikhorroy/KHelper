package com.roy.khelper.services

import com.intellij.util.messages.Topic
import com.roy.khelper.model.Problem

interface ProblemNotifier {
    companion object {
        val TOPIC = Topic.create("Problem Notifier", ProblemNotifier::class.java)
    }

    fun problemAdded(problem: Problem)
    fun testCaseUpdated(problemId: String)
    fun problemDeleted(problemId: String)
}
