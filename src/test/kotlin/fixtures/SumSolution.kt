package fixtures

// Sample "solutions" that RemoteTestRunnerTest runs in a separate JVM.

//~ Read once per class load. The runner must give each test a fresh copy, or test 2 reads test 1's stream.
private val reader = System.`in`.bufferedReader()

/** Kotlin top-level main: compiles to fixtures.SumSolutionKt. Prints the sum of two numbers. */
fun main() {
    val (a, b) = reader.readLine().trim().split(" ").map { it.toLong() }
    println(a + b)
}

class WrongSolution {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            println("wrong")
        }
    }
}

class CrashingSolution {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            error("boom")
        }
    }
}

/** Recursion this deep needs the large stack the runner gives to the solution thread. */
class DeepRecursionSolution {
    companion object {
        private fun depth(n: Int): Int = if (n == 0) 0 else 1 + depth(n - 1)

        @JvmStatic
        fun main(args: Array<String>) {
            println(depth(readln().trim().toInt()))
        }
    }
}
