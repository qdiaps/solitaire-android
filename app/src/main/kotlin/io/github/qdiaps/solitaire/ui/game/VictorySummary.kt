package io.github.qdiaps.solitaire.ui.game

import androidx.compose.runtime.Immutable
import io.github.qdiaps.solitaire.data.model.GameStats

/**
 * Immutable victory celebration metrics and record breakthrough highlights.
 *
 * @property timeSeconds Total elapsed game time in seconds.
 * @property movesCount Total number of moves executed.
 * @property score Final game score.
 * @property isNewBestTime Whether this game set a new personal fastest completion time.
 * @property isNewFewestMoves Whether this game set a new personal fewest moves record.
 * @property isNewHighScore Whether this game set a new personal high score.
 * @property isNewBestStreak Whether this win extended the winning streak to a new personal best.
 */
@Immutable
data class VictorySummary(
    val timeSeconds: Int,
    val movesCount: Int,
    val score: Int,
    val isNewBestTime: Boolean = false,
    val isNewFewestMoves: Boolean = false,
    val isNewHighScore: Boolean = false,
    val isNewBestStreak: Boolean = false
) {
    /**
     * Whether at least one personal lifetime record was achieved in this victory.
     */
    val hasAnyNewRecord: Boolean
        get() = isNewBestTime || isNewFewestMoves || isNewHighScore || isNewBestStreak

    companion object {
        /**
         * Calculates [VictorySummary] by comparing current victory metrics against [previousStats].
         *
         * @param timeSeconds Total elapsed time in seconds.
         * @param movesCount Total moves made.
         * @param score Final game score.
         * @param previousStats Historical lifetime statistics before this game victory was recorded.
         */
        fun calculate(
            timeSeconds: Int,
            movesCount: Int,
            score: Int,
            previousStats: GameStats
        ): VictorySummary {
            val isNewBestTime = previousStats.bestTimeSeconds == null || timeSeconds < previousStats.bestTimeSeconds
            val isNewFewestMoves = previousStats.fewestMoves == null || movesCount < previousStats.fewestMoves
            val isNewHighScore = score > previousStats.highScore
            val nextStreak = previousStats.currentStreak + 1
            val isNewBestStreak = nextStreak > previousStats.bestStreak

            return VictorySummary(
                timeSeconds = timeSeconds,
                movesCount = movesCount,
                score = score,
                isNewBestTime = isNewBestTime,
                isNewFewestMoves = isNewFewestMoves,
                isNewHighScore = isNewHighScore,
                isNewBestStreak = isNewBestStreak
            )
        }
    }
}
