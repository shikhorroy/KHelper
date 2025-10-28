import ktorapp.constant.INPUT_FILE
import ktorapp.constant.OUTPUT_FILE
import ktorapp.constant.OUTPUT_FOLDER
import ktorapp.constant.SAMPLE_INPUT_FOLDER
import ktorapp.services.MatchedTestTracker
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*

fun main() {
    val inputDir = File(SAMPLE_INPUT_FOLDER)
    val hasBatch = inputDir.exists() && inputDir.isDirectory &&
            inputDir.listFiles()?.any { isTxt(it) } == true

    if (hasBatch) {
        System.err.println("[LOCAL-BATCH] Running batch tests...")

        val tracker = MatchedTestTracker()
        val allFiles = inputDir.listFiles()!!.filter { isTxt(it) }
            .sortedBy { numeric(it) }

        val unmatchedFiles = tracker.filterUnmatched(allFiles)

        if (unmatchedFiles.isEmpty()) {
            System.err.println("✅ All ${allFiles.size} test(s) already matched! Skipping batch run.")
            System.err.println("💡 Tip: Delete .matched-tests.txt to re-run all tests")
            return
        }

        if (tracker.getMatchedCount() > 0) {
            System.err.println("⏭️  Skipping ${tracker.getMatchedCount()} already matched test(s)")
            System.err.println("▶️  Running ${unmatchedFiles.size} unmatched test(s): ${unmatchedFiles.joinToString(", ") { it.name }}")
        }

        val outDir = File(OUTPUT_FOLDER)
        if (!outDir.exists()) outDir.mkdirs()

        for (inF in unmatchedFiles) {
            val outF = File(outDir, inF.name)
            FileInputStream(inF).use { fis ->
                FileOutputStream(outF).use { fos ->
                    FastWriter(fos).use { fw ->
                        solve(FastScanner(fis), fw)
                    }
                }
            }
            System.err.println("${SAMPLE_INPUT_FOLDER}/${inF.name} -> ${OUTPUT_FOLDER}/${outF.name}")
        }
        return
    }

    // Single local OR Online Judge fallback
    val local = File(INPUT_FILE).exists()
    if (local) {
        FileInputStream(INPUT_FILE).use { fis ->
            FileOutputStream(OUTPUT_FILE).use { fos ->
                FastWriter(fos).use { fw ->
                    solve(FastScanner(fis), fw)
                }
            }
        }
        System.err.println("[LOCAL] ${INPUT_FILE} -> ${OUTPUT_FILE}")
    } else {
        FastWriter(System.out).use { out ->
            solve(FastScanner(System.`in`), out)
        }
    }
}

// ---------- Utilities ----------
private fun isTxt(f: File) =
    f.isFile && f.name.lowercase(Locale.ROOT).endsWith(".txt")

private fun numeric(f: File): Long =
    f.name.substringBeforeLast('.', f.name).toLongOrNull() ?: Long.MAX_VALUE