package com.roy.khelper.server.listeners

import com.roy.khelper.model.Problem

interface ProblemListener {
    fun onProblemReceived(problem: Problem)
}
