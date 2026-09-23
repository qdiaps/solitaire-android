package io.github.qdiaps.solitaire.domain.engine

import io.github.qdiaps.solitaire.domain.model.BoardState

/**
 * Manages undo and redo histories using LIFO snapshot stacks of [BoardState].
 *
 * Implements unlimited (or bounded by [maxHistorySize]) step-by-step state rollback
 * for Klondike Solitaire sessions per architecture and specification.
 *
 * @property maxHistorySize Maximum number of snapshots preserved in the undo stack.
 * Defaults to [DEFAULT_MAX_HISTORY].
 */
class UndoManager(
    val maxHistorySize: Int = DEFAULT_MAX_HISTORY
) {
    companion object {
        /** Default maximum undo history size (unbounded by default). */
        const val DEFAULT_MAX_HISTORY: Int = Int.MAX_VALUE
    }

    init {
        require(maxHistorySize > 0) { "maxHistorySize must be positive, got: $maxHistorySize" }
    }

    private val undoStack = ArrayDeque<BoardState>()
    private val redoStack = ArrayDeque<BoardState>()

    /** Whether an undo operation can currently be performed. */
    val canUndo: Boolean get() = undoStack.isNotEmpty()

    /** Whether a redo operation can currently be performed. */
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    /** The number of snapshots currently stored in the undo stack. */
    val undoCount: Int get() = undoStack.size

    /** The number of snapshots currently stored in the redo stack. */
    val redoCount: Int get() = redoStack.size

    /**
     * Records a [state] snapshot onto the undo stack prior to making a state-changing move.
     * Clears the redo stack. If the undo stack exceeds [maxHistorySize], the oldest snapshot is dropped.
     *
     * @param state The [BoardState] snapshot to record.
     */
    fun record(state: BoardState) {
        if (undoStack.size >= maxHistorySize) {
            undoStack.removeFirst()
        }
        undoStack.addLast(state)
        redoStack.clear()
    }

    /**
     * Pops the most recent snapshot from the undo stack, pushes [currentState] onto the redo stack,
     * and returns the restored [BoardState].
     *
     * @param currentState The board state prior to rolling back (to be preserved for redo).
     * @return The previous [BoardState], or `null` if the undo stack is empty.
     */
    fun undo(currentState: BoardState): BoardState? {
        if (!canUndo) return null
        val previousState = undoStack.removeLast()
        redoStack.addLast(currentState)
        return previousState
    }

    /**
     * Pops the most recent snapshot from the redo stack, pushes [currentState] onto the undo stack,
     * and returns the restored [BoardState].
     *
     * @param currentState The board state prior to redoing (to be preserved for undo).
     * @return The restored [BoardState], or `null` if the redo stack is empty.
     */
    fun redo(currentState: BoardState): BoardState? {
        if (!canRedo) return null
        val nextState = redoStack.removeLast()
        if (undoStack.size >= maxHistorySize) {
            undoStack.removeFirst()
        }
        undoStack.addLast(currentState)
        return nextState
    }

    /**
     * Peeks at the top snapshot of the undo stack without removing it, or returns `null` if empty.
     */
    fun peekUndo(): BoardState? = undoStack.lastOrNull()

    /**
     * Peeks at the top snapshot of the redo stack without removing it, or returns `null` if empty.
     */
    fun peekRedo(): BoardState? = redoStack.lastOrNull()

    /**
     * Returns an immutable snapshot list of all states currently in the undo stack (oldest to newest).
     */
    fun getUndoHistory(): List<BoardState> = undoStack.toList()

    /**
     * Returns an immutable snapshot list of all states currently in the redo stack (oldest to newest).
     */
    fun getRedoHistory(): List<BoardState> = redoStack.toList()

    /**
     * Restores undo and redo history from preserved snapshot collections (e.g. after deserialization).
     *
     * @param undoHistory Ordered list of undo snapshots (oldest to newest).
     * @param redoHistory Ordered list of redo snapshots (oldest to newest).
     */
    fun restoreHistory(
        undoHistory: List<BoardState>,
        redoHistory: List<BoardState> = emptyList()
    ) {
        clear()
        val truncatedUndo = if (undoHistory.size > maxHistorySize) {
            undoHistory.takeLast(maxHistorySize)
        } else {
            undoHistory
        }
        undoStack.addAll(truncatedUndo)
        redoStack.addAll(redoHistory)
    }

    /**
     * Clears all snapshots from both undo and redo stacks.
     */
    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
