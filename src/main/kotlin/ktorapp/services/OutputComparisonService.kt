package ktorapp.services

import ktorapp.constant.OUTPUT_FOLDER
import ktorapp.constant.SAMPLE_EXPECTED_OUTPUT_FOLDER
import kotlinx.serialization.Serializable
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

@Serializable
data class LineDifference(
    val lineNumber: Int,
    val expected: String,
    val actual: String
)

@Serializable
data class FileComparison(
    val fileName: String,
    val matched: Boolean,
    val differences: List<LineDifference>,
    val expectedExists: Boolean,
    val actualExists: Boolean,
    val message: String
)

@Serializable
data class ComparisonResult(
    val totalFiles: Int,
    val matchedFiles: Int,
    val comparisons: List<FileComparison>
)

class OutputComparisonService(
    private val matchedTestTracker: MatchedTestTracker
) {

    fun compareOutputs(): ComparisonResult {
        val base = Path.of(System.getProperty("user.dir"))
        val expectedDir = base.resolve(SAMPLE_EXPECTED_OUTPUT_FOLDER)
        val actualDir = base.resolve(OUTPUT_FOLDER)

        if (!expectedDir.exists()) {
            return ComparisonResult(
                totalFiles = 0,
                matchedFiles = 0,
                comparisons = listOf(
                    FileComparison(
                        fileName = "N/A",
                        matched = false,
                        differences = emptyList(),
                        expectedExists = false,
                        actualExists = false,
                        message = "Expected output folder not found: $SAMPLE_EXPECTED_OUTPUT_FOLDER"
                    )
                )
            )
        }

        if (!actualDir.exists()) {
            return ComparisonResult(
                totalFiles = 0,
                matchedFiles = 0,
                comparisons = listOf(
                    FileComparison(
                        fileName = "N/A",
                        matched = false,
                        differences = emptyList(),
                        expectedExists = true,
                        actualExists = false,
                        message = "Actual output folder not found: $OUTPUT_FOLDER"
                    )
                )
            )
        }

        val expectedFiles: List<Path> = Files.list(expectedDir)
            .filter { it.fileName.toString().endsWith(".txt") }
            .sorted()
            .toList()

        if (expectedFiles.isEmpty()) {
            return ComparisonResult(
                totalFiles = 0,
                matchedFiles = 0,
                comparisons = listOf(
                    FileComparison(
                        fileName = "N/A",
                        matched = false,
                        differences = emptyList(),
                        expectedExists = true,
                        actualExists = true,
                        message = "No test files found in $SAMPLE_EXPECTED_OUTPUT_FOLDER"
                    )
                )
            )
        }

        val comparisons = expectedFiles.map { expectedFile ->
            val fileName = expectedFile.fileName.toString()
            val actualFile = actualDir.resolve(fileName)

            compareFiles(fileName, expectedFile, actualFile)
        }

        val matchedCount = comparisons.count { it.matched }

        // Mark matched tests for future batch runs
        val matchedFileNames = comparisons.filter { it.matched }.map { it.fileName }
        if (matchedFileNames.isNotEmpty()) {
            matchedTestTracker.markAsMatched(matchedFileNames)
        }

        return ComparisonResult(
            totalFiles = comparisons.size,
            matchedFiles = matchedCount,
            comparisons = comparisons
        )
    }

    private fun compareFiles(fileName: String, expectedPath: Path, actualPath: Path): FileComparison {
        if (!actualPath.exists()) {
            return FileComparison(
                fileName = fileName,
                matched = false,
                differences = emptyList(),
                expectedExists = true,
                actualExists = false,
                message = "Actual output file not found"
            )
        }

        val expectedLines = expectedPath.readText().lines()
        val actualLines = actualPath.readText().lines()

        val differences = mutableListOf<LineDifference>()
        val maxLines = maxOf(expectedLines.size, actualLines.size)

        for (i in 0 until maxLines) {
            val expectedLine = expectedLines.getOrNull(i) ?: ""
            val actualLine = actualLines.getOrNull(i) ?: ""

            if (expectedLine != actualLine) {
                differences.add(
                    LineDifference(
                        lineNumber = i + 1,
                        expected = expectedLine,
                        actual = actualLine
                    )
                )
            }
        }

        return FileComparison(
            fileName = fileName,
            matched = differences.isEmpty(),
            differences = differences,
            expectedExists = true,
            actualExists = true,
            message = if (differences.isEmpty()) "All lines match!" else "${differences.size} line(s) differ"
        )
    }
}

