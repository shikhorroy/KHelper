package ktorapp.services

import ktorapp.constant.OUTPUT_FOLDER
import ktorapp.constant.SAMPLE_EXPECTED_OUTPUT_FOLDER
import ktorapp.constant.SAMPLE_INPUT_FOLDER
import ktorapp.models.Problem
import java.nio.file.Path

class ProblemService(
    private val fileService: FileService,
    private val matchedTestTracker: MatchedTestTracker
) {
    suspend fun processProblem(problem: Problem) {
        val base = Path.of(System.getProperty("user.dir"))
        val inputDir = base.resolve(SAMPLE_INPUT_FOLDER)
        val outputDir = base.resolve(SAMPLE_EXPECTED_OUTPUT_FOLDER)
        val actualOutputDir = base.resolve(OUTPUT_FOLDER)

        println("\n🗂️  Preparing directories...")
        fileService.clearDirectory(inputDir)
        fileService.clearDirectory(outputDir)
        fileService.clearDirectory(actualOutputDir)
        println("   ✓ Cleared $SAMPLE_INPUT_FOLDER")
        println("   ✓ Cleared $SAMPLE_EXPECTED_OUTPUT_FOLDER")
        println("   ✓ Cleared $OUTPUT_FOLDER")

        // Clear matched tests tracking for new problem
        matchedTestTracker.clearAll(silent = true)
        println("   ✓ Cleared previous test tracking")

        println("\n📄 Writing test files...")
        problem.tests.forEachIndexed { idx, test ->
            val i = idx + 1
            fileService.writeFile(inputDir.resolve("$i.txt"), test.input)
            fileService.writeFile(outputDir.resolve("$i.txt"), test.output)
            println("   ✓ Test $i: ${test.input.lines().size} lines input, ${test.output.lines().size} lines output")
        }

        println("\n📋 Saving metadata to $PROBLEM_META_TXT")
        val metadata = buildMetadata(problem)
        fileService.writeFile(base.resolve(PROBLEM_META_TXT), metadata)

        println("\n✅ Problem data saved successfully!")
    }

    private fun buildMetadata(problem: Problem): String {
        return buildString {
            appendLine(problem.name ?: "")
            appendLine(problem.group ?: "")
            appendLine(problem.url ?: "")
            appendLine("TL(ms)=${problem.timeLimit ?: -1}, ML(MB)=${problem.memoryLimit ?: -1}")
        }
    }

    fun logProblemReceived(problem: Problem) {
        println("\n───────────────────────────────────────────────────────────")
        println("📥 Received problem data")
        println("📝 Problem: ${problem.name ?: "Unknown"}")
        println("🏷️  Group: ${problem.group ?: "Unknown"}")
        println("🔗 URL: ${problem.url ?: "Unknown"}")
        println("⏱️  Time Limit: ${problem.timeLimit ?: -1} ms")
        println("💾 Memory Limit: ${problem.memoryLimit ?: -1} MB")
        println("🧪 Test Cases: ${problem.tests.size}")
    }

    companion object {
        const val PROBLEM_META_TXT = ".problem-meta.txt"
    }
}

