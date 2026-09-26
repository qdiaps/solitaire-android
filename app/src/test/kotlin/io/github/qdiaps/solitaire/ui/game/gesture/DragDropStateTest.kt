package io.github.qdiaps.solitaire.ui.game.gesture

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.CardLocation
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlinx.coroutines.test.runTest

import androidx.compose.runtime.MonotonicFrameClock
import kotlinx.coroutines.withContext


private class FakeHapticFeedback : HapticFeedback {
    var performedCount = 0
    var lastFeedbackType: HapticFeedbackType? = null

    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        performedCount++
        lastFeedbackType = hapticFeedbackType
    }
}

private class TestFrameClock(private val frameTimeNanos: Long = 16_000_000L) : MonotonicFrameClock {
    private var time = 0L
    override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
        time += frameTimeNanos
        return onFrame(time)
    }
}

class DragDropStateTest {

    private val cardAceHearts = Card(Suit.HEARTS, Rank.ACE, isFaceUp = true)
    private val cardTwoClubs = Card(Suit.CLUBS, Rank.TWO, isFaceUp = true)
    private val cardThreeHearts = Card(Suit.HEARTS, Rank.THREE, isFaceUp = true)
    private val cardFaceDown = Card(Suit.SPADES, Rank.KING, isFaceUp = false)

    @Nested
    @DisplayName("Initial idle state")
    inner class InitialStateTests {

        @Test
        fun `initial state is completely idle with default offsets`() {
            val state = DragDropState()

            assertFalse(state.isDragging)
            assertNull(state.sourceLocation)
            assertTrue(state.draggedCards.isEmpty())
            assertEquals(Offset.Zero, state.originPosition)
            assertEquals(Offset.Zero, state.dragPosition)
            assertEquals(Offset.Zero, state.dragOffset)
            assertFalse(state.isCardDragged(cardAceHearts))
            assertFalse(state.isCardHidden(CardLocation.Tableau(0), 0))
            assertFalse(state.isCardHidden(CardLocation.Waste, 0))
            assertFalse(state.isCardHidden(CardLocation.Foundation(0), 0))
        }
    }

    @Nested
    @DisplayName("Active drag lifecycle and coordinates tracking")
    inner class DragLifecycleTests {

        @Test
        fun `startDrag initializes active state and coordinates`() {
            val state = DragDropState()
            val origin = Offset(100f, 200f)
            val cards = listOf(cardTwoClubs, cardAceHearts)
            val source = CardLocation.Tableau(columnIndex = 1, cardIndex = 2)

            state.startDrag(source = source, cards = cards, originPosition = origin)

            assertTrue(state.isDragging)
            assertEquals(source, state.sourceLocation)
            assertEquals(cards, state.draggedCards)
            assertEquals(origin, state.originPosition)
            assertEquals(origin, state.dragPosition)
            assertEquals(Offset.Zero, state.dragOffset)
            assertTrue(state.isCardDragged(cardTwoClubs))
            assertTrue(state.isCardDragged(cardAceHearts))
            assertFalse(state.isCardDragged(cardThreeHearts))
        }

        @Test
        fun `startDrag rejects empty card list`() {
            val state = DragDropState()
            assertThrows(IllegalArgumentException::class.java) {
                state.startDrag(CardLocation.Waste, emptyList<Card>())
            }
        }

        @Test
        fun `onDragDelta accumulates motion and updates dragOffset`() {
            val state = DragDropState()
            val origin = Offset(100f, 200f)
            state.startDrag(CardLocation.Waste, listOf(cardAceHearts), originPosition = origin)

            state.onDragDelta(Offset(15f, -25f))
            assertEquals(Offset(115f, 175f), state.dragPosition)
            assertEquals(Offset(15f, -25f), state.dragOffset)

            state.onDragDelta(Offset(10f, 5f))
            assertEquals(Offset(125f, 180f), state.dragPosition)
            assertEquals(Offset(25f, -20f), state.dragOffset)
        }

        @Test
        fun `updateDragPosition sets absolute position directly`() {
            val state = DragDropState()
            val origin = Offset(50f, 50f)
            state.startDrag(CardLocation.Waste, listOf(cardAceHearts), originPosition = origin)

            state.updateDragPosition(Offset(200f, 350f))
            assertEquals(Offset(200f, 350f), state.dragPosition)
            assertEquals(Offset(150f, 300f), state.dragOffset)
        }

        @Test
        fun `reset restores state to idle defaults`() {
            val state = DragDropState()
            state.startDrag(CardLocation.Tableau(0), listOf(cardAceHearts), originPosition = Offset(50f, 50f))
            state.onDragDelta(Offset(100f, 100f))

            state.reset()

            assertFalse(state.isDragging)
            assertNull(state.sourceLocation)
            assertTrue(state.draggedCards.isEmpty())
            assertEquals(Offset.Zero, state.originPosition)
            assertEquals(Offset.Zero, state.dragPosition)
            assertEquals(Offset.Zero, state.dragOffset)
        }
    }

