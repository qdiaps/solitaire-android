package io.github.qdiaps.solitaire.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import io.github.qdiaps.solitaire.ui.game.components.DragOverlayContent
import io.github.qdiaps.solitaire.ui.theme.SolitaireTheme

@Preview(name = "1. Drag Overlay - Single Floating Card", widthDp = 360, heightDp = 640)
@Composable
fun DragOverlaySingleCardPreview() {
    SolitaireTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SolitaireTheme.colors.tableBackground)
        ) {
            DragOverlayContent(
                cards = listOf(
                    Card(Suit.HEARTS, Rank.QUEEN, isFaceUp = true)
                ),
                dragPosition = { IntOffset(180, 260) }
            )
        }
    }
}

@Preview(name = "2. Drag Overlay - Cascading Multi-Card Stack", widthDp = 360, heightDp = 640)
@Composable
fun DragOverlayCardStackPreview() {
    SolitaireTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SolitaireTheme.colors.tableBackground)
        ) {
            DragOverlayContent(
                cards = listOf(
                    Card(Suit.SPADES, Rank.TEN, isFaceUp = true),
                    Card(Suit.HEARTS, Rank.NINE, isFaceUp = true),
                    Card(Suit.CLUBS, Rank.EIGHT, isFaceUp = true)
                ),
                dragPosition = { IntOffset(140, 320) }
            )
        }
    }
}
