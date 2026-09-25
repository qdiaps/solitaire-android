package io.github.qdiaps.solitaire.ui.game.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import io.github.qdiaps.solitaire.ui.theme.CardDimensions
import io.github.qdiaps.solitaire.ui.theme.FeltTheme
import io.github.qdiaps.solitaire.ui.theme.SolitaireTheme

private val SAMPLE_INITIAL_DEAL_COLUMN = listOf(
    Card(Suit.SPADES, Rank.FOUR, isFaceUp = false),
    Card(Suit.DIAMONDS, Rank.JACK, isFaceUp = false),
    Card(Suit.CLUBS, Rank.NINE, isFaceUp = false),
    Card(Suit.HEARTS, Rank.TEN, isFaceUp = true)
)

private val SAMPLE_DEEP_CASCADE_13_CARDS = listOf(
    Card(Suit.SPADES, Rank.KING, isFaceUp = true),
    Card(Suit.HEARTS, Rank.QUEEN, isFaceUp = true),
    Card(Suit.CLUBS, Rank.JACK, isFaceUp = true),
    Card(Suit.DIAMONDS, Rank.TEN, isFaceUp = true),
    Card(Suit.SPADES, Rank.NINE, isFaceUp = true),
    Card(Suit.HEARTS, Rank.EIGHT, isFaceUp = true),
    Card(Suit.CLUBS, Rank.SEVEN, isFaceUp = true),
    Card(Suit.DIAMONDS, Rank.SIX, isFaceUp = true),
    Card(Suit.SPADES, Rank.FIVE, isFaceUp = true),
    Card(Suit.HEARTS, Rank.FOUR, isFaceUp = true),
    Card(Suit.CLUBS, Rank.THREE, isFaceUp = true),
    Card(Suit.DIAMONDS, Rank.TWO, isFaceUp = true),
    Card(Suit.SPADES, Rank.ACE, isFaceUp = true)
)

@Preview(name = "1. Empty Column (King Watermark)", showBackground = true)
@Composable
fun EmptyTableauColumnPreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    SolitaireTheme(cardDimensions = sampleDimensions) {
        Box(
            modifier = Modifier
                .background(SolitaireTheme.colors.tableBackground)
                .padding(16.dp)
        ) {
            TableauColumnView(cards = emptyList())
        }
    }
}

@Preview(name = "2. Single Card Column", showBackground = true)
@Composable
fun SingleCardTableauColumnPreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    SolitaireTheme(cardDimensions = sampleDimensions) {
        Box(
            modifier = Modifier
                .background(SolitaireTheme.colors.tableBackground)
                .padding(16.dp)
        ) {
            TableauColumnView(
                cards = listOf(Card(Suit.SPADES, Rank.KING, isFaceUp = true))
            )
        }
    }
}

@Preview(name = "3. Initial Deal Column (3 Hidden + 1 Exposed)", showBackground = true)
@Composable
fun InitialDealTableauColumnPreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    SolitaireTheme(cardDimensions = sampleDimensions) {
        Box(
            modifier = Modifier
                .background(SolitaireTheme.colors.tableBackground)
                .padding(16.dp)
        ) {
            TableauColumnView(cards = SAMPLE_INITIAL_DEAL_COLUMN)
        }
    }
}

@Preview(name = "4. Deep 13-Card Cascade", showBackground = true)
@Composable
fun DeepCascadeTableauColumnPreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    SolitaireTheme(cardDimensions = sampleDimensions) {
        Box(
            modifier = Modifier
                .background(SolitaireTheme.colors.tableBackground)
                .padding(16.dp)
        ) {
            TableauColumnView(cards = SAMPLE_DEEP_CASCADE_13_CARDS)
        }
    }
}

@Preview(name = "5. Highlighted Card in Column (Hint)", showBackground = true)
@Composable
fun HighlightedCardTableauColumnPreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    val highlightTarget = SAMPLE_INITIAL_DEAL_COLUMN.last()

    SolitaireTheme(cardDimensions = sampleDimensions) {
        Box(
            modifier = Modifier
                .background(SolitaireTheme.colors.tableBackground)
                .padding(16.dp)
        ) {
            TableauColumnView(
                cards = SAMPLE_INITIAL_DEAL_COLUMN,
                highlightedCard = highlightTarget
            )
        }
    }
}

@Preview(name = "6. Multi-Columns Side-by-Side Comparison", showBackground = true)
@Composable
fun MultiColumnsComparisonPreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    SolitaireTheme(cardDimensions = sampleDimensions) {
        Row(
            modifier = Modifier
                .background(SolitaireTheme.colors.tableBackground)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(sampleDimensions.columnSpacing)
        ) {
            TableauColumnView(cards = emptyList())
            TableauColumnView(
                cards = listOf(Card(Suit.SPADES, Rank.KING, isFaceUp = true))
            )
            TableauColumnView(cards = SAMPLE_INITIAL_DEAL_COLUMN)
            TableauColumnView(cards = SAMPLE_DEEP_CASCADE_13_CARDS.take(6))
        }
    }
}

@Preview(name = "7. Felt Themes Comparison", showBackground = true)
@Composable
fun TableauColumnFeltThemesPreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    val testThemes = listOf(
        FeltTheme.CLASSIC_GREEN,
        FeltTheme.DEEP_NAVY,
        FeltTheme.DARK_CHARCOAL,
        FeltTheme.WINE_RED
    )

    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        for (theme in testThemes) {
            SolitaireTheme(feltTheme = theme, cardDimensions = sampleDimensions) {
                Box(
                    modifier = Modifier
                        .background(SolitaireTheme.colors.tableBackground)
                        .padding(8.dp)
                ) {
                    TableauColumnView(cards = SAMPLE_INITIAL_DEAL_COLUMN)
                }
            }
        }
    }
}
