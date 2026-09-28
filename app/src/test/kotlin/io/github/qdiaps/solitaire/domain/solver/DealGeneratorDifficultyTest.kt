package io.github.qdiaps.solitaire.domain.solver

import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import io.github.qdiaps.solitaire.domain.rules.DealDifficulty
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DealGeneratorDifficultyTest {

    private fun createTestBoard(id: String): BoardState {
        val card = Card(Suit.SPADES, Rank.ACE, isFaceUp = true, id = id)
        return BoardState(stock = listOf(card))
    }

    @Nested
    @DisplayName("Difficulty-Specific Deal Provisioning")
    inner class DifficultyBufferingTests {

        @Test
        @DisplayName("getSolvableDeal with EASY retrieves from easy buffer")
        fun `getSolvableDeal with EASY retrieves from easy buffer`() = runTest {
            val easyBoard = createTestBoard("easy_1")
            val mediumBoard = createTestBoard("medium_1")

            val deals = listOf(easyBoard, mediumBoard)
            var index = 0

            val generator = DealGenerator(
                scope = backgroundScope,
                bufferCapacity = 2,
                dealProvider = { deals[(index++) % deals.size] },
                solvabilityChecker = { board, _ ->
                    SolvabilityResult.Solvable(
                        moves = emptyList(),
                        path = listOf(board),
                        statesEvaluated = 1,
                        durationMs = 5L
                    )
                },
                difficultyClassifier = { board, _ ->
                    if (board == easyBoard) DealDifficulty.EASY else DealDifficulty.MEDIUM
                }
            )

            val deal = generator.getSolvableDeal(DealDifficulty.EASY)
            assertEquals(easyBoard, deal)
        }

        @Test
        @DisplayName("getSolvableDeal with MEDIUM retrieves from medium buffer")
        fun `getSolvableDeal with MEDIUM retrieves from medium buffer`() = runTest {
            val easyBoard = createTestBoard("easy_1")
            val mediumBoard = createTestBoard("medium_1")

            val deals = listOf(easyBoard, mediumBoard)
            var index = 0

            val generator = DealGenerator(
                scope = backgroundScope,
                bufferCapacity = 2,
                dealProvider = { deals[(index++) % deals.size] },
                solvabilityChecker = { board, _ ->
                    SolvabilityResult.Solvable(
                        moves = emptyList(),
                        path = listOf(board),
                        statesEvaluated = 1,
                        durationMs = 5L
                    )
                },
                difficultyClassifier = { board, _ ->
                    if (board == easyBoard) DealDifficulty.EASY else DealDifficulty.MEDIUM
                }
            )

            val deal = generator.getSolvableDeal(DealDifficulty.MEDIUM)
            assertEquals(mediumBoard, deal)
        }

        @Test
        @DisplayName("getSolvableDeal with RANDOM immediately returns unverified dealProvider")
        fun `getSolvableDeal with RANDOM returns raw deal immediately`() = runTest {
            val randomBoard = createTestBoard("random_board")

            val generator = DealGenerator(
                scope = backgroundScope,
                bufferCapacity = 2,
                dealProvider = { randomBoard },
                solvabilityChecker = { _, _ ->
                    SolvabilityResult.Unsolvable(statesEvaluated = 1, durationMs = 1L)
                }
            )

            // Even if solvability checker returns Unsolvable, RANDOM mode returns the deal immediately!
            val deal = generator.getSolvableDeal(DealDifficulty.RANDOM)
            assertEquals(randomBoard, deal)
        }

        @Test
        @DisplayName("getSolvableDeal without arguments defaults to EASY")
        fun `getSolvableDeal without arguments defaults to EASY`() = runTest {
            val easyBoard = createTestBoard("easy_board")

            val generator = DealGenerator(
                scope = backgroundScope,
                bufferCapacity = 2,
                dealProvider = { easyBoard },
                solvabilityChecker = { board, _ ->
                    SolvabilityResult.Solvable(
                        moves = emptyList(),
                        path = listOf(board),
                        statesEvaluated = 1,
                        durationMs = 1L
                    )
                },
                difficultyClassifier = { _, _ -> DealDifficulty.EASY }
            )

            val deal = generator.getSolvableDeal()
            assertEquals(easyBoard, deal)
        }

        @Test
        @DisplayName("stop terminates all difficulty generation jobs")
        fun `stop terminates all difficulty generation jobs`() = runTest {
            val generator = DealGenerator(
                scope = backgroundScope,
                bufferCapacity = 2,
                dealProvider = { createTestBoard("any") },
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
    }
}
