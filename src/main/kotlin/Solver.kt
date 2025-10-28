import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InputStream
import java.io.OutputStream

// ---------- Your problem-specific solver ---------- //
fun solve(fs: FastScanner, out: FastWriter) {
    //~~️Keep the Dreams Alive in Your Heart ❤️~~//
    val tCases = fs.nextInt()
    for (cs in 1..tCases) {
        // code: ...
    }
}

fun main() {
    FastWriter(System.out).use { out ->
        solve(FastScanner(System.`in`), out)
    }
}

// ---------- Ultra-fast Scanner ----------
class FastScanner(
    input: InputStream,
    private val bufSize: Int = 1 shl 16 // 64 KiB; for small size input 1 shl 13 = 8 KiB
) {
    private val inStream = BufferedInputStream(input, bufSize)
    private val buffer = ByteArray(bufSize)
    private var len = 0
    private var ptr = 0

    private fun read(): Int {
        if (ptr >= len) {
            len = inStream.read(buffer)
            ptr = 0
            if (len <= 0) return -1
        }
        return buffer[ptr++].toInt()
    }

    private fun skipBlanks(): Int {
        var c = read()
        while (c in 0..32) c = read()
        return c
    }

    fun hasNext(): Boolean {
        var c = read()
        while (c in 0..32) c = read()
        if (c < 0) return false
        ptr--
        return true
    }

    fun next(): String {
        val sb = StringBuilder()
        var c = skipBlanks()
        while (c > 32) {
            sb.append(c.toChar())
            c = read()
        }
        return sb.toString()
    }

    fun nextInt(): Int {
        var c = skipBlanks()
        var sgn = 1
        if (c == '-'.code) {
            sgn = -1; c = read()
        }
        var res = 0
        while (c > 32) {
            res = res * 10 + (c - '0'.code)
            c = read()
        }
        return res * sgn
    }

    fun nextLong(): Long {
        var c = skipBlanks()
        var sgn = 1
        if (c == '-'.code) {
            sgn = -1; c = read()
        }
        var res = 0L
        while (c > 32) {
            res = res * 10 + (c - '0'.code)
            c = read()
        }
        return if (sgn == 1) res else -res
    }

    fun nextDouble(): Double = next().toDouble()
    fun nextLine(): String {
        val sb = StringBuilder()
        var c = read()
        if (c == '\n'.code) c = read()
        while (c >= 0 && c != '\n'.code) {
            if (c != '\r'.code) sb.append(c.toChar())
            c = read()
        }
        return sb.toString()
    }
}

// ---------- Ultra-fast Writer ----------
class FastWriter(
    private val out: OutputStream,
    bufSize: Int = 1 shl 16 // 64 KiB
) : AutoCloseable {
    private val bos = BufferedOutputStream(out, bufSize)
    private val tmp = ByteArray(20) // long পর্যন্ত ধরবে

    fun flush() = bos.flush()
    override fun close() = bos.flush()

    private fun wb(b: Int) {
        bos.write(b)
    }

    fun print(s: String) {
        var i = 0;
        val n = s.length
        while (i < n) {
            wb(s[i].code); i++
        }
    }

    fun println() = wb('\n'.code)
    fun print(ch: Char) = wb(ch.code)

    fun printInt(x: Int) {
        var v = x
        if (v == 0) {
            wb('0'.code); return
        }
        if (v < 0) {
            wb('-'.code); v = -v
        }
        var i = 0
        while (v > 0) {
            val d = v % 10
            tmp[i++] = ('0'.code + d).toByte()
            v /= 10
        }
        for (p in i - 1 downTo 0) wb(tmp[p].toInt())
    }

    fun printLong(x: Long) {
        var v = x
        if (v == 0L) {
            wb('0'.code); return
        }
        if (v < 0L) {
            wb('-'.code); v = -v
        }
        var i = 0
        while (v > 0L) {
            val d = (v % 10L).toInt()
            tmp[i++] = ('0'.code + d).toByte()
            v /= 10L
        }
        for (p in i - 1 downTo 0) wb(tmp[p].toInt())
    }

    fun printlnInt(x: Int) {
        printInt(x); println()
    }

    fun printlnLong(x: Long) {
        printLong(x); println()
    }

    fun println(s: String) {
        print(s); println()
    }
}