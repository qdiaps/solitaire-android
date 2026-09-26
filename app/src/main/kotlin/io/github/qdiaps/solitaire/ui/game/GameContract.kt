package io.github.qdiaps.solitaire.ui.game

import androidx.compose.runtime.Immutable
import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.CardLocation
import io.github.qdiaps.solitaire.domain.rules.AutoCompleteMove
import io.github.qdiaps.solitaire.domain.rules.DrawMode
import io.github.qdiaps.solitaire.domain.rules.Hint
import io.github.qdiaps.solitaire.ui.theme.FeltTheme

/**
 * Immutable UI state model for the Solitaire game screen.
 *
 * @property boardState Current snapshot of card piles, score, and moves count.
 * @property isGameWon Whether all 52 cards have been placed onto the 4 foundation piles.
 * @property isDeadlocked Whether the board has reached an unplayable state with no legal productive moves.
 * @property canUndo Whether at least one prior move snapshot is available in the undo stack.
 * @property elapsedTimeSeconds Duration of active play time in seconds.
 * @property feltTheme Active cloth surface theme for the card table.
 * @property isLeftHanded Whether the top row layout is mirrored for left-handed ergonomics.
 * @property activeHint Suggested move currently displayed to the player, or null if none.
 * @property isLoading Whether background deal generation or solver calculations are currently running.
 * @property isAutoCompleteAvailable Whether all remaining face-down cards are revealed and can safely auto-complete.
 */
@Immutable
data class GameUiState(
    val boardState: BoardState = BoardState(),
    val isGameWon: Boolean = false,
    val isDeadlocked: Boolean = false,
    val canUndo: Boolean = false,
    val elapsedTimeSeconds: Long = 0L,
    val feltTheme: FeltTheme = FeltTheme.CLASSIC_GREEN,
    val isLeftHanded: Boolean = false,
    val activeHint: Hint? = null,
    val isLoading: Boolean = false,
    val isAutoCompleteAvailable: Boolean = false,
    val isAutoCompleting: Boolean = false,
    val gameSessionId: Long = 1L,
    val drawMode: DrawMode = DrawMode.DRAW_ONE
) {
    /**
     * Cards that should be highlighted on the board (e.g., all cards in the moving stack from [activeHint]).
     */
    val highlightedCards: List<Card>
        get() = if (hintSourceLocation is CardLocation.Stock) emptyList() else activeHint?.cards.orEmpty()

    /**
     * Card that should be highlighted on the board (e.g., source card from [activeHint]).
     */
    val highlightedCard: Card?
        get() = if (hintSourceLocation is CardLocation.Stock) null else activeHint?.cards?.firstOrNull()

    /**
     * Source card location of the active hint, or null if none.
     */
    val hintSourceLocation: CardLocation?
        get() = activeHint?.from

    /**
     * Destination slot location of the active hint, or null if none.
     */
    val hintTargetLocation: CardLocation?
        get() = activeHint?.to

    /**
     * Indicates whether a hint is currently active and highlighted.
     */
    val isHintActive: Boolean
        get() = activeHint != null
}

/**
 * Represents player actions and UI events dispatched to the ViewModel.
 */
sealed interface GameIntent {
    /**
     * Draw the next card or triplet from the stock pile into the waste pile.
     */
    data object DrawStockCard : GameIntent

    /**
     * Recycle the waste pile back into the stock pile when stock is exhausted.
     */
    data object RecycleStock : GameIntent

    /**
     * Player tapped a card at a specific location, triggering smart-tap auto-move resolution.
     */
    data class OnCardTapped(val card: Card, val location: CardLocation) : GameIntent

    /**
     * Player completed a drag-and-drop gesture, releasing cards onto a target pile location.
     */
    data class OnCardDropped(
        val cards: List<Card>,
        val source: CardLocation,
        val target: CardLocation
    ) : GameIntent

    /**
     * Revert the most recent game action.
     */
    data object UndoMove : GameIntent

    /**
     * Request an optimal or productive move suggestion from the hint resolver engine.
     */
    data object RequestHint : GameIntent

    /**
     * Dismiss active hint highlight.
     */
    data object DismissHint : GameIntent

    /**
     * Automatically cascade remaining cards to foundation piles when all cards are face up.
     */
    data object AutoComplete : GameIntent

    /**
     * Applies a single animated auto-complete foundation promotion step.
     */
    data class ApplyAutoCompleteMove(val move: AutoCompleteMove) : GameIntent

    /**
     * Deal a fresh solvable game board.
     */
    data object StartNewGame : GameIntent

    /**
     * Restart current game board from its initial dealt state.
     */
    data object RestartGame : GameIntent

    /**
     * Toggle left-handed top row layout orientation.
     */
    data object ToggleLeftHanded : GameIntent

    /**
     * Switch felt table surface theme.
     */
    data class SelectFeltTheme(val theme: FeltTheme) : GameIntent

    /**
     * Skip victory cascade animation and show final win summary dialog.
     */
    data object SkipWinAnimation : GameIntent
}

/**
 * Single-shot UI side-effects dispatched by ViewModel to the presentation layer.
 */
sealed interface GameEvent {
    /**
     * Trigger light haptic click feedback (e.g. card pickup or tap).
     */
    data object PlayHapticTick : GameEvent

    /**
     * Trigger strong haptic snap feedback (e.g. card dropped onto legal foundation/tableau slot).
     */
    data object PlayHapticSnap : GameEvent

    /**
     * Trigger card deal riffle sound effect when a new game starts or restarts.
     */
    data object PlayDealSound : GameEvent

    /**
     * Display a transient text message or snackbar alert.
     */
    data class ShowMessage(val message: String) : GameEvent

    /**
     * Trigger full-screen victory cascade animation celebration.
     */
    data object TriggerWinCelebration : GameEvent
}
