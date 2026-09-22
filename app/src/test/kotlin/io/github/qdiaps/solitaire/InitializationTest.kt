package io.github.qdiaps.solitaire

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class InitializationTest {

    @Test
    @DisplayName("Verify testing pipeline and JUnit 6 setup")
    fun `test pipeline works properly`() {
        val expected = 52
        val actual = 13 * 4
        assertEquals(expected, actual, "Standard deck should contain 52 cards")
        assertTrue(true)
    }
}