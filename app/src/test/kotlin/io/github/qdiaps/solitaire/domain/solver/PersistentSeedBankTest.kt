package io.github.qdiaps.solitaire.domain.solver

import io.github.qdiaps.solitaire.domain.rules.DealDifficulty
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PersistentSeedBankTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private fun sampleCatalog(): SeedBankCatalog = SeedBankCatalog(
        version = 1,
        easySeeds = (1001L..1100L).toList(), // 100 seeds
        mediumSeeds = (2001L..2100L).toList() // 100 seeds
    )

    @Nested
    @DisplayName("Cold Start & Initialization")
    inner class InitializationTests {

        @Test
        @DisplayName("Initializes from default catalog when storage is empty")
        fun `initializes from default catalog when storage is empty`() = testScope.runTest {
            val storage = InMemorySeedBankStorage()
            val bank = PersistentSeedBank(
                storage = storage,
                defaultCatalogProvider = { sampleCatalog() },
                scope = testScope
            )

            assertFalse(bank.isInitialized)
            bank.initialize()
            assertTrue(bank.isInitialized)

            assertEquals(100, bank.getAvailableCount(DealDifficulty.EASY))
            assertEquals(100, bank.getAvailableCount(DealDifficulty.MEDIUM))
            assertTrue(bank.state.value.playedSeeds.isEmpty())

            // Storage should now be populated
            val saved = storage.load()
            assertNotNull(saved)
            assertEquals(100, saved!!.easySeeds.size)
            assertEquals(100, saved.mediumSeeds.size)
        }

        @Test
        @DisplayName("Restores existing state from storage on cold start")
        fun `restores existing state from storage on cold start`() = testScope.runTest {
            val existingState = SeedBankState(
                easySeeds = listOf(10L, 20L, 30L),
                mediumSeeds = listOf(50L, 60L),
                playedSeeds = setOf(1L, 2L)
            )
            val storage = InMemorySeedBankStorage(initialState = existingState)
            val bank = PersistentSeedBank(
                storage = storage,
                defaultCatalogProvider = { sampleCatalog() },
                scope = testScope
            )

            bank.initialize()

            assertEquals(3, bank.getAvailableCount(DealDifficulty.EASY))
            assertEquals(2, bank.getAvailableCount(DealDifficulty.MEDIUM))
            assertEquals(setOf(1L, 2L), bank.state.value.playedSeeds)
        }
    }

    @Nested
    @DisplayName("Seed Consumption & Rotation")
    inner class ConsumptionTests {

        @Test
        @DisplayName("consumeSeed picks random seed without replacement and adds to played history")
        fun `consumeSeed picks random seed without replacement and adds to played history`() = testScope.runTest {
            val storage = InMemorySeedBankStorage()
            val bank = PersistentSeedBank(
                storage = storage,
                defaultCatalogProvider = { sampleCatalog() },
                scope = testScope
            )
            bank.initialize()

            val consumed = bank.consumeSeed(DealDifficulty.EASY)
            assertNotNull(consumed)
            assertTrue(consumed in 1001L..1100L)
            assertEquals(99, bank.getAvailableCount(DealDifficulty.EASY))
            assertTrue(consumed in bank.state.value.playedSeeds)

            // Verify persistence
            testScope.advanceUntilIdle()
            val saved = storage.load()
            assertNotNull(saved)
            assertEquals(99, saved!!.easySeeds.size)
            assertFalse(consumed in saved.easySeeds)
            assertTrue(consumed in saved.playedSeeds)
        }

        @Test
        @DisplayName("Consuming all seeds returns null when bank is exhausted")
        fun `consuming all seeds returns null when bank is exhausted`() = testScope.runTest {
            val smallCatalog = SeedBankCatalog(
                version = 1,
                easySeeds = listOf(101L, 102L),
                mediumSeeds = emptyList()
            )
            val bank = PersistentSeedBank(
                storage = InMemorySeedBankStorage(),
                defaultCatalogProvider = { smallCatalog },
                scope = testScope
            )
            bank.initialize()

            val first = bank.consumeSeed(DealDifficulty.EASY)
            val second = bank.consumeSeed(DealDifficulty.EASY)
            val third = bank.consumeSeed(DealDifficulty.EASY)

            assertNotNull(first)
            assertNotNull(second)
            assertTrue(first != second)
            assertNull(third)
            assertEquals(0, bank.getAvailableCount(DealDifficulty.EASY))
        }

        @Test
        @DisplayName("consumeSeed for RANDOM returns null")
        fun `consumeSeed for RANDOM returns null`() = testScope.runTest {
            val bank = PersistentSeedBank(
                storage = InMemorySeedBankStorage(),
                defaultCatalogProvider = { sampleCatalog() },
                scope = testScope
            )
            bank.initialize()

            assertNull(bank.consumeSeed(DealDifficulty.RANDOM))
            assertEquals(100, bank.getAvailableCount(DealDifficulty.EASY))
        }
    }

    @Nested
    @DisplayName("Replenishment & Adding Seeds")
    inner class ReplenishmentTests {

        @Test
        @DisplayName("addSeed adds unique seed to bank and ignores duplicates and already played seeds")
        fun `addSeed adds unique seed and ignores duplicates`() = testScope.runTest {
            val bank = PersistentSeedBank(
                storage = InMemorySeedBankStorage(),
                defaultCatalogProvider = {
                    SeedBankCatalog(easySeeds = listOf(100L), mediumSeeds = emptyList())
                },
                scope = testScope
            )
            bank.initialize()

            // 1. Add fresh seed
            val added = bank.addSeed(DealDifficulty.EASY, 200L)
            assertTrue(added)
            assertEquals(2, bank.getAvailableCount(DealDifficulty.EASY))

            // 2. Duplicate in active bank
            val duplicateInBank = bank.addSeed(DealDifficulty.EASY, 200L)
            assertFalse(duplicateInBank)
            assertEquals(2, bank.getAvailableCount(DealDifficulty.EASY))

            // 3. Consume 100L and try to re-add it
            val consumed = bank.consumeSeed(DealDifficulty.EASY)
            assertNotNull(consumed)
            val reAddPlayed = bank.addSeed(DealDifficulty.EASY, consumed!!)
            assertFalse(reAddPlayed)
        }
    }

    @Nested
    @DisplayName("Stress Flush / Dev Action")
    inner class FlushTests {

        @Test
        @DisplayName("flush removes specified percentage of available seeds")
        fun `flush removes specified percentage of available seeds`() = testScope.runTest {
            val bank = PersistentSeedBank(
                storage = InMemorySeedBankStorage(),
                defaultCatalogProvider = { sampleCatalog() }, // 100 Easy, 100 Medium
                scope = testScope
            )
            bank.initialize()

            val removedCount = bank.flush(0.9f) // 90% flush
            assertEquals(180, removedCount)
            assertEquals(10, bank.getAvailableCount(DealDifficulty.EASY))
            assertEquals(10, bank.getAvailableCount(DealDifficulty.MEDIUM))
        }
    }
}
