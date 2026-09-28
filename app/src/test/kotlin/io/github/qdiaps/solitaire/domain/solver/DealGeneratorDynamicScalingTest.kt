package io.github.qdiaps.solitaire.domain.solver

import io.github.qdiaps.solitaire.domain.rules.DealDifficulty
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DealGeneratorDynamicScalingTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Nested
    @DisplayName("Dynamic Worker Scaling Calculations")
    inner class WorkerCountCalculations {

        @Test
        @DisplayName("Worker count is 0 when count >= 90")
        fun `worker count is 0 when bank has at least 90 seeds`() {
            assertEquals(0, DealGenerator.calculateWorkerCount(100, coreCount = 8))
            assertEquals(0, DealGenerator.calculateWorkerCount(95, coreCount = 8))
            assertEquals(0, DealGenerator.calculateWorkerCount(90, coreCount = 8))
        }

        @Test
        @DisplayName("Worker count is 1 when bank is between 50 and 89 seeds (routine replenishment)")
        fun `worker count is 1 when bank is between 50 and 89 seeds`() {
            assertEquals(1, DealGenerator.calculateWorkerCount(89, coreCount = 8))
            assertEquals(1, DealGenerator.calculateWorkerCount(70, coreCount = 8))
            assertEquals(1, DealGenerator.calculateWorkerCount(50, coreCount = 8))
        }

        @Test
        @DisplayName("Worker count scales dynamically up to cores limit when count is below 50")
        fun `worker count scales dynamically when bank drops below 50`() {
            // 8 cores -> max(2, min(7, 4)) = 4
            assertEquals(4, DealGenerator.calculateWorkerCount(49, coreCount = 8))
            assertEquals(4, DealGenerator.calculateWorkerCount(10, coreCount = 8))

            // 4 cores -> max(2, min(3, 4)) = 3
            assertEquals(3, DealGenerator.calculateWorkerCount(49, coreCount = 4))

            // 2 cores -> max(2, min(1, 4)) = 2
            assertEquals(2, DealGenerator.calculateWorkerCount(49, coreCount = 2))
        }
    }

    @Nested
    @DisplayName("Integration with PersistentSeedBank & Telemetry")
    inner class DynamicReplenishmentIntegration {

        @Test
        @DisplayName("getSolvableDeal consumes seed from bank instantly and updates telemetry")
        fun `getSolvableDeal consumes seed from bank instantly and updates telemetry`() = testScope.runTest {
            val bank = PersistentSeedBank(
                storage = InMemorySeedBankStorage(),
                defaultCatalogProvider = {
                    SeedBankCatalog(
                        easySeeds = (1001L..1100L).toList(), // 100 seeds
                        mediumSeeds = (2001L..2100L).toList()
                    )
                },
                scope = testScope
            )
            bank.initialize()

            val generator = DealGenerator(
                scope = testScope,
                dispatcher = testDispatcher,
                seedBank = bank,
                coreCountProvider = { 8 }
            )

            try {
                assertEquals(100, generator.debugStats.value.easyBankCount)
                assertEquals(100, generator.debugStats.value.mediumBankCount)
                assertEquals(0, generator.debugStats.value.activeWorkersCount)

                // Consume an easy deal
                val deal = generator.getSolvableDeal(DealDifficulty.EASY)
                assertNotNull(deal)
                assertEquals(99, generator.debugStats.value.easyBankCount)
                assertEquals(0, generator.debugStats.value.activeWorkersCount) // still >= 90, 0 workers
            } finally {
                generator.stop()
            }
        }

        @Test
        @DisplayName("Trimming below 90 triggers 1 replenishment worker until 100 is reached")
        fun `dropping below 90 triggers 1 replenishment worker until bank is full`() = testScope.runTest {
            // Start with 89 seeds (< 90)
            val bank = PersistentSeedBank(
                storage = InMemorySeedBankStorage(),
                defaultCatalogProvider = {
                    SeedBankCatalog(
                        easySeeds = (1001L..1089L).toList(), // 89 seeds
                        mediumSeeds = (2001L..2100L).toList()
                    )
                },
                scope = testScope
            )
            bank.initialize()

            var seedCounter = 5000L
            val generator = DealGenerator(
                scope = testScope,
                dispatcher = testDispatcher,
                seedBank = bank,
                coreCountProvider = { 8 },
                candidateSeedProvider = { seedCounter++ },
                solvabilityChecker = { _, _ ->
                    SolvabilityResult.Solvable(
                        moves = emptyList(),
                        path = emptyList(),
                        statesEvaluated = 100,
                        durationMs = 10L
                    )
                },
                difficultyClassifier = { _, _ -> DealDifficulty.EASY }
            )

            try {
                // Trigger check/replenish
                generator.checkAndReplenish(DealDifficulty.EASY)
                testScope.advanceUntilIdle()

                // Bank should have replenished back to 100!
                assertEquals(100, bank.getAvailableCount(DealDifficulty.EASY))
                assertEquals(100, generator.debugStats.value.easyBankCount)
                assertEquals(0, generator.debugStats.value.activeWorkersCount)
                assertTrue(generator.debugStats.value.totalSolvableFound >= 11)
            } finally {
                generator.stop()
            }
        }
    }
}
