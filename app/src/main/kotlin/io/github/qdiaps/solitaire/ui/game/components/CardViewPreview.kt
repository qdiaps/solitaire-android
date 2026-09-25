package io.github.qdiaps.solitaire.ui.game.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import io.github.qdiaps.solitaire.ui.theme.CardDimensions
import io.github.qdiaps.solitaire.ui.theme.FeltTheme
import io.github.qdiaps.solitaire.ui.theme.SolitaireTheme

@Preview(name = "Face-Up Cards (4 Suits)", showBackground = true)
@Composable
fun FaceUpCardsPreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    SolitaireTheme(cardDimensions = sampleDimensions) {
        Row(
            modifier = Modifier
                .background(SolitaireTheme.colors.tableBackground)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CardView(card = Card(suit = Suit.SPADES, rank = Rank.ACE, isFaceUp = true))
            CardView(card = Card(suit = Suit.HEARTS, rank = Rank.TEN, isFaceUp = true))
            CardView(card = Card(suit = Suit.DIAMONDS, rank = Rank.QUEEN, isFaceUp = true))
            CardView(card = Card(suit = Suit.CLUBS, rank = Rank.KING, isFaceUp = true))
        }
    }
}

@Preview(name = "Face-Down & Highlighted Cards", showBackground = true)
@Composable
fun FaceDownAndHighlightedPreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    SolitaireTheme(cardDimensions = sampleDimensions) {
        Row(
            modifier = Modifier
                .background(SolitaireTheme.colors.tableBackground)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Normal Face-down
            CardView(card = Card(suit = Suit.SPADES, rank = Rank.KING, isFaceUp = false))
            // Face-up highlighted (hint)
            CardView(
                card = Card(suit = Suit.HEARTS, rank = Rank.ACE, isFaceUp = true),
                isHighlighted = true
            )
            // Face-down highlighted
            CardView(
                card = Card(suit = Suit.DIAMONDS, rank = Rank.JACK, isFaceUp = false),
                isHighlighted = true
            )
        }
    }
}

@Preview(name = "Empty Slot Placeholders with Watermarks", showBackground = true)
@Composable
fun EmptySlotsPreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    SolitaireTheme(cardDimensions = sampleDimensions) {
        Column(
            modifier = Modifier
                .background(SolitaireTheme.colors.tableBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 4 Foundation suit slots
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CardSlotPlaceholder(watermark = SlotWatermark.FoundationSuit(Suit.HEARTS))
                CardSlotPlaceholder(watermark = SlotWatermark.FoundationSuit(Suit.SPADES))
                CardSlotPlaceholder(watermark = SlotWatermark.FoundationSuit(Suit.DIAMONDS))
                CardSlotPlaceholder(watermark = SlotWatermark.FoundationSuit(Suit.CLUBS))
            }
            // Stock recycle & Tableau King slots
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CardSlotPlaceholder(watermark = SlotWatermark.StockRecycle)
                CardSlotPlaceholder(watermark = SlotWatermark.TableauKing)
                CardSlotPlaceholder(watermark = SlotWatermark.None)
            }
        }
    }
}

@Preview(name = "Felt Themes Comparison", showBackground = true)
@Composable
fun FeltThemesComparisonPreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    val testThemes = listOf(
        FeltTheme.CLASSIC_GREEN,
        FeltTheme.DEEP_NAVY,
        FeltTheme.WINE_RED,
        FeltTheme.DARK_CHARCOAL
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (theme in testThemes) {
            SolitaireTheme(feltTheme = theme, cardDimensions = sampleDimensions) {
                Row(
                    modifier = Modifier
                        .background(SolitaireTheme.colors.tableBackground)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CardView(card = Card(suit = Suit.HEARTS, rank = Rank.ACE, isFaceUp = true))
                    CardView(card = Card(suit = Suit.SPADES, rank = Rank.KING, isFaceUp = false))
                    CardSlotPlaceholder(watermark = SlotWatermark.FoundationSuit(Suit.HEARTS))
                    CardSlotPlaceholder(watermark = SlotWatermark.StockRecycle)
                }
            }
        }
    }
}
