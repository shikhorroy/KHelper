package ktorapp.services

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*

class FileService(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun writeFile(path: Path, content: String) {
        withContext(dispatcher) {
            Files.createDirectories(path.parent)
            Files.writeString(
                path, content,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
            )
        }
    }

    fun clearDirectory(dir: Path) {
        if (Files.exists(dir)) {
            Files.walk(dir)
                .sorted(Comparator.reverseOrder())
                .forEach { if (it != dir) Files.deleteIfExists(it) }
        }
        Files.createDirectories(dir)
    }

    /**
     * Clear selected files and folders based on user options
     * This removes selected test cases, outputs, and tracking files
     * Also deletes the directories themselves
     */
    fun clearAll(options: ClearAllOptions = ClearAllOptions()): ClearAllResult {
        val baseDir = Path.of(System.getProperty("user.dir"))
        val deletedItems = mutableListOf<String>()

        try {
            // Delete sample/input directory and all contents
            if (options.clearTestInputs) {
                val sampleInput = baseDir.resolve("sample/input")
                if (Files.exists(sampleInput)) {
                    val count = Files.list(sampleInput).use { it.count() }
                    Files.walk(sampleInput)
                        .sorted(Comparator.reverseOrder())
                        .forEach { Files.deleteIfExists(it) }
                    if (count > 0) deletedItems.add("$count test input file(s)")
                    deletedItems.add("sample/input directory")
                }
            }

            // Delete sample/output directory and all contents
            if (options.clearTestOutputs) {
                val sampleOutput = baseDir.resolve("sample/output")
                if (Files.exists(sampleOutput)) {
                    val count = Files.list(sampleOutput).use { it.count() }
                    Files.walk(sampleOutput)
                        .sorted(Comparator.reverseOrder())
                        .forEach { Files.deleteIfExists(it) }
                    if (count > 0) deletedItems.add("$count expected output file(s)")
                    deletedItems.add("sample/output directory")
                }
            }

            // Delete sample directory if it exists and is now empty
            val sample = baseDir.resolve("sample")
            if (Files.exists(sample) && Files.list(sample).use { it.count() == 0L }) {
                Files.delete(sample)
                deletedItems.add("sample directory")
            }

            // Delete output directory and all contents
            if (options.clearActualOutputs) {
                val output = baseDir.resolve("output")
                if (Files.exists(output)) {
                    val count = Files.list(output).use { it.count() }
                    Files.walk(output)
                        .sorted(Comparator.reverseOrder())
                        .forEach { Files.deleteIfExists(it) }
                    if (count > 0) deletedItems.add("$count actual output file(s)")
                    deletedItems.add("output directory")
                }
            }

            // Delete .matched-tests.txt
            if (options.clearMatchedTests) {
                val matchedTests = baseDir.resolve(".matched-tests.txt")
                if (Files.exists(matchedTests)) {
                    Files.delete(matchedTests)
                    deletedItems.add("Matched tests tracking file")
                }
            }

            // Delete .problem-meta.txt
            if (options.clearProblemMeta) {
                val problemMeta = baseDir.resolve(".problem-meta.txt")
                if (Files.exists(problemMeta)) {
                    Files.delete(problemMeta)
                    deletedItems.add("Problem metadata file")
                }
            }

            // Delete single input/output files
            if (options.clearSingleFiles) {
                val singleInput = baseDir.resolve("single-input.txt")
                if (Files.exists(singleInput)) {
                    Files.delete(singleInput)
                    deletedItems.add("Single input file")
                }

                val outputTxt = baseDir.resolve("output.txt")
                if (Files.exists(outputTxt)) {
                    Files.delete(outputTxt)
                    deletedItems.add("Output file")
                }
            }

            println("✅ Successfully cleared all files to initial state")
            println("   Deleted: ${deletedItems.joinToString(", ")}")

            return ClearAllResult(
                success = true,
                message = "Successfully cleared all files to initial state",
                deletedItems = deletedItems
            )

        } catch (e: Exception) {
            println("❌ Error clearing files: ${e.message}")
            e.printStackTrace()
            return ClearAllResult(
                success = false,
                message = "Error clearing files: ${e.message}",
                deletedItems = deletedItems
            )
        }
    }
}

@Serializable
data class ClearAllOptions(
    val clearTestInputs: Boolean = true,
    val clearTestOutputs: Boolean = true,
    val clearActualOutputs: Boolean = true,
    val clearMatchedTests: Boolean = true,
    val clearProblemMeta: Boolean = true,
    val clearSingleFiles: Boolean = true
)

@Serializable
data class ClearAllResult(
    val success: Boolean,
    val message: String,
    val deletedItems: List<String>
)

