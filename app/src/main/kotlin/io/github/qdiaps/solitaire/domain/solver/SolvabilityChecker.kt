package io.github.qdiaps.solitaire.domain.solver

import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Move
import io.github.qdiaps.solitaire.domain.rules.DrawMode
import io.github.qdiaps.solitaire.domain.rules.KlondikeRules
import java.util.PriorityQueue

/**
 * Result returned by [SolvabilityChecker] upon analyzing a board state.
 */
sealed interface SolvabilityResult {
    val statesEvaluated: Int
    val durationMs: Long

    /**
     * The board position is solvable.
     *
     * @property moves Sequence of legal [Move]s that transforms the initial state into a won state.
     * @property path Complete sequence of resulting [BoardState] snapshots from initial to won state.
     */
    data class Solvable(
        val moves: List<Move>,
        val path: List<BoardState>,
        override val statesEvaluated: Int,
        override val durationMs: Long
    ) : SolvabilityResult

    /**
     * The board position has no reachable winning states (all paths exhausted).
     */
    data class Unsolvable(
        override val statesEvaluated: Int,
        override val durationMs: Long
    ) : SolvabilityResult

    /**
     * Search exceeded the configured [SolverConfig.timeoutMs] or [SolverConfig.maxStates] limits.
     */
    data class Timeout(
        override val statesEvaluated: Int,
        override val durationMs: Long
    ) : SolvabilityResult
}

/**
 * Configuration options for [SolvabilityChecker].
 *
 * @property maxStates Maximum number of state evaluations before terminating with [SolvabilityResult.Timeout].
 * @property timeoutMs Maximum duration in milliseconds before terminating search.
 * @property drawMode The draw rule applied when drawing or recycling the stock.
 * @property autoCollapseSafePromotions When true, provably safe foundation promotions are collapsed
 * greedily without branching, collapsing search space size by orders of magnitude.
 */
data class SolverConfig(
    val maxStates: Int = 10_000,
    val timeoutMs: Long = 1_500L,
    val drawMode: DrawMode = DrawMode.DRAW_ONE,
    val autoCollapseSafePromotions: Boolean = true
)

/**
 * High-performance A* / Heuristic Solvability Search Engine for Klondike Solitaire.
 *
 * Uses [SolverStateKey] for compact, symmetric visited set pruning and [SafePromotion] for
 * greedy branch collapsing.
 */
object SolvabilityChecker {

    private class SearchNode(
        val state: BoardState,
        val key: SolverStateKey,
        val parent: SearchNode?,
        val move: Move?,
        val g: Int,
        val h: Int,
        val f: Int = g + h
    ) : Comparable<SearchNode> {
        override fun compareTo(other: SearchNode): Int {
            val fCmp = this.f.compareTo(other.f)
            if (fCmp != 0) return fCmp
            return this.h.compareTo(other.h)
        }
    }

