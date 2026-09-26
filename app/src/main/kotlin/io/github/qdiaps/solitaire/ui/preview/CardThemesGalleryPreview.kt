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
import io.github.qdiaps.solitaire.ui.game.components.CardView
import io.github.qdiaps.solitaire.ui.theme.CardBackStyle
import io.github.qdiaps.solitaire.ui.theme.CardDimensions
import io.github.qdiaps.solitaire.ui.theme.CardFaceStyle
import io.github.qdiaps.solitaire.ui.theme.SolitaireTheme

@Preview(name = "1. Modern Clean Card Faces (4 Suits)", showBackground = true)
@Composable
fun ModernCleanCardsPreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    SolitaireTheme(cardDimensions = sampleDimensions) {
        Column(
            modifier = Modifier
                .background(SolitaireTheme.colors.tableBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Modern Clean Face Style (Canonical)",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CardView(
                    card = Card(suit = Suit.HEARTS, rank = Rank.ACE, isFaceUp = true),
                    cardFaceStyle = CardFaceStyle.MODERN_CLEAN
                )
                CardView(
                    card = Card(suit = Suit.SPADES, rank = Rank.KING, isFaceUp = true),
                    cardFaceStyle = CardFaceStyle.MODERN_CLEAN
                )
                CardView(
                    card = Card(suit = Suit.DIAMONDS, rank = Rank.TEN, isFaceUp = true),
                    cardFaceStyle = CardFaceStyle.MODERN_CLEAN
                )
                CardView(
                    card = Card(suit = Suit.CLUBS, rank = Rank.QUEEN, isFaceUp = true),
                    cardFaceStyle = CardFaceStyle.MODERN_CLEAN
                )
            }
        }
    }
}

@Preview(name = "2. Card Themes Gallery (Modern Clean Faces with 4 Card Backs)", showBackground = true)
@Composable
fun CardThemesGalleryPreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    SolitaireTheme(cardDimensions = sampleDimensions) {
        Column(
            modifier = Modifier
                .background(SolitaireTheme.colors.tableBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Card Theme Customization Gallery",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            CardBackStyle.entries.forEach { backStyle ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back card
                    CardView(
                        card = Card(suit = Suit.SPADES, rank = Rank.KING, isFaceUp = false),
                        cardBackStyle = backStyle
                    )

                    // Modern Clean face cards
                    CardView(
                        card = Card(suit = Suit.HEARTS, rank = Rank.ACE, isFaceUp = true),
                        cardFaceStyle = CardFaceStyle.MODERN_CLEAN
                    )
                    CardView(
                        card = Card(suit = Suit.DIAMONDS, rank = Rank.KING, isFaceUp = true),
                        cardFaceStyle = CardFaceStyle.MODERN_CLEAN
                    )
                    CardView(
                        card = Card(suit = Suit.CLUBS, rank = Rank.TEN, isFaceUp = true),
                        cardFaceStyle = CardFaceStyle.MODERN_CLEAN
                    )

                    Text(
                        text = backStyle.displayName,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