    @Nested
    @DisplayName("Tableau sub-stack slicing and initiation")
    inner class TableauSlicingTests {

        private val column = listOf(
            cardFaceDown,
            cardThreeHearts,
            cardTwoClubs,
            cardAceHearts
        )

        @Test
        fun `sliceTableauStack returns empty list for out of bounds index`() {
            val state = DragDropState()
            assertTrue(state.sliceTableauStack(column, -1).isEmpty())
            assertTrue(state.sliceTableauStack(column, 4).isEmpty())
            assertTrue(state.sliceTableauStack(emptyList<Card>(), 0).isEmpty())
        }

        @Test
        fun `sliceTableauStack returns empty list for face-down card`() {
            val state = DragDropState()
            // Index 0 is face-down
            assertTrue(state.sliceTableauStack(column, 0).isEmpty())
        }

        @Test
        fun `sliceTableauStack slices single top card`() {
            val state = DragDropState()
            // Top card at index 3
            val sliced = state.sliceTableauStack(column, 3)
            assertEquals(listOf(cardAceHearts), sliced)
        }

        @Test
        fun `sliceTableauStack slices multi-card sub-stack`() {
            val state = DragDropState()
            // Sub-stack from index 1 (Three of Hearts down to Ace of Hearts)
            val sliced = state.sliceTableauStack(column, 1)
            assertEquals(listOf(cardThreeHearts, cardTwoClubs, cardAceHearts), sliced)
        }

        @Test
        fun `startTableauDrag successfully lifts sub-stack`() {
            val state = DragDropState()
            val origin = Offset(80f, 150f)
            val success = state.startTableauDrag(
                columnIndex = 2,
                cardIndex = 1,
                columnCards = column,
                originPosition = origin
            )

            assertTrue(success)
            assertTrue(state.isDragging)
            assertEquals(CardLocation.Tableau(columnIndex = 2, cardIndex = 1), state.sourceLocation)
            assertEquals(listOf(cardThreeHearts, cardTwoClubs, cardAceHearts), state.draggedCards)
            assertEquals(origin, state.originPosition)
            assertEquals(origin, state.dragPosition)
        }

        @Test
        fun `startTableauDrag rejects face-down card drag`() {
            val state = DragDropState()
            val success = state.startTableauDrag(
                columnIndex = 2,
                cardIndex = 0, // face-down
                columnCards = column
            )

            assertFalse(success)
            assertFalse(state.isDragging)
            assertNull(state.sourceLocation)
        }
    }

    @Nested
    @DisplayName("Waste and Foundation drag initiation")
    inner class WasteAndFoundationDragTests {

        @Test
        fun `startWasteDrag lifts top card when available and face-up`() {
            val state = DragDropState()
            val origin = Offset(70f, 30f)
            val wasteCards = listOf(cardTwoClubs, cardAceHearts)

            val success = state.startWasteDrag(wasteCards = wasteCards, originPosition = origin)

            assertTrue(success)
            assertTrue(state.isDragging)
            assertEquals(CardLocation.Waste, state.sourceLocation)
            assertEquals(listOf(cardAceHearts), state.draggedCards)
            assertEquals(origin, state.originPosition)
        }

        @Test
        fun `startWasteDrag fails on empty waste`() {
            val state = DragDropState()
            val success = state.startWasteDrag(wasteCards = emptyList<Card>())

            assertFalse(success)
            assertFalse(state.isDragging)
        }

        @Test
        fun `startFoundationDrag lifts top card when available`() {
            val state = DragDropState()
            val origin = Offset(240f, 30f)
            val foundationCards = listOf(cardAceHearts, cardTwoClubs)

            val success = state.startFoundationDrag(
                foundationIndex = 1,
                foundationCards = foundationCards,
                originPosition = origin
            )

            assertTrue(success)
            assertTrue(state.isDragging)
            assertEquals(CardLocation.Foundation(1), state.sourceLocation)
            assertEquals(listOf(cardTwoClubs), state.draggedCards)
            assertEquals(origin, state.originPosition)
        }

        @Test
        fun `startFoundationDrag fails on empty foundation`() {
            val state = DragDropState()
            val success = state.startFoundationDrag(
                foundationIndex = 0,
                foundationCards = emptyList<Card>()
            )

            assertFalse(success)
            assertFalse(state.isDragging)
        }
    }

