package io.github.qdiaps.solitaire.domain.solver

import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.CardLocation
import io.github.qdiaps.solitaire.domain.rules.DealDifficulty
import io.github.qdiaps.solitaire.domain.rules.DrawMode

/**
 * Pure Kotlin heuristic classifier evaluating Klondike Solitaire deals for human playability.
 *
 * Distinguishes between gentle [DealDifficulty.EASY] deals (high opening mobility and low A* search depth)
 * and [DealDifficulty.MEDIUM] deals requiring deeper exploration.
 */
object DealDifficultyClassifier {

    /** Default upper bound on A* states evaluated for a deal to qualify as Easy. */
    const val DEFAULT_MAX_EASY_STATES_EVALUATED: Int = 250

    /** Default minimum number of opening tableau-originating moves for Easy qualification. */
    const val DEFAULT_MIN_EASY_OPENING_MOVES: Int = 2

    /**
     * Classifies a solved board into [DealDifficulty.EASY] or [DealDifficulty.MEDIUM].
     * Returns null if [solvabilityResult] is unsolvable or timed out.
     */
    fun classify(
        initialState: BoardState,
        solvabilityResult: SolvabilityResult,
        drawMode: DrawMode = DrawMode.DRAW_ONE,
        maxEasyStates: Int = DEFAULT_MAX_EASY_STATES_EVALUATED,
        minOpeningMoves: Int = DEFAULT_MIN_EASY_OPENING_MOVES
    ): DealDifficulty? {
        if (solvabilityResult !is SolvabilityResult.Solvable) return null
        return if (isEasy(initialState, solvabilityResult, drawMode, maxEasyStates, minOpeningMoves)) {
            DealDifficulty.EASY
        } else {
            DealDifficulty.MEDIUM
        }
    }

    /**
     * Determines whether a solvable deal meets the criteria for [DealDifficulty.EASY].
     */
    fun isEasy(
        initialState: BoardState,
        solvabilityResult: SolvabilityResult.Solvable,
        drawMode: DrawMode = DrawMode.DRAW_ONE,
        maxEasyStates: Int = DEFAULT_MAX_EASY_STATES_EVALUATED,
        minOpeningMoves: Int = DEFAULT_MIN_EASY_OPENING_MOVES
    ): Boolean {
        if (solvabilityResult.statesEvaluated > maxEasyStates) return false
        if (initialState.tableau.all { it.isEmpty() }) return true
        val openingMoves = countOpeningTableauMoves(initialState, drawMode)
        return openingMoves >= minOpeningMoves
    }

    /**
     * Counts all legal non-redundant moves originating specifically from the tableau.
     * Excludes stock-draw moves to focus purely on tableau interactivity.
     */
    fun countOpeningTableauMoves(
        state: BoardState,
        drawMode: DrawMode = DrawMode.DRAW_ONE
    ): Int {
        val transitions = SolverMoveGenerator.generateTransitions(state, drawMode)
        return transitions.count { it.move.source is CardLocation.Tableau }
    }
}
