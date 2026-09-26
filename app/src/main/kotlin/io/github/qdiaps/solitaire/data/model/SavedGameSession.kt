package io.github.qdiaps.solitaire.data.model

import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.rules.DrawMode
import kotlinx.serialization.Serializable

/**
 * Serialized snapshot representing an in-progress Solitaire gameplay session.
 *
 * Preserves the exact playing board state, timer progress, active draw mode,
 * and undo history stack across app restarts and process lifecycles.
 *
 * @property boardState The active [BoardState] snapshot (cards in stock, waste, foundations, and tableau).
 * @property elapsedTimeSeconds Total elapsed playing time in seconds accumulated so far.
 * @property drawMode Active stock draw mode ([DrawMode.DRAW_ONE] or [DrawMode.DRAW_THREE]).
 * @property undoHistory Sequential list of preceding [BoardState] snapshots allowing undo step rollback.
 * @property savedAtTimestamp Epoch millisecond timestamp recording when the snapshot was persisted.
 */
@Serializable
data class SavedGameSession(
    val boardState: BoardState,
    val elapsedTimeSeconds: Long = 0L,
    val drawMode: DrawMode = DrawMode.DRAW_ONE,
    val undoHistory: List<BoardState> = emptyList(),
    val savedAtTimestamp: Long = 0L,
    val hasMoved: Boolean = true
)
