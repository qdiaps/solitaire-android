package io.github.qdiaps.solitaire.ui.game.components

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class GameFormattersTest {

    @Nested
    @DisplayName("formatTime tests")
    inner class FormatTimeTests {

        @ParameterizedTest(name = "{0} seconds formats to {1}")
        @CsvSource(
            "0, 00:00",
            "-1, 00:00",
            "-100, 00:00",
            "5, 00:05",
            "59, 00:59",
            "60, 01:00",
            "61, 01:01",
            "125, 02:05",
            "599, 09:59",
            "600, 10:00",
            "3600, 60:00",
            "3661, 61:01"
        )
        fun `verify formatted time strings`(seconds: Long, expected: String) {
            assertEquals(expected, formatTime(seconds))
        }
    }
}
