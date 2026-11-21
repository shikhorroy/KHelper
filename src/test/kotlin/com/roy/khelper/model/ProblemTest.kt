package com.roy.khelper.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The id is the package name and the key of saved problems, so these results must never change. */
class ProblemTest {

    @Test
    fun `id is lowercase with underscores`() {
        assertEquals("a_watermelon", Problem(name = "A. Watermelon").generateId())
        assertEquals("d_two_arrays_ii", Problem(name = "  D -- Two  Arrays (II)  ").generateId())
    }

    @Test
    fun `id never starts with a digit`() {
        assertEquals("p1000_a_b", Problem(name = "1000. A+B").generateId())
    }

    @Test
    fun `name without letters or digits gets a stable fallback id`() {
        val id = Problem(name = "???").generateId()
        assertTrue(id.startsWith("problem_"))
        assertEquals(id, Problem(name = "???").generateId())
    }
}
