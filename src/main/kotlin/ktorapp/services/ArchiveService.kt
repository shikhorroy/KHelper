package ktorapp.services

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ktorapp.constant.OUTPUT_FOLDER
import ktorapp.constant.SAMPLE_EXPECTED_OUTPUT_FOLDER
import ktorapp.constant.SAMPLE_INPUT_FOLDER
import ktorapp.models.Problem
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Serializable
data class ArchivedProblem(
    val id: String,
    val name: String,
    val group: String,
    val url: String,
    val archivedAt: String,
    val testCount: Int,
    val problem: Problem
)

@Serializable
data class ArchiveListResponse(
    val archives: List<ArchivedProblem>,
    val count: Int
)

@Serializable
data class ArchiveResponse(
    val success: Boolean,
    val message: String,
    val archiveId: String? = null
)

class ArchiveService(
    private val fileService: FileService
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val baseDir = Path.of(System.getProperty("user.dir"))
    private val archiveDir = baseDir.resolve("archives")

    init {
        Files.createDirectories(archiveDir)
    }

    /**
     * Archive the current problem with its test cases and Solver.kt
     */
    suspend fun archiveProblem(problem: Problem, overwrite: Boolean = false): ArchiveResponse {
        val problemName = problem.name ?: "Unknown Problem"
        val archiveId = generateArchiveId(problemName)
        val archivePath = archiveDir.resolve(archiveId)

        // Check if archive already exists
        if (Files.exists(archivePath) && !overwrite) {
            return ArchiveResponse(
                success = false,
                message = "Archive already exists: $problemName",
                archiveId = archiveId
            )
        }

        try {
            // Create archive directory
            Files.createDirectories(archivePath)

            // Create subdirectories
            val inputDir = archivePath.resolve("input")
            val outputDir = archivePath.resolve("output")
            Files.createDirectories(inputDir)
            Files.createDirectories(outputDir)

            // Copy test cases
            val sampleInputDir = baseDir.resolve(SAMPLE_INPUT_FOLDER)
            val sampleOutputDir = baseDir.resolve(SAMPLE_EXPECTED_OUTPUT_FOLDER)

            if (Files.exists(sampleInputDir)) {
                Files.list(sampleInputDir).use { files ->
                    files.forEach { file ->
                        Files.copy(file, inputDir.resolve(file.fileName), StandardCopyOption.REPLACE_EXISTING)
                    }
                }
            }

            if (Files.exists(sampleOutputDir)) {
                Files.list(sampleOutputDir).use { files ->
                    files.forEach { file ->
                        Files.copy(file, outputDir.resolve(file.fileName), StandardCopyOption.REPLACE_EXISTING)
                    }
                }
            }

            // Copy Solver.kt
            val solverFile = baseDir.resolve("src/main/kotlin/Solver.kt")
            if (Files.exists(solverFile)) {
                Files.copy(solverFile, archivePath.resolve("Solver.kt"), StandardCopyOption.REPLACE_EXISTING)
            }

            // Save problem metadata
            val archivedProblem = ArchivedProblem(
                id = archiveId,
                name = problemName,
                group = problem.group ?: "",
                url = problem.url ?: "",
                archivedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                testCount = problem.tests.size,
                problem = problem
            )

            val metadataJson = json.encodeToString(archivedProblem)
            fileService.writeFile(archivePath.resolve("metadata.json"), metadataJson)

            println("✅ Problem archived successfully: $problemName")
            println("   Archive ID: $archiveId")
            println("   Test cases: ${problem.tests.size}")

            return ArchiveResponse(
                success = true,
                message = "Problem archived successfully",
                archiveId = archiveId
            )

        } catch (e: Exception) {
            println("❌ Error archiving problem: ${e.message}")
            e.printStackTrace()
            return ArchiveResponse(
                success = false,
                message = "Error archiving problem: ${e.message}"
            )
        }
    }

    /**
     * Get list of all archived problems
     */
    fun listArchives(): ArchiveListResponse {
        val archives = mutableListOf<ArchivedProblem>()

        try {
            if (Files.exists(archiveDir)) {
                Files.list(archiveDir).use { dirs ->
                    dirs.forEach { dir ->
                        if (Files.isDirectory(dir)) {
                            val metadataFile = dir.resolve("metadata.json")
                            if (Files.exists(metadataFile)) {
                                try {
                                    val metadata = json.decodeFromString<ArchivedProblem>(
                                        Files.readString(metadataFile)
                                    )
                                    archives.add(metadata)
                                } catch (e: Exception) {
                                    println("⚠️ Error reading archive metadata: ${dir.fileName}")
                                }
                            }
                        }
                    }
                }
            }

            // Sort by archived date (newest first)
            archives.sortByDescending { it.archivedAt }

        } catch (e: Exception) {
            println("❌ Error listing archives: ${e.message}")
        }

        return ArchiveListResponse(archives, archives.size)
    }

    /**
     * Import an archived problem
     */
    suspend fun importArchive(archiveId: String): ArchiveResponse {
        val archivePath = archiveDir.resolve(archiveId)

        if (!Files.exists(archivePath)) {
            return ArchiveResponse(
                success = false,
                message = "Archive not found: $archiveId"
            )
        }

        try {
            // Read metadata
            val metadataFile = archivePath.resolve("metadata.json")
            if (!Files.exists(metadataFile)) {
                return ArchiveResponse(
                    success = false,
                    message = "Archive metadata not found"
                )
            }

            val archivedProblem = json.decodeFromString<ArchivedProblem>(
                Files.readString(metadataFile)
            )

            // Clear current sample directories
            val sampleInputDir = baseDir.resolve(SAMPLE_INPUT_FOLDER)
            val sampleOutputDir = baseDir.resolve(SAMPLE_EXPECTED_OUTPUT_FOLDER)
            val outputDir = baseDir.resolve(OUTPUT_FOLDER)

            fileService.clearDirectory(sampleInputDir)
            fileService.clearDirectory(sampleOutputDir)
            fileService.clearDirectory(outputDir)

            // Copy archived test cases
            val archivedInputDir = archivePath.resolve("input")
            val archivedOutputDir = archivePath.resolve("output")

            if (Files.exists(archivedInputDir)) {
                Files.list(archivedInputDir).use { files ->
                    files.forEach { file ->
                        Files.copy(file, sampleInputDir.resolve(file.fileName), StandardCopyOption.REPLACE_EXISTING)
                    }
                }
            }

            if (Files.exists(archivedOutputDir)) {
                Files.list(archivedOutputDir).use { files ->
                    files.forEach { file ->
                        Files.copy(file, sampleOutputDir.resolve(file.fileName), StandardCopyOption.REPLACE_EXISTING)
                    }
                }
            }

            // Restore Solver.kt
            val archivedSolver = archivePath.resolve("Solver.kt")
            val currentSolver = baseDir.resolve("src/main/kotlin/Solver.kt")
            if (Files.exists(archivedSolver)) {
                Files.copy(archivedSolver, currentSolver, StandardCopyOption.REPLACE_EXISTING)
            }

            // Save problem metadata
            val metadataContent = buildString {
                appendLine(archivedProblem.problem.name ?: "")
                appendLine(archivedProblem.problem.group ?: "")
                appendLine(archivedProblem.problem.url ?: "")
                appendLine("TL(ms)=${archivedProblem.problem.timeLimit ?: -1}, ML(MB)=${archivedProblem.problem.memoryLimit ?: -1}")
            }
            fileService.writeFile(baseDir.resolve(".problem-meta.txt"), metadataContent)

            println("✅ Archive imported successfully: ${archivedProblem.name}")
            println("   Test cases: ${archivedProblem.testCount}")

            return ArchiveResponse(
                success = true,
                message = "Archive imported successfully: ${archivedProblem.name}"
            )

        } catch (e: Exception) {
            println("❌ Error importing archive: ${e.message}")
            e.printStackTrace()
            return ArchiveResponse(
                success = false,
                message = "Error importing archive: ${e.message}"
            )
        }
    }

    /**
     * Delete an archived problem
     */
    fun deleteArchive(archiveId: String): ArchiveResponse {
        val archivePath = archiveDir.resolve(archiveId)

        if (!Files.exists(archivePath)) {
            return ArchiveResponse(
                success = false,
                message = "Archive not found: $archiveId"
            )
        }

        try {
            // Delete the archive directory recursively
            Files.walk(archivePath)
                .sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }

            println("✅ Archive deleted: $archiveId")

            return ArchiveResponse(
                success = true,
                message = "Archive deleted successfully"
            )

        } catch (e: Exception) {
            println("❌ Error deleting archive: ${e.message}")
            return ArchiveResponse(
                success = false,
                message = "Error deleting archive: ${e.message}"
            )
        }
    }

    /**
     * Generate a unique archive ID from problem name
     */
    private fun generateArchiveId(problemName: String): String {
        val sanitized = problemName
            .replace(Regex("[^a-zA-Z0-9-_]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .take(50)

        val timestamp = System.currentTimeMillis()
        return "${sanitized}_$timestamp"
    }
}

