package io.github.qdiaps.solitaire.ui.game.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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

private val SAMPLE_FOUNDATIONS_PARTIAL = listOf(
    listOf(Card(suit = Suit.HEARTS, rank = Rank.ACE, isFaceUp = true)),
    listOf(
        Card(suit = Suit.DIAMONDS, rank = Rank.ACE, isFaceUp = true),
        Card(suit = Suit.DIAMONDS, rank = Rank.TWO, isFaceUp = true)
    ),
    emptyList(),
    listOf(Card(suit = Suit.SPADES, rank = Rank.ACE, isFaceUp = true))
)

private val SAMPLE_FOUNDATIONS_FULL = listOf(
    listOf(Card(suit = Suit.HEARTS, rank = Rank.KING, isFaceUp = true)),
    listOf(Card(suit = Suit.DIAMONDS, rank = Rank.KING, isFaceUp = true)),
    listOf(Card(suit = Suit.CLUBS, rank = Rank.KING, isFaceUp = true)),
    listOf(Card(suit = Suit.SPADES, rank = Rank.KING, isFaceUp = true))
)

@Preview(name = "1. Standard Initial State", showBackground = true)
@Composable
fun TopRowInitialPreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    SolitaireTheme(cardDimensions = sampleDimensions) {
        Column(
            modifier = Modifier
                .background(SolitaireTheme.colors.tableBackground)
                .padding(vertical = 16.dp)
        ) {
            TopRowView(
                stockCount = 24,
                wasteTopCard = null,
                foundations = List(4) { emptyList() },
                isLeftHanded = false
            )
        }
    }
}

@Preview(name = "2. Standard Mid-Game State", showBackground = true)
@Composable
fun TopRowMidGamePreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    SolitaireTheme(cardDimensions = sampleDimensions) {
        Column(
            modifier = Modifier
                .background(SolitaireTheme.colors.tableBackground)
                .padding(vertical = 16.dp)
        ) {
            TopRowView(
                stockCount = 15,
                wasteTopCard = Card(suit = Suit.CLUBS, rank = Rank.TEN, isFaceUp = true),
                foundations = SAMPLE_FOUNDATIONS_PARTIAL,
                isLeftHanded = false
            )
        }
    }
}

@Preview(name = "3. Empty Stock with Recycle Watermark", showBackground = true)
@Composable
fun TopRowEmptyStockRecyclePreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    SolitaireTheme(cardDimensions = sampleDimensions) {
        Column(
            modifier = Modifier
                .background(SolitaireTheme.colors.tableBackground)
                .padding(vertical = 16.dp)
        ) {
            TopRowView(
                stockCount = 0,
                wasteTopCard = Card(suit = Suit.HEARTS, rank = Rank.SEVEN, isFaceUp = true),
                foundations = SAMPLE_FOUNDATIONS_PARTIAL,
                canRecycle = true,
                isLeftHanded = false
            )
        }
    }
}

@Preview(name = "4. Exhausted Stock and Waste", showBackground = true)
@Composable
fun TopRowExhaustedStockPreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    SolitaireTheme(cardDimensions = sampleDimensions) {
        Column(
            modifier = Modifier
                .background(SolitaireTheme.colors.tableBackground)
                .padding(vertical = 16.dp)
        ) {
            TopRowView(
                stockCount = 0,
                wasteTopCard = null,
                foundations = SAMPLE_FOUNDATIONS_PARTIAL,
                canRecycle = false,
                isLeftHanded = false
            )
        }
    }
}

@Preview(name = "5. Left-Handed Mode (Mirrored)", showBackground = true)
@Composable
fun TopRowLeftHandedPreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    SolitaireTheme(cardDimensions = sampleDimensions) {
        Column(
            modifier = Modifier
                .background(SolitaireTheme.colors.tableBackground)
                .padding(vertical = 16.dp)
        ) {
            TopRowView(
                stockCount = 15,
                wasteTopCard = Card(suit = Suit.CLUBS, rank = Rank.TEN, isFaceUp = true),
                foundations = SAMPLE_FOUNDATIONS_PARTIAL,
                isLeftHanded = true
            )
        }
    }
}

@Preview(name = "6. Highlighted Hint Elements", showBackground = true)
@Composable
fun TopRowHighlightedPreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    SolitaireTheme(cardDimensions = sampleDimensions) {
        Column(
            modifier = Modifier
                .background(SolitaireTheme.colors.tableBackground)
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Waste highlighted (e.g. can move to foundation)
            TopRowView(
                stockCount = 15,
                wasteTopCard = Card(suit = Suit.DIAMONDS, rank = Rank.THREE, isFaceUp = true),
                foundations = SAMPLE_FOUNDATIONS_PARTIAL,
                isWasteHighlighted = true,
                highlightedFoundationIndex = 1
            )
            // Stock highlighted (e.g. hint draws from stock)
            TopRowView(
                stockCount = 10,
                wasteTopCard = Card(suit = Suit.SPADES, rank = Rank.FOUR, isFaceUp = true),
                foundations = SAMPLE_FOUNDATIONS_PARTIAL,
                isStockHighlighted = true
            )
        }
    }
}

@Preview(name = "7. Full Foundations Victory", showBackground = true)
@Composable
fun TopRowVictoryPreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    SolitaireTheme(cardDimensions = sampleDimensions) {
        Column(
            modifier = Modifier
                .background(SolitaireTheme.colors.tableBackground)
                .padding(vertical = 16.dp)
        ) {
            TopRowView(
                stockCount = 0,
                wasteTopCard = null,
                foundations = SAMPLE_FOUNDATIONS_FULL,
                canRecycle = false,
                isLeftHanded = false
            )
        }
    }
}

@Preview(name = "8. Felt Themes Comparison", showBackground = true)
@Composable
fun TopRowFeltThemesPreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    val testThemes = listOf(
        FeltTheme.CLASSIC_GREEN,
        FeltTheme.DEEP_NAVY,
        FeltTheme.DARK_CHARCOAL,
        FeltTheme.WINE_RED
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        for (theme in testThemes) {
            SolitaireTheme(feltTheme = theme, cardDimensions = sampleDimensions) {
                Column(
                    modifier = Modifier
                        .background(SolitaireTheme.colors.tableBackground)
                        .padding(vertical = 8.dp)
                ) {
                    TopRowView(
                        stockCount = 12,
                        wasteTopCard = Card(suit = Suit.HEARTS, rank = Rank.QUEEN, isFaceUp = true),
                        foundations = SAMPLE_FOUNDATIONS_PARTIAL,
                        isLeftHanded = false
                    )
                }
            }
        }
    }
}
