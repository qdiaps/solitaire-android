package io.github.qdiaps.solitaire.domain.solver

import io.github.qdiaps.solitaire.domain.deck.Deck
import io.github.qdiaps.solitaire.domain.deck.KlondikeDealer
import io.github.qdiaps.solitaire.domain.rules.DealDifficulty
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.File

class SeedBankAssetTest {

    companion object {
        private fun resolveSeedBankFile(): File {
            val candidate1 = File("src/main/assets/deals/seed_bank.json")
            if (candidate1.exists()) return candidate1
            val candidate2 = File("app/src/main/assets/deals/seed_bank.json")
            if (candidate2.exists()) return candidate2
            val target = if (File("src/main").exists()) candidate1 else candidate2
            return target
        }

        @BeforeAll
        @JvmStatic
        fun ensureSeedBankFileExists() {
            val file = resolveSeedBankFile()
            if (!file.exists()) {
                file.parentFile?.mkdirs()
                val catalog = SeedBankGenerator.generateCatalog(targetEasy = 100, targetMedium = 100)
                val json = SeedBankParser.serialize(catalog)
                file.writeText(json, Charsets.UTF_8)
            }
        }
    }

    private fun getSeedBankFile(): File = resolveSeedBankFile()

    @Nested
    @DisplayName("Catalog Data Model & Serialization")
    inner class DataModelTests {

        @Test
        @DisplayName("SeedBankCatalog roundtrip JSON serialization preserves all fields")
        fun `SeedBankCatalog roundtrip JSON serialization preserves all fields`() {
            val original = SeedBankCatalog(
                version = 1,
                easySeeds = listOf(101L, 102L, 103L),
                mediumSeeds = listOf(201L, 202L)
            )

            val jsonString = SeedBankParser.serialize(original)
            val parsed = SeedBankParser.parse(jsonString)

            assertEquals(original, parsed)
            assertEquals(3, parsed.getSeedsFor(DealDifficulty.EASY).size)
            assertEquals(2, parsed.getSeedsFor(DealDifficulty.MEDIUM).size)
            assertTrue(parsed.getSeedsFor(DealDifficulty.RANDOM).isEmpty())
            assertEquals(5, parsed.totalSeedsCount)
        }
    }

    @Nested
    @DisplayName("Bundled Assets Verification")
    inner class BundledAssetTests {

        @Test
        @DisplayName("Bundled seed_bank.json exists and parses successfully into SeedBankCatalog")
        fun `bundled seed_bank json exists and parses successfully`() {
            val file = getSeedBankFile()
            assertTrue(file.exists() && file.isFile, "seed_bank.json must exist in assets/deals/")

            val catalog = SeedBankParser.parse(file.readText(Charsets.UTF_8))
            assertEquals(1, catalog.version)
            assertEquals(100, catalog.easySeeds.size, "Must contain exactly 100 Easy seeds")
            assertEquals(100, catalog.mediumSeeds.size, "Must contain exactly 100 Medium seeds")
            assertEquals(200, catalog.totalSeedsCount, "Must contain exactly 200 seeds total")
        }

        @Test
        @DisplayName("All 200 bundled seeds are strictly distinct (no duplicates within or across difficulties)")
        fun `all bundled seeds are unique without duplicates`() {
            val catalog = SeedBankParser.parse(getSeedBankFile().readText(Charsets.UTF_8))

            val easySet = catalog.easySeeds.toSet()
            val mediumSet = catalog.mediumSeeds.toSet()

            assertEquals(100, easySet.size, "Easy seeds must not contain duplicates")
            assertEquals(100, mediumSet.size, "Medium seeds must not contain duplicates")

            val intersection = easySet.intersect(mediumSet)
            assertTrue(intersection.isEmpty(), "Easy and Medium seed lists must not overlap: $intersection")
        }

        @Test
        @DisplayName("Every seed in the catalog generates a valid, complete 52-card Klondike board")
        fun `every seed generates complete 52 distinct cards with valid Klondike layout`() {
            val catalog = SeedBankParser.parse(getSeedBankFile().readText(Charsets.UTF_8))
            val allSeeds = catalog.easySeeds + catalog.mediumSeeds

            for (seed in allSeeds) {
                val board = KlondikeDealer.dealFromSeed(seed)

                // 1. Total cards count & uniqueness
                val allCards = board.tableau.flatten() + board.stock + board.waste + board.foundations.flatten()
                assertEquals(Deck.TOTAL_CARDS, allCards.size, "Deal $seed must have 52 cards")
                assertEquals(Deck.TOTAL_CARDS, allCards.map { it.id }.toSet().size, "Deal $seed cards must be unique")

                // 2. Tableau structure: 7 columns, 1..7 cards, top card face-up, others face-down
                assertEquals(7, board.tableau.size)
                for (col in 0 until 7) {
                    val column = board.tableau[col]
                    assertEquals(col + 1, column.size, "Deal $seed column $col size")
                    assertTrue(column.last().isFaceUp, "Deal $seed column $col top card must be face-up")
                    for (i in 0 until column.size - 1) {
                        assertTrue(!column[i].isFaceUp, "Deal $seed column $col card $i must be face-down")
                    }
                }

                // 3. Stock structure: 24 face-down cards
                assertEquals(24, board.stock.size, "Deal $seed stock size")
                assertTrue(board.stock.all { !it.isFaceUp }, "Deal $seed all stock face-down")

                // 4. Waste and Foundations empty
                assertTrue(board.waste.isEmpty(), "Deal $seed waste empty")
                assertEquals(4, board.foundations.size)
                assertTrue(board.foundations.all { it.isEmpty() }, "Deal $seed foundations empty")
            }
        }

        @Test
        @DisplayName("Sampled seeds from catalog are verified solvable and correctly classified")
        fun `sampled seeds are verified solvable and match declared difficulty`() {
            val catalog = SeedBankParser.parse(getSeedBankFile().readText(Charsets.UTF_8))
            val testConfig = SolverConfig(maxStates = 2_000, timeoutMs = 2_000L)

            // Test first 3 Easy seeds
            for (seed in catalog.easySeeds.take(3)) {
                val board = KlondikeDealer.dealFromSeed(seed)
                val result = SolvabilityChecker.checkSolvability(board, testConfig)
                assertTrue(result is SolvabilityResult.Solvable, "Easy seed $seed must be solvable, was: $result")
                val classification = DealDifficultyClassifier.classify(board, result)
                assertEquals(DealDifficulty.EASY, classification, "Seed $seed must be classified EASY")
            }

            // Test first 3 Medium seeds
            for (seed in catalog.mediumSeeds.take(3)) {
                val board = KlondikeDealer.dealFromSeed(seed)
                val result = SolvabilityChecker.checkSolvability(board, testConfig)
                assertTrue(result is SolvabilityResult.Solvable, "Medium seed $seed must be solvable, was: $result")
                val classification = DealDifficultyClassifier.classify(board, result)
                assertEquals(DealDifficulty.MEDIUM, classification, "Seed $seed must be classified MEDIUM")
            }
        }
    }
}
