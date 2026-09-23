package io.github.qdiaps.solitaire.domain.engine

import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class UndoManagerTest {

    private val aceHearts = Card(Suit.HEARTS, Rank.ACE, isFaceUp = true)
    private val twoHearts = Card(Suit.HEARTS, Rank.TWO, isFaceUp = true)
    private val kingSpades = Card(Suit.SPADES, Rank.KING, isFaceUp = true)

    private val initialState = BoardState(
        score = 0,
        movesCount = 0,
        stock = listOf(aceHearts, twoHearts)
    )

    private val stateAfterMove1 = BoardState(
        score = 10,
        movesCount = 1,
        stock = listOf(twoHearts),
        waste = listOf(aceHearts)
    )

    private val stateAfterMove2 = BoardState(
        score = 15,
        movesCount = 2,
        foundations = listOf(listOf(aceHearts), emptyList(), emptyList(), emptyList()),
        waste = emptyList()
    )

    @Nested
    @DisplayName("Initial state and empty stack behavior")
    inner class InitialStateTests {

        @Test
        @DisplayName("Newly created UndoManager has empty stacks")
        fun `newly created UndoManager has empty stacks`() {
            val undoManager = UndoManager()

            assertFalse(undoManager.canUndo)
            assertFalse(undoManager.canRedo)
            assertEquals(0, undoManager.undoCount)
            assertEquals(0, undoManager.redoCount)
            assertNull(undoManager.peekUndo())
            assertNull(undoManager.peekRedo())
        }

        @Test
        @DisplayName("undo on empty stack returns null")
        fun `undo on empty stack returns null`() {
            val undoManager = UndoManager()

            val result = undoManager.undo(initialState)

            assertNull(result)
            assertFalse(undoManager.canUndo)
        }

        @Test
        @DisplayName("redo on empty stack returns null")
        fun `redo on empty stack returns null`() {
            val undoManager = UndoManager()

            val result = undoManager.redo(initialState)

            assertNull(result)
            assertFalse(undoManager.canRedo)
        }
    }

    @Nested
    @DisplayName("Basic record and undo operations")
    inner class RecordAndUndoTests {

        @Test
        @DisplayName("record adds state to undo stack")
        fun `record adds state to undo stack`() {
            val undoManager = UndoManager()

            undoManager.record(initialState)

            assertTrue(undoManager.canUndo)
            assertFalse(undoManager.canRedo)
            assertEquals(1, undoManager.undoCount)
            assertEquals(initialState, undoManager.peekUndo())
        }

        @Test
        @DisplayName("undo restores previous state and populates redo stack")
        fun `undo restores previous state and populates redo stack`() {
            val undoManager = UndoManager()
            undoManager.record(initialState)

            val restoredState = undoManager.undo(currentState = stateAfterMove1)

            assertEquals(initialState, restoredState)
            assertFalse(undoManager.canUndo)
            assertTrue(undoManager.canRedo)
            assertEquals(1, undoManager.redoCount)
            assertEquals(stateAfterMove1, undoManager.peekRedo())
        }

        @Test
        @DisplayName("undo rolls back score, movesCount, and card locations")
        fun `undo rolls back score movesCount and cards`() {
            val undoManager = UndoManager()
            undoManager.record(initialState)

            val restored = undoManager.undo(stateAfterMove1)

            assertNotNull(restored)
            assertEquals(0, restored!!.score)
            assertEquals(0, restored.movesCount)
            assertEquals(listOf(aceHearts, twoHearts), restored.stock)
            assertTrue(restored.waste.isEmpty())
        }

        @Test
        @DisplayName("Multiple consecutive undos restore states in LIFO order")
        fun `multiple undos restore states in LIFO order`() {
            val undoManager = UndoManager()
            undoManager.record(initialState)
            undoManager.record(stateAfterMove1)

            assertEquals(2, undoManager.undoCount)

            // Undo move 2 -> restores stateAfterMove1
            val undo1 = undoManager.undo(stateAfterMove2)
            assertEquals(stateAfterMove1, undo1)
            assertEquals(1, undoManager.undoCount)
            assertEquals(1, undoManager.redoCount)

            // Undo move 1 -> restores initialState
            val undo2 = undoManager.undo(undo1!!)
            assertEquals(initialState, undo2)
            assertEquals(0, undoManager.undoCount)
            assertEquals(2, undoManager.redoCount)
        }
    }

    @Nested
    @DisplayName("Redo operations")
    inner class RedoTests {

        @Test
        @DisplayName("redo restores undone state and re-enables undo")
        fun `redo restores undone state and re-enables undo`() {
            val undoManager = UndoManager()
            undoManager.record(initialState)

            val undoneState = undoManager.undo(currentState = stateAfterMove1)
            assertEquals(initialState, undoneState)

            val redoneState = undoManager.redo(currentState = undoneState!!)
            assertEquals(stateAfterMove1, redoneState)
            assertTrue(undoManager.canUndo)
            assertFalse(undoManager.canRedo)
        }

        @Test
        @DisplayName("Consecutive undos followed by consecutive redos restore full sequence")
        fun `consecutive undos and redos restore full sequence`() {
            val undoManager = UndoManager()
            undoManager.record(initialState)
            undoManager.record(stateAfterMove1)

            // Undo 2 -> stateAfterMove1
            val u1 = undoManager.undo(stateAfterMove2)!!
            // Undo 1 -> initialState
            val u2 = undoManager.undo(u1)!!

            // Redo 1 -> stateAfterMove1
            val r1 = undoManager.redo(u2)
            assertEquals(stateAfterMove1, r1)

            // Redo 2 -> stateAfterMove2
            val r2 = undoManager.redo(r1!!)
            assertEquals(stateAfterMove2, r2)

            assertFalse(undoManager.canRedo)
            assertEquals(2, undoManager.undoCount)
        }

        @Test
        @DisplayName("Recording new move invalidates and clears redo stack")
        fun `recording new move invalidates redo stack`() {
            val undoManager = UndoManager()
            undoManager.record(initialState)

            // Undo back to initialState
            undoManager.undo(stateAfterMove1)
            assertTrue(undoManager.canRedo)

            // Player branches to a new move instead of redoing
            val alternateMove = BoardState(score = 5, movesCount = 1, waste = listOf(twoHearts))
            undoManager.record(initialState)

            assertFalse(undoManager.canRedo)
            assertEquals(0, undoManager.redoCount)
            assertEquals(1, undoManager.undoCount)
        }
    }

    @Nested
    @DisplayName("Clear and history management")
    inner class ClearAndHistoryTests {

        @Test
        @DisplayName("clear resets both undo and redo stacks")
        fun `clear resets both stacks`() {
            val undoManager = UndoManager()
            undoManager.record(initialState)
            undoManager.undo(stateAfterMove1)

            undoManager.clear()

            assertFalse(undoManager.canUndo)
            assertFalse(undoManager.canRedo)
            assertEquals(0, undoManager.undoCount)
            assertEquals(0, undoManager.redoCount)
        }

        @Test
        @DisplayName("getUndoHistory and getRedoHistory return immutable snapshots")
        fun `getHistory returns correct snapshots`() {
            val undoManager = UndoManager()
            undoManager.record(initialState)
            undoManager.record(stateAfterMove1)

            val history = undoManager.getUndoHistory()
            assertEquals(listOf(initialState, stateAfterMove1), history)
        }

        @Test
        @DisplayName("restoreHistory correctly restores undo and redo stacks")
        fun `restoreHistory correctly restores stacks`() {
            val undoManager = UndoManager()

            undoManager.restoreHistory(
                undoHistory = listOf(initialState, stateAfterMove1),
                redoHistory = listOf(stateAfterMove2)
            )

            assertEquals(2, undoManager.undoCount)
            assertEquals(1, undoManager.redoCount)
            assertTrue(undoManager.canUndo)
            assertTrue(undoManager.canRedo)
            assertEquals(stateAfterMove1, undoManager.peekUndo())
            assertEquals(stateAfterMove2, undoManager.peekRedo())
        }

        @Test
        @DisplayName("maxHistorySize drops oldest entries when exceeded")
        fun `maxHistorySize drops oldest entries`() {
            val boundedUndoManager = UndoManager(maxHistorySize = 2)

            val s0 = BoardState(movesCount = 0)
            val s1 = BoardState(movesCount = 1)
            val s2 = BoardState(movesCount = 2)
            val s3 = BoardState(movesCount = 3)

            boundedUndoManager.record(s0)
            boundedUndoManager.record(s1)
            boundedUndoManager.record(s2)

            // s0 should have been dropped, remaining: s1, s2
            assertEquals(2, boundedUndoManager.undoCount)
            assertEquals(listOf(s1, s2), boundedUndoManager.getUndoHistory())

            boundedUndoManager.record(s3)
            // s1 dropped, remaining: s2, s3
            assertEquals(listOf(s2, s3), boundedUndoManager.getUndoHistory())
        }
    }
}
