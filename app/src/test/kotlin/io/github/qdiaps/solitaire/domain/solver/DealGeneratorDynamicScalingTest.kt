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

        @Test
        @DisplayName("calculateDesiredWorkers continues routine refill up to 100 once activated")
        fun `calculateDesiredWorkers continues routine refill up to 100 once activated`() {
            // Idle: 90..99 returns 0
            assertEquals(0, DealGenerator.calculateDesiredWorkers(currentCount = 95, runningWorkers = 0, coreCount = 8))
            assertEquals(0, DealGenerator.calculateDesiredWorkers(currentCount = 90, runningWorkers = 0, coreCount = 8))

            // Wakes up on < 90
            assertEquals(1, DealGenerator.calculateDesiredWorkers(currentCount = 89, runningWorkers = 0, coreCount = 8))

            // Active worker keeps refilling all the way up to 100
            assertEquals(1, DealGenerator.calculateDesiredWorkers(currentCount = 90, runningWorkers = 1, coreCount = 8))
            assertEquals(1, DealGenerator.calculateDesiredWorkers(currentCount = 95, runningWorkers = 1, coreCount = 8))
            assertEquals(1, DealGenerator.calculateDesiredWorkers(currentCount = 99, runningWorkers = 1, coreCount = 8))

            // Stops at 100
            assertEquals(0, DealGenerator.calculateDesiredWorkers(currentCount = 100, runningWorkers = 1, coreCount = 8))
            assertEquals(0, DealGenerator.calculateDesiredWorkers(currentCount = 105, runningWorkers = 1, coreCount = 8))

            // Deep depletion scales down to 1 when reaching 50
            assertEquals(4, DealGenerator.calculateDesiredWorkers(currentCount = 49, runningWorkers = 4, coreCount = 8))
            assertEquals(1, DealGenerator.calculateDesiredWorkers(currentCount = 50, runningWorkers = 4, coreCount = 8))
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

        @Test
        @DisplayName("Cold start with depleted bank automatically triggers replenishment without manual call")
        fun `cold start with depleted bank automatically triggers replenishment without manual call`() = testScope.runTest {
            val bank = PersistentSeedBank(
                storage = InMemorySeedBankStorage(),
                defaultCatalogProvider = {
                    SeedBankCatalog(
                        easySeeds = (1001L..1080L).toList(),
                        mediumSeeds = (2001L..2100L).toList()
                    )
                },
                scope = testScope
            )
            bank.initialize()

            var seedCounter = 9000L
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
                        statesEvaluated = 50,
                        durationMs = 5L
                    )
                },
                difficultyClassifier = { _, _ -> DealDifficulty.EASY }
            )

            try {
                testScope.advanceUntilIdle()

                assertEquals(100, bank.getAvailableCount(DealDifficulty.EASY))
                assertEquals(100, generator.debugStats.value.easyBankCount)
                assertEquals(0, generator.debugStats.value.activeWorkersCount)
            } finally {
                generator.stop()
            }
        }

        @Test
        @DisplayName("Cross-difficulty harvesting: Easy worker deposits Medium deals if Medium bank needs seeds")
        fun `cross difficulty harvesting deposits Medium deals if Medium bank needs seeds`() = testScope.runTest {
            val bank = PersistentSeedBank(
                storage = InMemorySeedBankStorage(),
                defaultCatalogProvider = {
                    SeedBankCatalog(
                        easySeeds = (1001L..1085L).toList(),
                        mediumSeeds = (2001L..2090L).toList()
                    )
                },
                scope = testScope
            )
            bank.initialize()

            var seedCounter = 9500L
            var classifyAsMedium = false
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
                        statesEvaluated = 50,
                        durationMs = 5L
                    )
                },
                difficultyClassifier = { _, _ ->
                    classifyAsMedium = !classifyAsMedium
                    if (classifyAsMedium) DealDifficulty.MEDIUM else DealDifficulty.EASY
                }
            )

            try {
                testScope.advanceUntilIdle()

                assertEquals(100, bank.getAvailableCount(DealDifficulty.EASY))
                assertEquals(100, bank.getAvailableCount(DealDifficulty.MEDIUM))
                assertEquals(100, generator.debugStats.value.easyBankCount)
                assertEquals(100, generator.debugStats.value.mediumBankCount)
                assertEquals(0, generator.debugStats.value.activeWorkersCount)
            } finally {
                generator.stop()
            }
        }

        @Test
        @DisplayName("Cross-difficulty harvesting: Does not overflow bank if other difficulty is already at 100")
        fun `cross difficulty harvesting does not overflow bank if other difficulty is at capacity`() = testScope.runTest {
            val bank = PersistentSeedBank(
                storage = InMemorySeedBankStorage(),
                defaultCatalogProvider = {
                    SeedBankCatalog(
                        easySeeds = (1001L..1085L).toList(),
                        mediumSeeds = (2001L..2100L).toList()
                    )
                },
                scope = testScope
            )
            bank.initialize()

            var seedCounter = 9900L
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
                        statesEvaluated = 50,
                        durationMs = 5L
                    )
                },
                difficultyClassifier = { _, _ ->
                    if (seedCounter % 2L == 0L) DealDifficulty.EASY else DealDifficulty.MEDIUM
                }
            )

            try {
                testScope.advanceUntilIdle()

                assertEquals(100, bank.getAvailableCount(DealDifficulty.EASY))
                assertEquals(100, bank.getAvailableCount(DealDifficulty.MEDIUM))
                assertEquals(0, generator.debugStats.value.activeWorkersCount)
            } finally {
                generator.stop()
            }
        }

        @Test
        @DisplayName("Workers record discovery log entries with workerId, seed, difficulty, and duration")
        fun `workers record discovery log entries with workerId seed difficulty and duration`() = testScope.runTest {
            val bank = PersistentSeedBank(
                storage = InMemorySeedBankStorage(),
                defaultCatalogProvider = {
                    SeedBankCatalog(
                        easySeeds = (1001L..1085L).toList(),
                        mediumSeeds = (2001L..2100L).toList()
                    )
                },
                scope = testScope
            )
            bank.initialize()

            var seedCounter = 7700L
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
                        statesEvaluated = 50,
                        durationMs = 25L
                    )
                },
                difficultyClassifier = { _, _ -> DealDifficulty.EASY }
            )

            try {
                testScope.advanceUntilIdle()

                val logs = generator.debugStats.value.recentWorkerLogs
                assertTrue(logs.isNotEmpty())
                val firstLog = logs.first()
                assertTrue(firstLog.workerId >= 1)
                assertEquals(DealDifficulty.EASY, firstLog.difficulty)
                assertTrue(firstLog.seed >= 7700L)
                assertTrue(firstLog.durationMs >= 0L)
                assertTrue(firstLog.timestampMs > 0L)
            } finally {
                generator.stop()
            }
        }
    }
}
