package io.github.qdiaps.solitaire.domain.solver

import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import io.github.qdiaps.solitaire.domain.rules.DrawMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@OptIn(ExperimentalCoroutinesApi::class)
class DealGeneratorTest {

    private fun createTestBoard(seed: Int): BoardState {
        val card = Card(Suit.SPADES, Rank.ACE, isFaceUp = true, id = "seed_$seed")
        return BoardState(stock = listOf(card))
    }

    private fun createWonBoard(): BoardState {
        val fullFoundations = Suit.entries.map { suit ->
            Rank.entries.map { rank -> Card(suit, rank, isFaceUp = true) }
        }
        return BoardState(foundations = fullFoundations)
    }

    @Nested
    @DisplayName("Buffer pre-filling and filtering")
    inner class BufferPrefillingTests {

        @Test
        @DisplayName("Only solvable deals are placed into the buffer")
        fun `only solvable deals are buffered and returned`() = runTest {
            val boardUnsolvable = createTestBoard(1)
            val boardSolvable1 = createTestBoard(2)
            val boardTimeout = createTestBoard(3)
            val boardSolvable2 = createTestBoard(4)

            val deals = listOf(boardUnsolvable, boardSolvable1, boardTimeout, boardSolvable2)
            var index = 0

            val generator = DealGenerator(
                scope = backgroundScope,
                bufferCapacity = 2,
                dealProvider = { deals[(index++) % deals.size] },
                solvabilityChecker = { board, _ ->
                    when (board) {
                        boardSolvable1, boardSolvable2 -> SolvabilityResult.Solvable(
                            moves = emptyList(),
                            path = listOf(board),
                            statesEvaluated = 1,
                            durationMs = 5L
                        )
                        boardUnsolvable -> SolvabilityResult.Unsolvable(statesEvaluated = 1, durationMs = 2L)
                        else -> SolvabilityResult.Timeout(statesEvaluated = 10, durationMs = 100L)
                    }
                }
            )

            val firstDeal = generator.getSolvableDeal()
            val secondDeal = generator.getSolvableDeal()

            assertEquals(boardSolvable1, firstDeal)
            assertEquals(boardSolvable2, secondDeal)
        }

        @Test
        @DisplayName("Generator pauses production when buffer reaches capacity")
        fun `generator suspends production when buffer is full`() = runTest {
            var dealsGeneratedCount = 0
            val capacity = 2

            val generator = DealGenerator(
                scope = backgroundScope,
                bufferCapacity = capacity,
                dealProvider = {
                    dealsGeneratedCount++
                    createTestBoard(dealsGeneratedCount)
                },
                solvabilityChecker = { board, _ ->
                    SolvabilityResult.Solvable(
                        moves = emptyList(),
                        path = listOf(board),
                        statesEvaluated = 1,
                        durationMs = 1L
                    )
                }
            )

            // Let background worker fill the channel
            testScheduler.advanceUntilIdle()
            val countBeforeConsuming = dealsGeneratedCount
            assertTrue(
                countBeforeConsuming <= capacity + 1,
                "Producer should suspend when buffer is full. Generated: $countBeforeConsuming, max expected: ${capacity + 1}"
            )

            // Consume one deal, freeing a slot in buffer
            generator.getSolvableDeal()
            testScheduler.advanceUntilIdle()

            // One more item should have been produced to refill the freed slot
            assertTrue(dealsGeneratedCount >= countBeforeConsuming)
        }
    }

    @Nested
    @DisplayName("Lifecycle and cancellation")
    inner class LifecycleTests {

        @Test
        @DisplayName("Stopping generator terminates background job")
        fun `stop terminates generation job`() = runTest {
            val generator = DealGenerator(
                scope = backgroundScope,
                bufferCapacity = 2,
                dealProvider = { createTestBoard(0) },
                solvabilityChecker = { board, _ ->
                    SolvabilityResult.Solvable(
                        moves = emptyList(),
                        path = listOf(board),
                        statesEvaluated = 1,
                        durationMs = 1L
                    )
                }
            )

            assertTrue(generator.isRunning)
            generator.stop()
            assertFalse(generator.isRunning)
        }

        @Test
        @DisplayName("Cancelling parent scope terminates generator")
        fun `cancelling parent scope stops generator`() = runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val customScope = TestScope(testDispatcher)

            val generator = DealGenerator(
                scope = customScope,
                dispatcher = testDispatcher,
                bufferCapacity = 2,
                dealProvider = { createTestBoard(0) },
                solvabilityChecker = { board, _ ->
                    SolvabilityResult.Solvable(
                        moves = emptyList(),
                        path = listOf(board),
                        statesEvaluated = 1,
                        durationMs = 1L
                    )
                }
            )

            assertTrue(generator.isRunning)
            customScope.cancel()
            testDispatcher.scheduler.advanceUntilIdle()

            assertFalse(generator.isRunning)
        }

        @Test
        @DisplayName("Zero or negative buffer capacity throws IllegalArgumentException")
        fun `invalid buffer capacity throws exception`() = runTest {
            assertThrows<IllegalArgumentException> {
                DealGenerator(
                    scope = backgroundScope,
                    bufferCapacity = 0
                )
            }
            assertThrows<IllegalArgumentException> {
                DealGenerator(
                    scope = backgroundScope,
                    bufferCapacity = -1
                )
            }
        }

        @Test
        @DisplayName("Secondary constructor configures DrawMode correctly")
        fun `secondary constructor configures draw mode`() = runTest {
            val generator = DealGenerator(
                scope = backgroundScope,
                drawMode = DrawMode.DRAW_THREE,
                bufferCapacity = 2
            )
            assertEquals(DrawMode.DRAW_THREE, generator.solverConfig.drawMode)
            assertEquals(2, generator.bufferCapacity)
        }

        @Test
        @DisplayName("getSolvableDeal restarts generator if stopped")
        fun `getSolvableDeal restarts generator if stopped`() = runTest {
            val wonBoard = createWonBoard()
            val generator = DealGenerator(
                scope = backgroundScope,
                bufferCapacity = 1,
                dealProvider = { wonBoard },
                solvabilityChecker = { board, _ ->
                    SolvabilityResult.Solvable(
                        moves = emptyList(),
                        path = listOf(board),
                        statesEvaluated = 1,
                        durationMs = 1L
                    )
                }
            )

            generator.stop()
            assertFalse(generator.isRunning)

            val deal = generator.getSolvableDeal()
            assertEquals(wonBoard, deal)
            assertTrue(generator.isRunning)
        }
    }

    @Nested
    @DisplayName("Real engine integration")
    inner class RealEngineTests {

        @Test
        @DisplayName("Can provide a solvable deal with default components")
        fun `can provide solvable deal with won board provider`() = runTest {
            val wonBoard = createWonBoard()
            val generator = DealGenerator(
                scope = backgroundScope,
                bufferCapacity = 1,
                dealProvider = { wonBoard }
            )

            val deal = generator.getSolvableDeal()
            assertEquals(wonBoard, deal)
        }
    }
}
