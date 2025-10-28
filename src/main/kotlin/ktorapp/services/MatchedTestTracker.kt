package ktorapp.services

import java.io.File

/**
 * Service to track and persist matched test cases to skip them in subsequent batch runs.
 */
class MatchedTestTracker {
    private val matchedFile = File(MATCHED_TESTS_FILE)
    private val matchedTests = mutableSetOf<String>()

    init {
        loadMatchedTests()
    }

    /**
     * Load previously matched test cases from file
     */
    private fun loadMatchedTests() {
        if (matchedFile.exists()) {
            try {
                matchedTests.addAll(matchedFile.readLines().filter { it.isNotBlank() })
                if (matchedTests.isNotEmpty()) {
                    println(
                        "📌 Loaded ${matchedTests.size} previously matched test(s): ${
                            matchedTests.sorted().joinToString(", ")
                        }"
                    )
                }
            } catch (e: Exception) {
                System.err.println("⚠️  Failed to load matched tests: ${e.message}")
            }
        }
    }

    /**
     * Save matched tests to file
     */
    private fun saveMatchedTests() {
        try {
            matchedFile.parentFile?.mkdirs()
            matchedFile.writeText(matchedTests.sorted().joinToString("\n"))
        } catch (e: Exception) {
            System.err.println("⚠️  Failed to save matched tests: ${e.message}")
        }
    }

    /**
     * Check if a test case is already matched
     */
    fun isMatched(fileName: String): Boolean {
        return matchedTests.contains(fileName)
    }

    /**
     * Mark a test case as matched
     */
    fun markAsMatched(fileName: String) {
        if (matchedTests.add(fileName)) {
            saveMatchedTests()
            println("✓ Marked $fileName as matched")
        }
    }

    /**
     * Mark multiple test cases as matched
     */
    fun markAsMatched(fileNames: List<String>) {
        var changed = false
        fileNames.forEach {
            changed = true
            matchedTests.add(it)
        }
        if (changed) {
            saveMatchedTests()
            println("✓ Marked ${fileNames.size} test(s) as matched")
        }
    }

    /**
     * Unmark a test case (for rerun)
     */
    fun unmark(fileName: String) {
        if (matchedTests.remove(fileName)) {
            saveMatchedTests()
            println("✓ Unmarked $fileName")
        }
    }

    /**
     * Clear all matched tests
     */
    fun clearAll(silent: Boolean = false) {
        matchedTests.clear()
        saveMatchedTests()
        if (!silent) {
            println("✓ Cleared all matched tests")
        }
    }

    /**
     * Get all matched test file names
     */
    fun getMatchedTests(): Set<String> {
        return matchedTests.toSet()
    }

    /**
     * Get count of matched tests
     */
    fun getMatchedCount(): Int {
        return matchedTests.size
    }

    /**
     * Filter out already matched files from a list
     */
    fun filterUnmatched(files: List<File>): List<File> {
        return files.filter { !isMatched(it.name) }
    }

    companion object {
        private const val MATCHED_TESTS_FILE = ".matched-tests.txt"
    }
}

