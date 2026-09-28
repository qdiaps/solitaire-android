package io.github.qdiaps.solitaire.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import io.github.qdiaps.solitaire.ui.game.animation.victory.ClassicBounceAnimator
import io.github.qdiaps.solitaire.ui.game.animation.victory.FoundationOrigin
import io.github.qdiaps.solitaire.ui.game.animation.victory.VictoryOverlay
import io.github.qdiaps.solitaire.ui.game.animation.victory.VictoryStartSpec
import io.github.qdiaps.solitaire.ui.game.animation.victory.rememberCardSpriteCache
import io.github.qdiaps.solitaire.ui.theme.CardDimensions
import io.github.qdiaps.solitaire.ui.theme.SolitaireTheme

@Preview(name = "1. Victory Overlay - Active Classic Bouncing Cascade", widthDp = 360, heightDp = 640)
@Composable
fun VictoryOverlayBouncingCascadePreview() {
    val dimensions = CardDimensions.calculate(availableWidth = 360.dp)
    SolitaireTheme(cardDimensions = dimensions) {
        val density = LocalDensity.current
        val spriteCache = rememberCardSpriteCache()

        val animator = remember {
            val foundations = Suit.entries.map { suit ->
                Rank.entries.map { rank -> Card(suit, rank, isFaceUp = true) }
            }
            val cardWidthPx = with(density) { dimensions.cardWidth.toPx() }
            val cardHeightPx = with(density) { dimensions.cardHeight.toPx() }
            val screenWidthPx = with(density) { 360.dp.toPx() }
            val screenHeightPx = with(density) { 640.dp.toPx() }

            val origins = listOf(
                FoundationOrigin(screenWidthPx * 0.45f, screenHeightPx * 0.05f),
                FoundationOrigin(screenWidthPx * 0.58f, screenHeightPx * 0.05f),
                FoundationOrigin(screenWidthPx * 0.71f, screenHeightPx * 0.05f),
                FoundationOrigin(screenWidthPx * 0.84f, screenHeightPx * 0.05f)
            )

            ClassicBounceAnimator().apply {
                start(
                    VictoryStartSpec(
                        foundations = foundations,
                        foundationOrigins = origins,
                        screenWidth = screenWidthPx,
                        screenHeight = screenHeightPx,
                        cardWidth = cardWidthPx,
                        cardHeight = cardHeightPx
                    )
                )
                // Advance 800ms of physics to show multiple bouncing cards mid-flight
                update(0.8f)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SolitaireTheme.colors.tableBackground)
        ) {
            VictoryOverlay(
                animator = animator,
                spriteCache = spriteCache,
                onDismiss = {}
            )
        }
    }
}
