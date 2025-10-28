package ktorapp.services

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
}

