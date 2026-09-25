package io.github.qdiaps.solitaire.ui.theme

import androidx.compose.ui.unit.dp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class CardDimensionsTest {

    @Nested
    @DisplayName("Card dimensions calculation")
    inner class CalculationTests {

        @ParameterizedTest
        @ValueSource(floats = [360f, 393f, 411f, 600f])
        fun `verify 5 to 7 aspect ratio for card height across standard widths`(width: Float) {
            val dimensions = CardDimensions.calculate(availableWidth = width.dp)
            val expectedHeight = dimensions.cardWidth.value * 1.4f
            assertEquals(expectedHeight, dimensions.cardHeight.value, 0.001f)
        }

        @ParameterizedTest
        @ValueSource(floats = [360f, 393f, 411f, 600f])
        fun `verify total width matches available width without overflow`(width: Float) {
            val dimensions = CardDimensions.calculate(availableWidth = width.dp)
            val totalCalculatedWidth = dimensions.horizontalPadding.value * 2 +
                dimensions.cardWidth.value * 7 +
                dimensions.columnSpacing.value * 6

            assertEquals(width, totalCalculatedWidth, 0.01f)
            assertTrue(dimensions.cardWidth.value > 0f)
            assertTrue(dimensions.cardHeight.value > 0f)
        }

        @Test
        fun `verify peek ratios where faceDownPeek is 20 percent and faceUpPeek is 35 percent of card height`() {
            val dimensions = CardDimensions.calculate(availableWidth = 360.dp)
            assertEquals(dimensions.cardHeight.value * 0.20f, dimensions.faceDownPeek.value, 0.001f)
            assertEquals(dimensions.cardHeight.value * 0.35f, dimensions.faceUpPeek.value, 0.001f)
            assertTrue(dimensions.faceDownPeek.value < dimensions.faceUpPeek.value)
        }

        @Test
        fun `verify corner radius is 8 percent of card width`() {
            val dimensions = CardDimensions.calculate(availableWidth = 393.dp)
            assertEquals(dimensions.cardWidth.value * 0.08f, dimensions.cornerRadius.value, 0.001f)
        }

        @Test
        fun `verify card width is clamped to maxCardWidth on wide screens`() {
            val maxAllowedWidth = 72.dp
            val dimensions = CardDimensions.calculate(
                availableWidth = 600.dp,
                maxCardWidth = maxAllowedWidth
            )

            assertEquals(maxAllowedWidth.value, dimensions.cardWidth.value, 0.001f)
            val expectedHorizontalPadding = (600f - (72f * 7 + dimensions.columnSpacing.value * 6)) / 2f
            assertEquals(expectedHorizontalPadding, dimensions.horizontalPadding.value, 0.01f)
        }

        @Test
        fun `verify default maxCardWidth caps card width on tablet screens`() {
            val dimensions = CardDimensions.calculate(availableWidth = 800.dp)
            assertEquals(CardDimensions.DEFAULT_MAX_CARD_WIDTH.value, dimensions.cardWidth.value, 0.001f)
        }
    }

    @Nested
    @DisplayName("Precondition validation")
    inner class ValidationTests {

        @ParameterizedTest
        @ValueSource(floats = [-10f, 0f, 10f, 31f])
        fun `verify throws exception for non-positive or too small available width`(invalidWidth: Float) {
            assertThrows(IllegalArgumentException::class.java) {
                CardDimensions.calculate(availableWidth = invalidWidth.dp)
            }
        }
    }
}
