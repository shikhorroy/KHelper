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
data class GroupedArchive(
    val groupName: String,
    val problems: List<ArchivedProblem>,
    val problemCount: Int
)

@Serializable
data class ArchiveListResponse(
    val groups: List<GroupedArchive>,
    val totalProblems: Int,
    val totalGroups: Int
)

@Serializable
data class ArchiveResponse(
    val success: Boolean,
    val message: String,
    val archiveId: String? = null
)

@Serializable
data class GroupRenameRequest(
    val oldGroupName: String,
    val newGroupName: String
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
     * Archive the current problem with its test cases and Solution.kt
     */
    suspend fun archiveProblem(problem: Problem, overwrite: Boolean = false): ArchiveResponse {
        val problemName = problem.name ?: "Unknown Problem"
        val groupName = problem.group?.takeIf { it.isNotBlank() } ?: "Ungrouped"

        val archiveId = generateArchiveId(problemName)
        val groupPath = archiveDir.resolve(sanitizeGroupName(groupName))
        val archivePath = groupPath.resolve(archiveId)

        // Check if archive already exists
        if (Files.exists(archivePath) && !overwrite) {
            return ArchiveResponse(
                success = false,
                message = "Archive already exists: $problemName",
                archiveId = archiveId
            )
        }

        try {
            // Create group and archive directories
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

            // Copy Solution.kt
            val solutionFile = baseDir.resolve("src/main/kotlin/Solution.kt")
            if (Files.exists(solutionFile)) {
                Files.copy(solutionFile, archivePath.resolve("Solution.kt"), StandardCopyOption.REPLACE_EXISTING)
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
     * Get list of all archived problems grouped by their groups
     */
    fun listArchives(): ArchiveListResponse {
        val groupedArchives = mutableMapOf<String, MutableList<ArchivedProblem>>()

        try {
            if (Files.exists(archiveDir)) {
                // Iterate through group directories
                Files.list(archiveDir).use { groupDirs ->
                    groupDirs.forEach { groupDir ->
                        if (Files.isDirectory(groupDir)) {
                            // Iterate through problem directories in this group
                            Files.list(groupDir).use { problemDirs ->
                                problemDirs.forEach { problemDir ->
                                    if (Files.isDirectory(problemDir)) {
                                        val metadataFile = problemDir.resolve("metadata.json")
                                        if (Files.exists(metadataFile)) {
                                            try {
                                                val metadata = json.decodeFromString<ArchivedProblem>(
                                                    Files.readString(metadataFile)
                                                )
                                                groupedArchives
                                                    .getOrPut(metadata.group.ifBlank { "Ungrouped" }) { mutableListOf() }
                                                    .add(metadata)
                                            } catch (_: Exception) {
                                                println("⚠️ Error reading archive metadata: ${problemDir.fileName}")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        } catch (_: Exception) {
            println("❌ Error listing archives")
        }

        val groups = groupedArchives.map { (groupName, problems) ->
            problems.sortByDescending { it.archivedAt }
            GroupedArchive(
                groupName = groupName,
                problems = problems,
                problemCount = problems.size
            )
        }.sortedBy { it.groupName }

        val totalProblems = groups.sumOf { it.problemCount }

        return ArchiveListResponse(
            groups = groups,
            totalProblems = totalProblems,
            totalGroups = groups.size
        )
    }

    /**
     * Import an archived problem
     */
    suspend fun importArchive(archiveId: String, groupName: String): ArchiveResponse {
        val groupPath = archiveDir.resolve(sanitizeGroupName(groupName))
        val archivePath = groupPath.resolve(archiveId)

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

            // Restore Solution.kt
            val archivedSolution = archivePath.resolve("Solution.kt")
            val currentSolution = baseDir.resolve("src/main/kotlin/Solution.kt")
            if (Files.exists(archivedSolution)) {
                Files.copy(archivedSolution, currentSolution, StandardCopyOption.REPLACE_EXISTING)
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
    fun deleteArchive(archiveId: String, groupName: String): ArchiveResponse {
        val groupPath = archiveDir.resolve(sanitizeGroupName(groupName))
        val archivePath = groupPath.resolve(archiveId)

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

            // Check if group directory is empty and delete it
            if (Files.exists(groupPath)) {
                val isEmpty = Files.list(groupPath).use { it.count() == 0L }
                if (isEmpty) {
                    Files.deleteIfExists(groupPath)
                    println("✅ Empty group deleted: $groupName")
                }
            }

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
     * Delete an entire group and all its problems
     */
    fun deleteGroup(groupName: String): ArchiveResponse {
        val groupPath = archiveDir.resolve(sanitizeGroupName(groupName))

        if (!Files.exists(groupPath)) {
            return ArchiveResponse(
                success = false,
                message = "Group not found: $groupName"
            )
        }

        try {
            // Delete the group directory recursively
            Files.walk(groupPath)
                .sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }

            println("✅ Group deleted: $groupName")

            return ArchiveResponse(
                success = true,
                message = "Group deleted successfully"
            )

        } catch (e: Exception) {
            println("❌ Error deleting group: ${e.message}")
            return ArchiveResponse(
                success = false,
                message = "Error deleting group: ${e.message}"
            )
        }
    }

    /**
     * Rename a group
     */
    suspend fun renameGroup(oldGroupName: String, newGroupName: String): ArchiveResponse {
        if (oldGroupName.isBlank() || newGroupName.isBlank()) {
            return ArchiveResponse(
                success = false,
                message = "Group names cannot be empty"
            )
        }

        val oldGroupPath = archiveDir.resolve(sanitizeGroupName(oldGroupName))
        val newGroupPath = archiveDir.resolve(sanitizeGroupName(newGroupName))

        if (!Files.exists(oldGroupPath)) {
            return ArchiveResponse(
                success = false,
                message = "Group not found: $oldGroupName"
            )
        }

        if (Files.exists(newGroupPath)) {
            return ArchiveResponse(
                success = false,
                message = "Group already exists: $newGroupName"
            )
        }

        try {
            // Move the directory
            Files.move(oldGroupPath, newGroupPath)

            // Update metadata in all problems
            val problemDirs = Files.list(newGroupPath).use { it.toList() }
            for (problemDir in problemDirs) {
                if (Files.isDirectory(problemDir)) {
                    val metadataFile = problemDir.resolve("metadata.json")
                    if (Files.exists(metadataFile)) {
                        try {
                            val metadata = json.decodeFromString<ArchivedProblem>(
                                Files.readString(metadataFile)
                            )
                            val updatedMetadata = metadata.copy(group = newGroupName)
                            val updatedJson = json.encodeToString(updatedMetadata)
                            fileService.writeFile(metadataFile, updatedJson)
                        } catch (_: Exception) {
                            println("⚠️ Error updating metadata for: ${problemDir.fileName}")
                        }
                    }
                }
            }

            println("✅ Group renamed: $oldGroupName → $newGroupName")

            return ArchiveResponse(
                success = true,
                message = "Group renamed successfully"
            )

        } catch (e: Exception) {
            println("❌ Error renaming group: ${e.message}")
            return ArchiveResponse(
                success = false,
                message = "Error renaming group: ${e.message}"
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

    /**
     * Sanitize group name for file system
     */
    private fun sanitizeGroupName(groupName: String): String {
        return groupName
            .replace(Regex("[^a-zA-Z0-9-_ ]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .take(100)
            .ifBlank { "Ungrouped" }
    }
}