    /**
     * Determines whether [initialState] is solvable under the given [config].
     *
     * @param initialState The starting board configuration.
     * @param config Search limits and heuristics settings.
     * @return [SolvabilityResult] indicating whether a winning sequence was found.
     */
    fun checkSolvability(
        initialState: BoardState,
        config: SolverConfig = SolverConfig()
    ): SolvabilityResult {
        val startTime = System.currentTimeMillis()
        var statesEvaluated = 0

        // 1. Check if already won
        if (KlondikeRules.isGameWon(initialState)) {
            return SolvabilityResult.Solvable(
                moves = emptyList(),
                path = listOf(initialState),
                statesEvaluated = 0,
                durationMs = System.currentTimeMillis() - startTime
            )
        }

        // 2. Prepare root node
        val rootNode = SearchNode(
            state = initialState,
            key = SolverStateKey.from(initialState),
            parent = null,
            move = null,
            g = 0,
            h = calculateHeuristic(initialState)
        )

        val startNode = if (config.autoCollapseSafePromotions) {
            collapseSafePromotions(rootNode)
        } else {
            rootNode
        }

        if (KlondikeRules.isGameWon(startNode.state)) {
            return buildSolvableResult(startNode, 1, startTime)
        }

        val openSet = PriorityQueue<SearchNode>()
        val visited = HashSet<SolverStateKey>()

        openSet.add(startNode)
        visited.add(startNode.key)

        // 3. Main A* search loop
        while (openSet.isNotEmpty()) {
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed >= config.timeoutMs) {
                return SolvabilityResult.Timeout(statesEvaluated, elapsed)
            }
            if (statesEvaluated >= config.maxStates) {
                return SolvabilityResult.Timeout(statesEvaluated, elapsed)
            }

            val current = openSet.poll() ?: break
            statesEvaluated++

            if (KlondikeRules.isGameWon(current.state)) {
                return buildSolvableResult(current, statesEvaluated, startTime)
            }

            val transitions = SolverMoveGenerator.generateTransitions(
                state = current.state,
                drawMode = config.drawMode
            )

            for (transition in transitions) {
                val childNode = SearchNode(
                    state = transition.state,
                    key = SolverStateKey.from(transition.state),
                    parent = current,
                    move = transition.move,
                    g = current.g + 1,
                    h = calculateHeuristic(transition.state)
                )

                val effectiveNode = if (config.autoCollapseSafePromotions) {
                    collapseSafePromotions(childNode)
                } else {
                    childNode
                }

                if (KlondikeRules.isGameWon(effectiveNode.state)) {
                    return buildSolvableResult(effectiveNode, statesEvaluated + 1, startTime)
                }

                if (visited.add(effectiveNode.key)) {
                    openSet.add(effectiveNode)
                }
            }
        }

        val elapsed = System.currentTimeMillis() - startTime
        return SolvabilityResult.Unsolvable(statesEvaluated, elapsed)
    }

    /**
     * Calculates an admissible heuristic distance to the goal state.
     * Lower score indicates closer proximity to game victory.
     */
    private fun calculateHeuristic(state: BoardState): Int {
        val cardsInFoundation = state.foundations.sumOf { it.size }
        val cardsNotInFoundation = 52 - cardsInFoundation
        val faceDownCardsInTableau = state.tableau.sumOf { col -> col.count { !it.isFaceUp } }
        val stockAndWaste = state.stock.size + state.waste.size

        return cardsNotInFoundation + (2 * faceDownCardsInTableau) + stockAndWaste
    }

    /**
     * Collapses all provably safe promotions in sequence, returning the final collapsed node
     * with parent pointers preserving the exact move chain.
     */
    private fun collapseSafePromotions(initialNode: SearchNode): SearchNode {
        var currentNode = initialNode
        while (true) {
            val safeTransitions = SafePromotion.findSafeTransitions(currentNode.state)
            if (safeTransitions.isEmpty()) break
            val transition = safeTransitions.first()
            currentNode = SearchNode(
                state = transition.state,
                key = SolverStateKey.from(transition.state),
                parent = currentNode,
                move = transition.move,
                g = currentNode.g + 1,
                h = calculateHeuristic(transition.state)
            )
        }
        return currentNode
    }

    /**
     * Reconstructs the complete [SolvabilityResult.Solvable] solution by backtracking from the
     * winning [SearchNode].
     */
    private fun buildSolvableResult(
        winningNode: SearchNode,
        statesEvaluated: Int,
        startTime: Long
    ): SolvabilityResult.Solvable {
        val path = mutableListOf<BoardState>()
        val moves = mutableListOf<Move>()

        var curr: SearchNode? = winningNode
        while (curr != null) {
            path.add(curr.state)
            curr.move?.let { moves.add(it) }
            curr = curr.parent
        }

        path.reverse()
        moves.reverse()

        val durationMs = System.currentTimeMillis() - startTime
        return SolvabilityResult.Solvable(
            moves = moves,
            path = path,
            statesEvaluated = statesEvaluated,
            durationMs = durationMs
        )
    }
}
