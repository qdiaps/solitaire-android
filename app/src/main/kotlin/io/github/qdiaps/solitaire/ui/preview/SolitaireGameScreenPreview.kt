package io.github.qdiaps.solitaire.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.qdiaps.solitaire.domain.deck.KlondikeDealer
import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import io.github.qdiaps.solitaire.ui.game.SolitaireGameScreen
import io.github.qdiaps.solitaire.ui.theme.FeltTheme
import kotlin.random.Random

private val SAMPLE_INITIAL_DEAL_STATE = KlondikeDealer.dealShuffled(Random(42))

private val SAMPLE_MID_GAME_STATE = BoardState(
    stock = listOf(
        Card(Suit.CLUBS, Rank.TWO, isFaceUp = false),
        Card(Suit.HEARTS, Rank.FIVE, isFaceUp = false),
        Card(Suit.SPADES, Rank.JACK, isFaceUp = false)
    ),
    waste = listOf(
        Card(Suit.DIAMONDS, Rank.SEVEN, isFaceUp = true)
    ),
    foundations = listOf(
        listOf(
            Card(Suit.HEARTS, Rank.ACE, isFaceUp = true),
            Card(Suit.HEARTS, Rank.TWO, isFaceUp = true)
        ),
        listOf(
            Card(Suit.DIAMONDS, Rank.ACE, isFaceUp = true)
        ),
        emptyList(),
        emptyList()
    ),
    tableau = listOf(
        emptyList(),
        listOf(
            Card(Suit.SPADES, Rank.KING, isFaceUp = true),
            Card(Suit.HEARTS, Rank.QUEEN, isFaceUp = true),
            Card(Suit.CLUBS, Rank.JACK, isFaceUp = true)
        ),
        listOf(
            Card(Suit.DIAMONDS, Rank.FOUR, isFaceUp = false),
            Card(Suit.DIAMONDS, Rank.TEN, isFaceUp = true)
        ),
        emptyList(),
        listOf(
            Card(Suit.CLUBS, Rank.FIVE, isFaceUp = false),
            Card(Suit.HEARTS, Rank.NINE, isFaceUp = false),
            Card(Suit.SPADES, Rank.EIGHT, isFaceUp = true),
            Card(Suit.HEARTS, Rank.SEVEN, isFaceUp = true),
            Card(Suit.CLUBS, Rank.SIX, isFaceUp = true)
        ),
        listOf(
            Card(Suit.DIAMONDS, Rank.KING, isFaceUp = true)
        ),
        listOf(
            Card(Suit.HEARTS, Rank.THREE, isFaceUp = false),
            Card(Suit.SPADES, Rank.TWO, isFaceUp = false),
            Card(Suit.DIAMONDS, Rank.SIX, isFaceUp = false),
            Card(Suit.CLUBS, Rank.ACE, isFaceUp = true)
        )
    ),
    score = 425,
    movesCount = 37
)

@Preview(name = "1. Empty Board Layout", device = "id:pixel_7", showBackground = true)
@Composable
fun EmptyBoardPreview() {
    SolitaireGameScreen(
        boardState = BoardState(),
        timeSeconds = 0L,
        canUndo = false
    )
}

@Preview(name = "2. Initial Deal Board", device = "id:pixel_7", showBackground = true)
@Composable
fun InitialDealBoardPreview() {
    SolitaireGameScreen(
        boardState = SAMPLE_INITIAL_DEAL_STATE,
        timeSeconds = 12L,
        canUndo = false
    )
}

@Preview(name = "3. Active Mid-Game Board", device = "id:pixel_7", showBackground = true)
@Composable
fun ActiveMidGameBoardPreview() {
    SolitaireGameScreen(
        boardState = SAMPLE_MID_GAME_STATE,
        timeSeconds = 194L, // 03:14
        canUndo = true
    )
}

@Preview(name = "4. Left-Handed Mode (Mirrored Top Row)", device = "id:pixel_7", showBackground = true)
@Composable
fun LeftHandedModeBoardPreview() {
    SolitaireGameScreen(
        boardState = SAMPLE_INITIAL_DEAL_STATE,
        isLeftHanded = true,
        timeSeconds = 25L,
        canUndo = true
    )
}

@Preview(name = "5. Active Hint Highlight", device = "id:pixel_7", showBackground = true)
@Composable
fun ActiveHintBoardPreview() {
    val hintCard = SAMPLE_MID_GAME_STATE.tableau[4][4] // Six of Clubs

    SolitaireGameScreen(
        boardState = SAMPLE_MID_GAME_STATE,
        timeSeconds = 210L,
        canUndo = true,
        highlightedCard = hintCard,
        isHintActive = true
    )
}

@Preview(name = "6. Felt Theme - Deep Navy", device = "id:pixel_7", showBackground = true)
@Composable
fun DeepNavyThemeBoardPreview() {
    SolitaireGameScreen(
        boardState = SAMPLE_MID_GAME_STATE,
        feltTheme = FeltTheme.DEEP_NAVY,
        timeSeconds = 194L,
        canUndo = true
    )
}

@Preview(name = "7. Felt Theme - Dark Charcoal", device = "id:pixel_7", showBackground = true)
@Composable
fun DarkCharcoalThemeBoardPreview() {
    SolitaireGameScreen(
        boardState = SAMPLE_MID_GAME_STATE,
        feltTheme = FeltTheme.DARK_CHARCOAL,
        timeSeconds = 194L,
        canUndo = true
    )
}

@Preview(name = "8. Felt Theme - Wine Red", device = "id:pixel_7", showBackground = true)
@Composable
fun WineRedThemeBoardPreview() {
    SolitaireGameScreen(
        boardState = SAMPLE_MID_GAME_STATE,
        feltTheme = FeltTheme.WINE_RED,
        timeSeconds = 194L,
        canUndo = true
    )
}
