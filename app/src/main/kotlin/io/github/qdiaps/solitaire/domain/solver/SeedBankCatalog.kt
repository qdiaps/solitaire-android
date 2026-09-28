package io.github.qdiaps.solitaire.domain.solver

import io.github.qdiaps.solitaire.domain.rules.DealDifficulty
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Immutable catalog of pre-verified solvable Klondike seeds segmented by [DealDifficulty].
 *
 * @property version Catalog schema version.
 * @property easySeeds Pre-verified seeds meeting [DealDifficulty.EASY] criteria.
 * @property mediumSeeds Pre-verified seeds meeting [DealDifficulty.MEDIUM] criteria.
 */
@Serializable
data class SeedBankCatalog(
    val version: Int = 1,
    val easySeeds: List<Long>,
    val mediumSeeds: List<Long>
) {
    /**
     * Returns seeds for the specified [difficulty].
     * Returns an empty list for [DealDifficulty.RANDOM] as random deals are not pre-seeded.
     */
    fun getSeedsFor(difficulty: DealDifficulty): List<Long> = when (difficulty) {
        DealDifficulty.EASY -> easySeeds
        DealDifficulty.MEDIUM -> mediumSeeds
        DealDifficulty.RANDOM -> emptyList()
    }

    /**
     * Total number of pre-seeded deals contained in the catalog.
     */
    val totalSeedsCount: Int
        get() = easySeeds.size + mediumSeeds.size
}

/**
 * JSON serialization and parsing utility for [SeedBankCatalog].
 */
object SeedBankParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    /**
     * Parses a JSON string into a [SeedBankCatalog].
     */
    fun parse(jsonString: String): SeedBankCatalog =
        json.decodeFromString(jsonString)

    /**
     * Serializes a [SeedBankCatalog] into a formatted JSON string.
     */
    fun serialize(catalog: SeedBankCatalog): String =
        json.encodeToString(catalog)
}