    @Nested
    @DisplayName("Source card visibility flags")
    inner class SourceVisibilityTests {

        @Test
        fun `isCardHidden hides lifted cards in source tableau column only`() {
            val state = DragDropState()
            // Lift cards 2..4 from column 3
            state.startDrag(
                source = CardLocation.Tableau(columnIndex = 3, cardIndex = 2),
                cards = listOf(cardTwoClubs, cardAceHearts)
            )

            // In source column 3:
            assertFalse(state.isCardHidden(CardLocation.Tableau(3), 0))
            assertFalse(state.isCardHidden(CardLocation.Tableau(3), 1))
            assertTrue(state.isCardHidden(CardLocation.Tableau(3), 2))
            assertTrue(state.isCardHidden(CardLocation.Tableau(3), 3))

            // In another column 2:
            assertFalse(state.isCardHidden(CardLocation.Tableau(2), 2))

            // In waste or foundations:
            assertFalse(state.isCardHidden(CardLocation.Waste, 0))
            assertFalse(state.isCardHidden(CardLocation.Foundation(0), 0))
        }

        @Test
        fun `isCardHidden hides waste top card when dragging from waste`() {
            val state = DragDropState()
            state.startDrag(CardLocation.Waste, listOf(cardAceHearts))

            assertTrue(state.isCardHidden(CardLocation.Waste, 0))
            assertFalse(state.isCardHidden(CardLocation.Foundation(0), 0))
            assertFalse(state.isCardHidden(CardLocation.Tableau(0), 0))
        }

        @Test
        fun `isCardHidden hides foundation top card when dragging from foundation`() {
            val state = DragDropState()
            state.startDrag(CardLocation.Foundation(2), listOf(cardAceHearts))

            assertTrue(state.isCardHidden(CardLocation.Foundation(2), 0))
            assertFalse(state.isCardHidden(CardLocation.Foundation(1), 0))
            assertFalse(state.isCardHidden(CardLocation.Tableau(2), 0))
        }
    }

    @Nested
    @DisplayName("Snap-back animation and lifecycle")
    inner class SnapBackTests {

        @Test
        fun `snapBack returns dragPosition to originPosition and resets state`() = runTest {
            val state = DragDropState()
            val origin = Offset(100f, 150f)
            state.startDrag(CardLocation.Waste, listOf(cardAceHearts), originPosition = origin)
            state.onDragDelta(Offset(200f, 300f))

            assertEquals(Offset(300f, 450f), state.dragPosition)
            assertTrue(state.isDragging)
            assertFalse(state.isSnappingBack)
            assertTrue(state.isActive)

            withContext(TestFrameClock()) { state.snapBack() }

            assertFalse(state.isDragging)
            assertFalse(state.isSnappingBack)
            assertFalse(state.isActive)
            assertEquals(Offset.Zero, state.dragPosition)
            assertEquals(Offset.Zero, state.originPosition)
            assertTrue(state.draggedCards.isEmpty())
        }

        @Test
        fun `isCardDragged and isCardHidden remain true during active drag and reset after snapBack`() = runTest {
            val state = DragDropState()
            val origin = Offset(100f, 150f)
            state.startDrag(
                CardLocation.Tableau(columnIndex = 2, cardIndex = 1),
                listOf(cardAceHearts),
                originPosition = origin
            )

            assertTrue(state.isCardDragged(cardAceHearts))
            assertTrue(state.isCardHidden(CardLocation.Tableau(2), 1))

            withContext(TestFrameClock()) { state.snapBack() }

            assertFalse(state.isCardDragged(cardAceHearts))
            assertFalse(state.isCardHidden(CardLocation.Tableau(2), 1))
        }

        @Test
        fun `snapBack when not active does nothing`() = runTest {
            val state = DragDropState()
            withContext(TestFrameClock()) { state.snapBack() }
            assertFalse(state.isActive)
        }
    }

