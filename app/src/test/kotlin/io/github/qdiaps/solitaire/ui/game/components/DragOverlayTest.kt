package io.github.qdiaps.solitaire.ui.game.components

import androidx.compose.ui.unit.dp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DragOverlayTest {

    private val cardHeight = 100.dp
    private val faceUpPeek = 25.dp

    @Nested
    @DisplayName("Elevation Constants")
    inner class ConstantsTests {

        @Test
        fun `drag overlay uses 12dp elevation for lifted cards`() {
            assertEquals(12.dp, DRAG_OVERLAY_ELEVATION)
        }
    }

    @Nested
    @DisplayName("Empty and Zero Stack Dimensions")
    inner class EmptyStackTests {

        @Test
        fun `height is 0dp when card count is zero or negative`() {
            assertEquals(0.dp, calculateDragOverlayStackHeight(0, cardHeight, faceUpPeek))
            assertEquals(0.dp, calculateDragOverlayStackHeight(-1, cardHeight, faceUpPeek))
        }

        @Test
        fun `offsets list is empty when card count is zero or negative`() {
            assertTrue(calculateDragStackOffsets(0, faceUpPeek).isEmpty())
            assertTrue(calculateDragStackOffsets(-2, faceUpPeek).isEmpty())
        }
    }

    @Nested
    @DisplayName("Single Card Dimensions")
    inner class SingleCardTests {

        @Test
        fun `height for single card equals cardHeight exactly`() {
            val height = calculateDragOverlayStackHeight(1, cardHeight, faceUpPeek)
            assertEquals(cardHeight, height)
        }

        @Test
        fun `offsets for single card contains only 0dp`() {
            val offsets = calculateDragStackOffsets(1, faceUpPeek)
            assertEquals(listOf(0.dp), offsets)
        }
    }

    @Nested
    @DisplayName("Multi-Card Stack Dimensions")
    inner class MultiCardStackTests {

        @Test
        fun `height for three cards includes base height and peek for extra cards`() {
            // 100.dp + (3 - 1) * 25.dp = 150.dp
            val height = calculateDragOverlayStackHeight(3, cardHeight, faceUpPeek)
            assertEquals(150.dp, height)
        }

        @Test
        fun `offsets for three cards cascade linearly by faceUpPeek`() {
            val offsets = calculateDragStackOffsets(3, faceUpPeek)
            assertEquals(listOf(0.dp, 25.dp, 50.dp), offsets)
        }

        @Test
        fun `height and offsets for stack of five cards`() {
            // 100.dp + (5 - 1) * 25.dp = 200.dp
            val height = calculateDragOverlayStackHeight(5, cardHeight, faceUpPeek)
            assertEquals(200.dp, height)

            val offsets = calculateDragStackOffsets(5, faceUpPeek)
            assertEquals(listOf(0.dp, 25.dp, 50.dp, 75.dp, 100.dp), offsets)
        }
    }
}
