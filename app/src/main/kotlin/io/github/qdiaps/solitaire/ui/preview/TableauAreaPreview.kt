package io.github.qdiaps.solitaire.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.qdiaps.solitaire.domain.deck.KlondikeDealer
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import io.github.qdiaps.solitaire.ui.game.components.TableauAreaView
import io.github.qdiaps.solitaire.ui.theme.CardDimensions
import io.github.qdiaps.solitaire.ui.theme.FeltTheme
import io.github.qdiaps.solitaire.ui.theme.SolitaireTheme
import kotlin.random.Random

private val SAMPLE_INITIAL_TABLEAU = KlondikeDealer.dealShuffled(Random(42)).tableau

private val SAMPLE_MID_GAME_TABLEAU = listOf(
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
)

@Preview(name = "1. Initial Deal 7-Columns Layout", showBackground = true)
@Composable
fun TableauAreaInitialDealPreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    SolitaireTheme(cardDimensions = sampleDimensions) {
        Box(
            modifier = Modifier
                .background(SolitaireTheme.colors.tableBackground)
                .padding(vertical = 16.dp)
        ) {
            TableauAreaView(tableau = SAMPLE_INITIAL_TABLEAU)
        }
    }
}

@Preview(name = "2. Mid-Game State (Empty Columns & Cascades)", showBackground = true)
@Composable
fun TableauAreaMidGamePreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    SolitaireTheme(cardDimensions = sampleDimensions) {
        Box(
            modifier = Modifier
                .background(SolitaireTheme.colors.tableBackground)
                .padding(vertical = 16.dp)
        ) {
            TableauAreaView(tableau = SAMPLE_MID_GAME_TABLEAU)
        }
    }
}

@Preview(name = "3. Highlighted Card (Active Hint)", showBackground = true)
@Composable
fun TableauAreaHighlightedPreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    val targetCard = SAMPLE_MID_GAME_TABLEAU[4][3] // Seven of Hearts

    SolitaireTheme(cardDimensions = sampleDimensions) {
        Box(
            modifier = Modifier
                .background(SolitaireTheme.colors.tableBackground)
                .padding(vertical = 16.dp)
        ) {
            TableauAreaView(
                tableau = SAMPLE_MID_GAME_TABLEAU,
                highlightedCard = targetCard
            )
        }
    }
}

@Preview(name = "4. Felt Themes Comparison", showBackground = true)
@Composable
fun TableauAreaFeltThemesPreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    val testThemes = listOf(
        FeltTheme.CLASSIC_GREEN,
        FeltTheme.DEEP_NAVY,
        FeltTheme.DARK_CHARCOAL,
        FeltTheme.WINE_RED
    )

    Column {
        for (theme in testThemes) {
            SolitaireTheme(feltTheme = theme, cardDimensions = sampleDimensions) {
                Box(
                    modifier = Modifier
                        .background(SolitaireTheme.colors.tableBackground)
                        .padding(vertical = 12.dp)
                ) {
                    TableauAreaView(tableau = SAMPLE_MID_GAME_TABLEAU)
                }
            }
        }
    }
}
