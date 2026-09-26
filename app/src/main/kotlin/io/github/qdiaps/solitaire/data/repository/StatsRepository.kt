package io.github.qdiaps.solitaire.data.repository

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import io.github.qdiaps.solitaire.data.local.DataStoreManager
import io.github.qdiaps.solitaire.data.model.GameStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Repository interface for managing and observing player game statistics and lifetime records.
 */
interface StatsRepository {
    /**
     * Reactive stream of current player statistics.
     */
    val statsFlow: Flow<GameStats>

    /**
     * Retrieves the current snapshot of player statistics.
     */
    suspend fun getStats(): GameStats

    /**
     * Records the start of a new game.
     * Increments [GameStats.gamesPlayed].
     * If a previous game was in progress and was never won (abandoned), resets [GameStats.currentStreak] to 0.
     * Marks [GameStats.hasGameInProgress] as true.
     */
    suspend fun recordGameStarted()

    /**
     * Records a game victory with the completion metrics.
     * Increments [GameStats.gamesWon] and [GameStats.currentStreak].
     * Updates [GameStats.bestStreak] if current streak exceeds it.
     * Updates [GameStats.bestTimeSeconds] if [timeSeconds] is lower than current best (or first win).
     * Updates [GameStats.fewestMoves] if [moves] is lower than current fewest (or first win).
     * Updates [GameStats.highScore] if [score] exceeds current high score.
     * Marks [GameStats.hasGameInProgress] as false.
     *
     * @param timeSeconds Total elapsed game time in seconds.
     * @param moves Total number of moves made in the game.
     * @param score Final game score.
     */
    suspend fun recordGameWon(timeSeconds: Int, moves: Int, score: Int)

    /**
     * Records an abandoned or forfeited game.
     * If a game was in progress, resets [GameStats.currentStreak] to 0
     * and sets [GameStats.hasGameInProgress] to false.
     */
    suspend fun recordGameAbandoned()

    /**
     * Resets all player statistics to initial empty state.
     */
    suspend fun resetStats()
}

/**
 * DataStore-backed implementation of [StatsRepository].
 */
class DataStoreStatsRepository(
    private val dataStoreManager: DataStoreManager
) : StatsRepository {

    override val statsFlow: Flow<GameStats> = dataStoreManager.data.map { preferences ->
        GameStats(
            gamesPlayed = preferences[KEY_GAMES_PLAYED] ?: 0,
            gamesWon = preferences[KEY_GAMES_WON] ?: 0,
            currentStreak = preferences[KEY_CURRENT_STREAK] ?: 0,
            bestStreak = preferences[KEY_BEST_STREAK] ?: 0,
            bestTimeSeconds = preferences[KEY_BEST_TIME_SECONDS],
            fewestMoves = preferences[KEY_FEWEST_MOVES],
            highScore = preferences[KEY_HIGH_SCORE] ?: 0,
            hasGameInProgress = preferences[KEY_HAS_GAME_IN_PROGRESS] ?: false
        )
    }.distinctUntilChanged()

    override suspend fun getStats(): GameStats = statsFlow.first()

    override suspend fun recordGameStarted() {
        dataStoreManager.edit { preferences ->
            val inProgress = preferences[KEY_HAS_GAME_IN_PROGRESS] ?: false
            if (inProgress) {
                preferences[KEY_CURRENT_STREAK] = 0
            }
            val played = (preferences[KEY_GAMES_PLAYED] ?: 0) + 1
            preferences[KEY_GAMES_PLAYED] = played
            preferences[KEY_HAS_GAME_IN_PROGRESS] = true
        }
    }

    override suspend fun recordGameWon(timeSeconds: Int, moves: Int, score: Int) {
        dataStoreManager.edit { preferences ->
            val won = (preferences[KEY_GAMES_WON] ?: 0) + 1
            val played = maxOf(preferences[KEY_GAMES_PLAYED] ?: 0, won)
            val currentStreak = (preferences[KEY_CURRENT_STREAK] ?: 0) + 1
            val bestStreak = maxOf(preferences[KEY_BEST_STREAK] ?: 0, currentStreak)

            val currentBestTime = preferences[KEY_BEST_TIME_SECONDS]
            val bestTime = if (currentBestTime == null || timeSeconds < currentBestTime) timeSeconds else currentBestTime

            val currentFewestMoves = preferences[KEY_FEWEST_MOVES]
            val fewestMoves = if (currentFewestMoves == null || moves < currentFewestMoves) moves else currentFewestMoves

            val currentHighScore = preferences[KEY_HIGH_SCORE] ?: 0
            val highScore = maxOf(currentHighScore, score)

            preferences[KEY_GAMES_PLAYED] = played
            preferences[KEY_GAMES_WON] = won
            preferences[KEY_CURRENT_STREAK] = currentStreak
            preferences[KEY_BEST_STREAK] = bestStreak
            preferences[KEY_BEST_TIME_SECONDS] = bestTime
            preferences[KEY_FEWEST_MOVES] = fewestMoves
            preferences[KEY_HIGH_SCORE] = highScore
            preferences[KEY_HAS_GAME_IN_PROGRESS] = false
        }
    }

    override suspend fun recordGameAbandoned() {
        dataStoreManager.edit { preferences ->
            val inProgress = preferences[KEY_HAS_GAME_IN_PROGRESS] ?: false
            if (inProgress) {
                preferences[KEY_CURRENT_STREAK] = 0
                preferences[KEY_HAS_GAME_IN_PROGRESS] = false
            }
        }
    }

    override suspend fun resetStats() {
        dataStoreManager.edit { preferences ->
            preferences.remove(KEY_GAMES_PLAYED)
            preferences.remove(KEY_GAMES_WON)
            preferences.remove(KEY_CURRENT_STREAK)
            preferences.remove(KEY_BEST_STREAK)
            preferences.remove(KEY_BEST_TIME_SECONDS)
            preferences.remove(KEY_FEWEST_MOVES)
            preferences.remove(KEY_HIGH_SCORE)
            preferences.remove(KEY_HAS_GAME_IN_PROGRESS)
        }
    }

    companion object {
        val KEY_GAMES_PLAYED = intPreferencesKey("stats_games_played")
        val KEY_GAMES_WON = intPreferencesKey("stats_games_won")
        val KEY_CURRENT_STREAK = intPreferencesKey("stats_current_streak")
        val KEY_BEST_STREAK = intPreferencesKey("stats_best_streak")
        val KEY_BEST_TIME_SECONDS = intPreferencesKey("stats_best_time_seconds")
        val KEY_FEWEST_MOVES = intPreferencesKey("stats_fewest_moves")
        val KEY_HIGH_SCORE = intPreferencesKey("stats_high_score")
        val KEY_HAS_GAME_IN_PROGRESS = booleanPreferencesKey("stats_has_game_in_progress")
    }
}
