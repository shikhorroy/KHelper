package com.roy.khelper.generator

import com.intellij.openapi.application.ReadAction
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

/** Single-file merges. Merging other project files needs real files under the project folder and is not covered. */
class MainGeneratorTest : LightJavaCodeInsightFixtureTestCase() {

    fun `test java solution becomes public class Main and helpers lose public`() {
        val file = myFixture.configureByText(
            "JSolution.java",
            """
            package problems.a_test;

            import java.util.Scanner;
            import problems.a_test.*;

            public class JSolution {
                public static void main(String[] args) {
                    new JSolution().run(new Scanner(System.in));
                }

                void run(Scanner in) {
                    System.out.println(new Helper().twice(in.nextInt()));
                }
            }

            public final class Helper {
                int twice(int x) { return 2 * x; }
            }
            """.trimIndent(),
        )

        val main = ReadAction.compute<String, Exception> { MainGenerator.buildContent(project, file) }

        assertTrue(main, main.contains("public class Main {"))
        assertTrue(main, main.contains("new Main().run("))
        assertFalse(main, main.contains("JSolution"))
        assertTrue(main, main.contains("\nfinal class Helper"))
        assertTrue(main, main.contains("import java.util.Scanner;"))
        assertFalse("own package star import must be dropped:\n$main", main.contains("import problems."))
        assertFalse(main, main.contains("package "))
    }

    fun `test kotlin helper class next to top-level main is kept and main is used`() {
        val file = myFixture.configureByText(
            "KSolution.kt",
            """
            package problems.a_test

            import problems.a_test.*
            import kotlin.math.max

            class Edge(val to: Int)

            fun best(a: Int, b: Int) = max(a, b)

            fun main() {
                val edge = Edge(best(1, 2))
                println(edge.to)
            }
            """.trimIndent(),
        )

        val main = ReadAction.compute<String, Exception> { MainGenerator.buildContent(project, file) }

        assertTrue(main, main.contains("class Edge(val to: Int)"))
        assertTrue(main, main.contains("fun best(a: Int, b: Int) = max(a, b)"))
        assertTrue(main, main.contains("fun main() {\n    val edge = Edge(best(1, 2))\n    println(edge.to)\n}"))
        assertTrue(main, main.contains("import kotlin.math.max"))
        assertFalse("own package star import must be dropped:\n$main", main.contains("import problems."))
    }

    fun `test legacy kotlin KSolution class uses solve as main`() {
        val file = myFixture.configureByText(
            "KSolution.kt",
            """
            package problems.a_test

            class KSolution {
                fun solve() {
                    println(answer())
                }

                fun answer() = 42
            }
            """.trimIndent(),
        )

        val main = ReadAction.compute<String, Exception> { MainGenerator.buildContent(project, file) }

        assertTrue(main, main.contains("fun answer() = 42"))
        assertTrue(main, main.contains("fun main() {\n    println(answer())\n}"))
    }
}
