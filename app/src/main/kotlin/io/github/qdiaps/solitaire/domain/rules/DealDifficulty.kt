package io.github.qdiaps.solitaire.domain.rules

import kotlinx.serialization.Serializable

/**
 * Deal difficulty setting determining how game layouts are generated.
 */
@Serializable
enum class DealDifficulty {
    /** 100% winnable deals with high opening mobility and gentle A* solution paths. */
    EASY,

    /** 100% winnable deals with standard A* heuristic solvability. */
    MEDIUM,

    /** Classic unverified random shuffle (authentic Windows Solitaire win rate ~30%). */
    RANDOM
}
