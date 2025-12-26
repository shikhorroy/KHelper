package com.roy.khelper.model

enum class Verdict {
    ACCEPTED,
    WRONG_ANSWER,
    TIME_LIMIT_EXCEEDED,
    RUNTIME_ERROR,
    COMPILATION_ERROR
}

data class TestResult(
    val testCase: TestCase,
    val testNumber: Int,
    val verdict: Verdict,
    val actualOutput: String? = null,
    val executionTime: Long? = null,  // in milliseconds
    val errorMessage: String? = null
) {
    fun isPassed(): Boolean = verdict == Verdict.ACCEPTED

    fun getVerdictSymbol(): String = when (verdict) {
        Verdict.ACCEPTED -> "✓"
        Verdict.WRONG_ANSWER -> "✗"
        Verdict.TIME_LIMIT_EXCEEDED -> "⏱"
        Verdict.RUNTIME_ERROR -> "💥"
        Verdict.COMPILATION_ERROR -> "⚠"
    }

    fun getVerdictText(): String = when (verdict) {
        Verdict.ACCEPTED -> "Accepted"
        Verdict.WRONG_ANSWER -> "Wrong Answer"
        Verdict.TIME_LIMIT_EXCEEDED -> "Time Limit Exceeded"
        Verdict.RUNTIME_ERROR -> "Runtime Error"
        Verdict.COMPILATION_ERROR -> "Compilation Error"
    }
}

data class ProblemTestResults(
    val problemId: String,
    val results: List<TestResult>
) {
    fun passedCount(): Int = results.count { it.isPassed() }

    fun totalCount(): Int = results.size

    fun allPassed(): Boolean = results.all { it.isPassed() }

    fun getSummary(): String = "${passedCount()}/${totalCount()} tests passed"
}
