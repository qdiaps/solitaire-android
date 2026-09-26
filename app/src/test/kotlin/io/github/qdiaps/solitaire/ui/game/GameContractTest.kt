package io.github.qdiaps.solitaire.ui.game

import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.CardLocation
import io.github.qdiaps.solitaire.domain.model.Move
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import io.github.qdiaps.solitaire.domain.rules.AutoCompleteMove
import io.github.qdiaps.solitaire.domain.rules.DrawMode
import io.github.qdiaps.solitaire.domain.rules.Hint
import io.github.qdiaps.solitaire.domain.rules.HintPriority
import io.github.qdiaps.solitaire.ui.theme.CardBackStyle
import io.github.qdiaps.solitaire.ui.theme.CardFaceStyle
import io.github.qdiaps.solitaire.ui.theme.FeltTheme
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GameContractTest {

    @Nested
    @DisplayName("GameUiState tests")
    inner class GameUiStateTests {

        @Test
        fun `verify default GameUiState properties`() {
            val state = GameUiState()

            assertEquals(BoardState(), state.boardState)
            assertFalse(state.isGameWon)
            assertFalse(state.isDeadlocked)
            assertFalse(state.canUndo)
            assertEquals(0L, state.elapsedTimeSeconds)
            assertEquals(FeltTheme.CLASSIC_GREEN, state.feltTheme)
            assertFalse(state.isLeftHanded)
            assertNull(state.activeHint)
            assertFalse(state.isLoading)
            assertFalse(state.isAutoCompleteAvailable)
            assertEquals(1L, state.gameSessionId)
            assertEquals(DrawMode.DRAW_ONE, state.drawMode)
            assertEquals(CardBackStyle.CLASSIC_LATTICE, state.cardBackStyle)
            assertEquals(CardFaceStyle.MODERN_CLEAN, state.cardFaceStyle)
            assertTrue(state.soundEnabled)
            assertTrue(state.hapticsEnabled)
            assertFalse(state.autoHintEnabled)
            assertFalse(state.isSettingsOpen)
            assertNull(state.highlightedCard)
            assertFalse(state.isHintActive)
        }

        @Test
        fun `verify highlightedCard and isHintActive derived from activeHint`() {
            val hintCard = Card(Suit.HEARTS, Rank.ACE, isFaceUp = true)
            val move = Move(
                source = CardLocation.Waste,
                destination = CardLocation.Foundation(0),
                cards = listOf(hintCard)
            )
            val hint = Hint(
                move = move,
                priority = HintPriority.FOUNDATION_PROMOTION,
                description = "Move Ace to Foundation"
            )

            val stateWithHint = GameUiState(activeHint = hint)

            assertTrue(stateWithHint.isHintActive)
            assertEquals(hintCard, stateWithHint.highlightedCard)
            assertEquals(listOf(hintCard), stateWithHint.highlightedCards)
            assertEquals(CardLocation.Waste, stateWithHint.hintSourceLocation)
            assertEquals(CardLocation.Foundation(0), stateWithHint.hintTargetLocation)
        }

        @Test
        fun `verify state copying retains immutability`() {
            val initial = GameUiState(elapsedTimeSeconds = 10L, isGameWon = false)
            val updated = initial.copy(
                elapsedTimeSeconds = 11L,
                isGameWon = true,
                canUndo = true,
                isSettingsOpen = true,
                drawMode = DrawMode.DRAW_THREE
            )

            assertEquals(10L, initial.elapsedTimeSeconds)
            assertFalse(initial.isGameWon)
            assertFalse(initial.canUndo)
            assertFalse(initial.isSettingsOpen)
            assertEquals(DrawMode.DRAW_ONE, initial.drawMode)

            assertEquals(11L, updated.elapsedTimeSeconds)
            assertTrue(updated.isGameWon)
            assertTrue(updated.canUndo)
            assertTrue(updated.isSettingsOpen)
            assertEquals(DrawMode.DRAW_THREE, updated.drawMode)
        }
    }

    @Nested
    @DisplayName("GameIntent tests")
    inner class GameIntentTests {

        @Test
        fun `verify exhaustive handling of all GameIntents`() {
            val card = Card(Suit.SPADES, Rank.KING, isFaceUp = true)
            val intents: List<GameIntent> = listOf(
                GameIntent.DrawStockCard,
                GameIntent.RecycleStock,
                GameIntent.OnCardTapped(card, CardLocation.Waste),
                GameIntent.OnCardDropped(
                    cards = listOf(card),
                    source = CardLocation.Waste,
                    target = CardLocation.Tableau(columnIndex = 0)
                ),
                GameIntent.UndoMove,
                GameIntent.RequestHint,
                GameIntent.DismissHint,
                GameIntent.AutoComplete,
                GameIntent.StartAutoComplete,
                GameIntent.FinishAutoComplete,
                GameIntent.ApplyAutoCompleteMove(
                    AutoCompleteMove(
                        card = Card(Suit.SPADES, Rank.ACE, isFaceUp = true),
                        from = CardLocation.Tableau(0, 0),
                        to = CardLocation.Foundation(0),
                        resultingState = BoardState()
                    )
                ),
                GameIntent.StartNewGame,
                GameIntent.RestartGame,
                GameIntent.ToggleLeftHanded,
                GameIntent.SelectFeltTheme(FeltTheme.DEEP_NAVY),
                GameIntent.SetFeltTheme(FeltTheme.DARK_CHARCOAL),
                GameIntent.SkipWinAnimation,
                GameIntent.OpenSettings,
                GameIntent.CloseSettings,
                GameIntent.SetDrawMode(DrawMode.DRAW_THREE),
                GameIntent.SetLeftHanded(true),
                GameIntent.SetCardBackStyle(CardBackStyle.CRIMSON_VINTAGE),
                GameIntent.SetCardFaceStyle(CardFaceStyle.MODERN_CLEAN),
                GameIntent.SetSoundEnabled(false),
                GameIntent.SetHapticsEnabled(false),
                GameIntent.SetAutoHintEnabled(true),
                GameIntent.ResetSettingsToDefaults,
                GameIntent.OpenStats,
                GameIntent.CloseStats,
                GameIntent.ResetStats
            )

            assertEquals(30, intents.size)

            for (intent in intents) {
                val label = when (intent) {
                    is GameIntent.DrawStockCard -> "DrawStockCard"
                    is GameIntent.RecycleStock -> "RecycleStock"
                    is GameIntent.OnCardTapped -> "OnCardTapped:${intent.card.id}"
                    is GameIntent.OnCardDropped -> "OnCardDropped:${intent.cards.size}"
                    is GameIntent.UndoMove -> "UndoMove"
                    is GameIntent.RequestHint -> "RequestHint"
                    is GameIntent.DismissHint -> "DismissHint"
                    is GameIntent.AutoComplete -> "AutoComplete"
                    is GameIntent.StartAutoComplete -> "StartAutoComplete"
                    is GameIntent.FinishAutoComplete -> "FinishAutoComplete"
                    is GameIntent.ApplyAutoCompleteMove -> "ApplyAutoCompleteMove:${intent.move.card.id}"
                    is GameIntent.StartNewGame -> "StartNewGame"
                    is GameIntent.RestartGame -> "RestartGame"
                    is GameIntent.ToggleLeftHanded -> "ToggleLeftHanded"
                    is GameIntent.SelectFeltTheme -> "SelectFeltTheme:${intent.theme.name}"
                    is GameIntent.SetFeltTheme -> "SetFeltTheme:${intent.theme.name}"
                    is GameIntent.SkipWinAnimation -> "SkipWinAnimation"
                    is GameIntent.OpenSettings -> "OpenSettings"
                    is GameIntent.CloseSettings -> "CloseSettings"
                    is GameIntent.SetDrawMode -> "SetDrawMode:${intent.drawMode}"
                    is GameIntent.SetLeftHanded -> "SetLeftHanded:${intent.isLeftHanded}"
                    is GameIntent.SetCardBackStyle -> "SetCardBackStyle:${intent.cardBackStyle}"
                    is GameIntent.SetCardFaceStyle -> "SetCardFaceStyle:${intent.cardFaceStyle}"
                    is GameIntent.SetSoundEnabled -> "SetSoundEnabled:${intent.enabled}"
                    is GameIntent.SetHapticsEnabled -> "SetHapticsEnabled:${intent.enabled}"
                    is GameIntent.SetAutoHintEnabled -> "SetAutoHintEnabled:${intent.enabled}"
                    is GameIntent.ResetSettingsToDefaults -> "ResetSettingsToDefaults"
                    is GameIntent.OpenStats -> "OpenStats"
                    is GameIntent.CloseStats -> "CloseStats"
                    is GameIntent.ResetStats -> "ResetStats"
                }
                assertTrue(label.isNotEmpty())
            }
        }
    }

    @Nested
    @DisplayName("GameEvent tests")
    inner class GameEventTests {

        @Test
        fun `verify exhaustive handling of all GameEvents`() {
            val events: List<GameEvent> = listOf(
                GameEvent.PlayHapticTick,
                GameEvent.PlayHapticSnap,
                GameEvent.PlayDealSound,
                GameEvent.ShowMessage("Test message"),
                GameEvent.TriggerWinCelebration
            )

            assertEquals(5, events.size)

            for (event in events) {
                val name = when (event) {
                    is GameEvent.PlayHapticTick -> "tick"
                    is GameEvent.PlayHapticSnap -> "snap"
                    is GameEvent.PlayDealSound -> "deal"
                    is GameEvent.ShowMessage -> "message:${event.message}"
                    is GameEvent.TriggerWinCelebration -> "celebration"
                }
                assertTrue(name.isNotEmpty())
            }
        }
    }
}
