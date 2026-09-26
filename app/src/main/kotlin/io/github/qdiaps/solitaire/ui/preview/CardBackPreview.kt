package io.github.qdiaps.solitaire.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import io.github.qdiaps.solitaire.ui.game.components.CardBackView
import io.github.qdiaps.solitaire.ui.game.components.CardView
import io.github.qdiaps.solitaire.ui.theme.CardBackStyle
import io.github.qdiaps.solitaire.ui.theme.CardDimensions
import io.github.qdiaps.solitaire.ui.theme.FeltTheme
import io.github.qdiaps.solitaire.ui.theme.SolitaireTheme

@Preview(name = "1. All 4 Card Back Styles Side-by-Side", showBackground = true)
@Composable
fun AllCardBackStylesPreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    SolitaireTheme(cardDimensions = sampleDimensions) {
        Column(
            modifier = Modifier
                .background(SolitaireTheme.colors.tableBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Card Back Customization (Phase 5)",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CardBackStyle.entries.forEach { style ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CardView(
                            card = Card(suit = Suit.SPADES, rank = Rank.KING, isFaceUp = false),
                            cardBackStyle = style
                        )
                        Text(
                            text = style.displayName,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "2. Card Back Styles with Face-Up Comparison", showBackground = true)
@Composable
fun CardBackComparisonWithFaceUpPreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    SolitaireTheme(cardDimensions = sampleDimensions) {
        Column(
            modifier = Modifier
                .background(SolitaireTheme.colors.tableBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CardBackStyle.entries.forEach { style ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CardView(
                        card = Card(suit = Suit.HEARTS, rank = Rank.ACE, isFaceUp = true)
                    )
                    CardView(
                        card = Card(suit = Suit.SPADES, rank = Rank.KING, isFaceUp = false),
                        cardBackStyle = style
                    )
                    Text(
                        text = style.displayName,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Preview(name = "3. Card Backs across 4 Felt Themes", showBackground = true)
@Composable
fun CardBacksAcrossFeltThemesPreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    val feltThemes = listOf(
        FeltTheme.CLASSIC_GREEN,
        FeltTheme.DEEP_NAVY,
        FeltTheme.DARK_CHARCOAL,
        FeltTheme.WINE_RED
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        feltThemes.forEach { felt ->
            SolitaireTheme(feltTheme = felt, cardDimensions = sampleDimensions) {
                Row(
                    modifier = Modifier
                        .background(SolitaireTheme.colors.tableBackground)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CardBackStyle.entries.forEach { style ->
                        CardView(
                            card = Card(suit = Suit.SPADES, rank = Rank.KING, isFaceUp = false),
                            cardBackStyle = style
                        )
                    }
                }
            }
        }
    }
}