    @Nested
    @DisplayName("Haptic feedback and onDropRelease integration")
    inner class HapticFeedbackAndDropReleaseTests {

        @Test
        fun `card pickup triggers haptic feedback`() {
            val state = DragDropState()
            val fakeHaptics = FakeHapticFeedback()

            state.startDrag(
                source = CardLocation.Waste,
                cards = listOf(cardAceHearts),
                hapticFeedback = fakeHaptics
            )

            assertEquals(1, fakeHaptics.performedCount)
            assertEquals(HapticFeedbackType.LongPress, fakeHaptics.lastFeedbackType)
        }

        @Test
        fun `valid onDropRelease triggers haptic snap and invokes onValidDrop`() = runTest {
            val state = DragDropState()
            val fakeHaptics = FakeHapticFeedback()
            val registry = DropTargetRegistry()
            val foundation0 = CardLocation.Foundation(0)
            registry.register(foundation0, Rect(0f, 0f, 100f, 140f))

            val board = BoardState(waste = listOf(cardAceHearts))
            state.startDrag(
                source = CardLocation.Waste,
                cards = listOf(cardAceHearts),
                originPosition = Offset(0f, 0f)
            )

            var droppedCards: List<Card>? = null
            var droppedSource: CardLocation? = null
            var droppedTarget: CardLocation? = null

            val result = withContext(TestFrameClock()) {
                state.onDropRelease(
                    boardState = board,
                    registry = registry,
                    draggedBounds = Rect(10f, 10f, 90f, 130f),
                    hapticFeedback = fakeHaptics,
                    onValidDrop = { cards, src, tgt ->
                        droppedCards = cards
                        droppedSource = src
                        droppedTarget = tgt
                    }
                )
            }

            assertTrue(result)
            assertFalse(state.isActive)
            assertEquals(1, fakeHaptics.performedCount)
            assertEquals(HapticFeedbackType.LongPress, fakeHaptics.lastFeedbackType)
            assertEquals(listOf(cardAceHearts), droppedCards)
            assertEquals(CardLocation.Waste, droppedSource)
            assertEquals(foundation0, droppedTarget)
        }

        @Test
        fun `invalid onDropRelease animates snap-back and does not invoke onValidDrop`() = runTest {
            val state = DragDropState()
            val fakeHaptics = FakeHapticFeedback()
            val registry = DropTargetRegistry()
            val foundation0 = CardLocation.Foundation(0)
            registry.register(foundation0, Rect(0f, 0f, 100f, 140f))

            // Two of Clubs cannot go to empty Foundation 0 (requires Ace)
            val board = BoardState(waste = listOf(cardTwoClubs))
            val origin = Offset(50f, 50f)
            state.startDrag(
                source = CardLocation.Waste,
                cards = listOf(cardTwoClubs),
                originPosition = origin
            )
            state.onDragDelta(Offset(100f, 100f))

            var dropInvoked = false

            val result = withContext(TestFrameClock()) {
                state.onDropRelease(
                    boardState = board,
                    registry = registry,
                    draggedBounds = Rect(10f, 10f, 90f, 130f),
                    hapticFeedback = fakeHaptics,
                    onValidDrop = { _, _, _ -> dropInvoked = true }
                )
            }

            assertFalse(result)
            assertFalse(dropInvoked)
            assertFalse(state.isActive)
            assertEquals(0, fakeHaptics.performedCount) // no drop snap on invalid drop
            assertEquals(Offset.Zero, state.dragPosition)
        }

        @Test
        fun `onDropRelease returns false when state is idle`() = runTest {
            val state = DragDropState()
            val registry = DropTargetRegistry()
            val board = BoardState()

            var dropInvoked = false
            val result = withContext(TestFrameClock()) {
                state.onDropRelease(
                    boardState = board,
                    registry = registry,
                    draggedBounds = Rect(0f, 0f, 10f, 10f),
                    onValidDrop = { _, _, _ -> dropInvoked = true }
                )
            }

            assertFalse(result)
            assertFalse(dropInvoked)
        }
    }
}
