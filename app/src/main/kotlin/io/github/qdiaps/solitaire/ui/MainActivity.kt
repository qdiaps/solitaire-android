package io.github.qdiaps.solitaire.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import io.github.qdiaps.solitaire.ui.game.components.CardSlotPlaceholder
import io.github.qdiaps.solitaire.ui.game.components.CardView
import io.github.qdiaps.solitaire.ui.game.components.SlotWatermark
import io.github.qdiaps.solitaire.ui.theme.CardDimensions
import io.github.qdiaps.solitaire.ui.theme.FeltTheme
import io.github.qdiaps.solitaire.ui.theme.SolitaireTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val dimensions = CardDimensions.calculate(availableWidth = maxWidth)
                SolitaireTheme(feltTheme = FeltTheme.CLASSIC_GREEN, cardDimensions = dimensions) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(SolitaireTheme.colors.tableBackground)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = dimensions.horizontalPadding, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Solitaire Card Component Showcase",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Face-Up Cards (Suits & Ranks)",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(dimensions.columnSpacing)) {
                            CardView(card = Card(suit = Suit.HEARTS, rank = Rank.ACE, isFaceUp = true))
                            CardView(card = Card(suit = Suit.DIAMONDS, rank = Rank.TEN, isFaceUp = true))
                            CardView(card = Card(suit = Suit.CLUBS, rank = Rank.QUEEN, isFaceUp = true))
                            CardView(card = Card(suit = Suit.SPADES, rank = Rank.KING, isFaceUp = true))
                        }

                        Text(
                            text = "Face-Down & Highlighted",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(dimensions.columnSpacing)) {
                            CardView(card = Card(suit = Suit.SPADES, rank = Rank.KING, isFaceUp = false))
                            CardView(
                                card = Card(suit = Suit.HEARTS, rank = Rank.SEVEN, isFaceUp = true),
                                isHighlighted = true
                            )
                        }

                        Text(
                            text = "Empty Slot Placeholders (Foundations & Recycle)",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(dimensions.columnSpacing)) {
                            CardSlotPlaceholder(watermark = SlotWatermark.FoundationSuit(Suit.HEARTS))
                            CardSlotPlaceholder(watermark = SlotWatermark.FoundationSuit(Suit.SPADES))
                            CardSlotPlaceholder(watermark = SlotWatermark.FoundationSuit(Suit.DIAMONDS))
                            CardSlotPlaceholder(watermark = SlotWatermark.FoundationSuit(Suit.CLUBS))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(dimensions.columnSpacing)) {
                            CardSlotPlaceholder(watermark = SlotWatermark.StockRecycle)
                            CardSlotPlaceholder(watermark = SlotWatermark.TableauKing)
                            CardSlotPlaceholder(watermark = SlotWatermark.None)
                        }
                    }
                }
            }
        }
    }
}
