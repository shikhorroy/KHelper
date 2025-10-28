package ktorapp.services

import ktorapp.constant.OUTPUT_FOLDER
import ktorapp.constant.SAMPLE_EXPECTED_OUTPUT_FOLDER
import ktorapp.constant.SAMPLE_INPUT_FOLDER
import kotlinx.serialization.Serializable
import ktorapp.models.Test
import java.nio.file.Path

@Serializable
data class TestCaseRequest(
    val input: String,
    val output: String
)

@Serializable
data class TestCaseUpdateRequest(
    val testNumber: Int,
    val input: String,
    val output: String
)

@Serializable
data class TestCaseDeleteRequest(
    val testNumber: Int
)

@Serializable
data class TestCasesResponse(
    val success: Boolean,
    val message: String,
    val totalTests: Int
)

class TestCaseService(
    private val fileService: FileService
) {
    private val base = Path.of(System.getProperty("user.dir"))
    private val inputDir = base.resolve(SAMPLE_INPUT_FOLDER)
    private val outputDir = base.resolve(SAMPLE_EXPECTED_OUTPUT_FOLDER)
    private val actualOutputDir = base.resolve(OUTPUT_FOLDER)

    /**
     * Get all existing test cases
     */
    fun getAllTestCases(): List<Test> {
        if (!inputDir.toFile().exists() || !outputDir.toFile().exists()) {
            return emptyList()
        }

        val inputFiles = inputDir.toFile().listFiles()
            ?.filter { it.extension == "txt" }
            ?.sortedBy { it.nameWithoutExtension.toIntOrNull() ?: 0 }
            ?: return emptyList()

        return inputFiles.mapNotNull { inputFile ->
            val testNumber = inputFile.nameWithoutExtension
            val outputFile = outputDir.resolve("$testNumber.txt").toFile()

            if (outputFile.exists()) {
                Test(
                    input = inputFile.readText(),
                    output = outputFile.readText()
                )
            } else {
                null
            }
        }
    }

    /**
     * Add a new test case
     */
    suspend fun addTestCase(input: String, output: String): TestCasesResponse {
        try {
            // Ensure directories exist
            inputDir.toFile().mkdirs()
            outputDir.toFile().mkdirs()

            // Find next test number
            val existingTests = getAllTestCases()
            val nextNumber = existingTests.size + 1

            // Write files
            val inputFile = inputDir.resolve("$nextNumber.txt")
            val outputFile = outputDir.resolve("$nextNumber.txt")

            fileService.writeFile(inputFile, input)
            fileService.writeFile(outputFile, output)

            println("✅ Added new test case #$nextNumber")

            return TestCasesResponse(
                success = true,
                message = "Test case #$nextNumber added successfully",
                totalTests = nextNumber
            )
        } catch (e: Exception) {
            println("❌ Failed to add test case: ${e.message}")
            return TestCasesResponse(
                success = false,
                message = "Failed to add test case: ${e.message}",
                totalTests = getAllTestCases().size
            )
        }
    }

    /**
     * Update an existing test case
     */
    suspend fun updateTestCase(testNumber: Int, input: String, output: String): TestCasesResponse {
        try {
            if (testNumber < 1) {
                return TestCasesResponse(
                    success = false,
                    message = "Invalid test number: $testNumber",
                    totalTests = getAllTestCases().size
                )
            }

            val inputFile = inputDir.resolve("$testNumber.txt")
            val outputFile = outputDir.resolve("$testNumber.txt")

            if (!inputFile.toFile().exists() || !outputFile.toFile().exists()) {
                return TestCasesResponse(
                    success = false,
                    message = "Test case #$testNumber not found",
                    totalTests = getAllTestCases().size
                )
            }

            // Update files
            fileService.writeFile(inputFile, input)
            fileService.writeFile(outputFile, output)

            // Clear actual output if exists (force rerun)
            val actualFile = actualOutputDir.resolve("$testNumber.txt")
            if (actualFile.toFile().exists()) {
                actualFile.toFile().delete()
                println("🗑️  Cleared actual output for test #$testNumber (will need rerun)")
            }

            println("✅ Updated test case #$testNumber")

            return TestCasesResponse(
                success = true,
                message = "Test case #$testNumber updated successfully",
                totalTests = getAllTestCases().size
            )
        } catch (e: Exception) {
            println("❌ Failed to update test case: ${e.message}")
            return TestCasesResponse(
                success = false,
                message = "Failed to update test case: ${e.message}",
                totalTests = getAllTestCases().size
            )
        }
    }

    /**
     * Delete a test case
     */
    fun deleteTestCase(testNumber: Int): TestCasesResponse {
        try {
            if (testNumber < 1) {
                return TestCasesResponse(
                    success = false,
                    message = "Invalid test number: $testNumber",
                    totalTests = getAllTestCases().size
                )
            }

            val inputFile = inputDir.resolve("$testNumber.txt").toFile()
            val outputFile = outputDir.resolve("$testNumber.txt").toFile()

            if (!inputFile.exists() || !outputFile.exists()) {
                return TestCasesResponse(
                    success = false,
                    message = "Test case #$testNumber not found",
                    totalTests = getAllTestCases().size
                )
            }

            // Delete files
            inputFile.delete()
            outputFile.delete()

            // Delete actual output if exists
            val actualFile = actualOutputDir.resolve("$testNumber.txt").toFile()
            if (actualFile.exists()) {
                actualFile.delete()
            }

            // Renumber remaining tests
            renumberTests(testNumber)

            println("✅ Deleted test case #$testNumber")

            val newTotal = getAllTestCases().size

            return TestCasesResponse(
                success = true,
                message = "Test case #$testNumber deleted successfully",
                totalTests = newTotal
            )
        } catch (e: Exception) {
            println("❌ Failed to delete test case: ${e.message}")
            return TestCasesResponse(
                success = false,
                message = "Failed to delete test case: ${e.message}",
                totalTests = getAllTestCases().size
            )
        }
    }

    /**
     * Renumber test files after deletion
     */
    private fun renumberTests(deletedNumber: Int) {
        val allFiles = inputDir.toFile().listFiles()
            ?.filter { it.extension == "txt" }
            ?.sortedBy { it.nameWithoutExtension.toIntOrNull() ?: 0 }
            ?: return

        allFiles.forEach { inputFile ->
            val currentNumber = inputFile.nameWithoutExtension.toIntOrNull() ?: return@forEach

            if (currentNumber > deletedNumber) {
                val newNumber = currentNumber - 1

                // Rename input file
                val newInputFile = inputDir.resolve("$newNumber.txt").toFile()
                inputFile.renameTo(newInputFile)

                // Rename output file
                val oldOutputFile = outputDir.resolve("$currentNumber.txt").toFile()
                val newOutputFile = outputDir.resolve("$newNumber.txt").toFile()
                if (oldOutputFile.exists()) {
                    oldOutputFile.renameTo(newOutputFile)
                }

                // Rename actual output file if exists
                val oldActualFile = actualOutputDir.resolve("$currentNumber.txt").toFile()
                val newActualFile = actualOutputDir.resolve("$newNumber.txt").toFile()
                if (oldActualFile.exists()) {
                    oldActualFile.renameTo(newActualFile)
                }
            }
        }
    }
}

