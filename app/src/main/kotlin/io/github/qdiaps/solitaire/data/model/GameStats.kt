package io.github.qdiaps.solitaire.data.model

import androidx.compose.runtime.Immutable

/**
 * Immutable player statistics record tracking lifetime gameplay performance and records.
 *
 * @property gamesPlayed Total number of games started.
 * @property gamesWon Total number of games completed with a victory.
 * @property currentStreak Current consecutive winning streak.
 * @property bestStreak Highest consecutive winning streak achieved.
 * @property bestTimeSeconds Lowest time in seconds taken to complete a game, or `null` if no wins.
 * @property fewestMoves Lowest number of moves taken to complete a game, or `null` if no wins.
 * @property highScore Highest score achieved on any won game.
 * @property hasGameInProgress Whether a game is currently underway (used to track abandoned games / streak breaks).
 */
@Immutable
data class GameStats(
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val bestTimeSeconds: Int? = null,
    val fewestMoves: Int? = null,
    val highScore: Int = 0,
    val hasGameInProgress: Boolean = false
) {
    /**
     * Percentage of games won (0.0 to 100.0).
     */
    val winPercentage: Double
        get() = if (gamesPlayed == 0) 0.0 else (gamesWon.toDouble() / gamesPlayed.toDouble()) * 100.0

    /**
     * Integer rounded win rate percentage (0 to 100).
     */
    val winRatePercent: Int
        get() = kotlin.math.round(winPercentage).toInt()
}
