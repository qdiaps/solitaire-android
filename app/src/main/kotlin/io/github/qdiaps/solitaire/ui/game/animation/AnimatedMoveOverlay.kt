package io.github.qdiaps.solitaire.ui.game.animation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.qdiaps.solitaire.ui.game.components.CardView
import io.github.qdiaps.solitaire.ui.theme.SolitaireTheme
import kotlin.math.roundToInt

/**
 * Top-level floating overlay layer rendering card flight animations
 * (Smart Tap flights and Stock Draw 3D flip) in root window coordinates.
 *
 * @param flightState Active [CardFlightState] orchestrating card movement.
 * @param modifier Compose [Modifier] applied to this overlay.
 */
@Composable
fun AnimatedMoveOverlay(
    flightState: CardFlightState,
    modifier: Modifier = Modifier
) {
    val flight = flightState.activeFlight ?: return
    val dimensions = SolitaireTheme.cardDimensions
    val density = LocalDensity.current

    Box(modifier = modifier.fillMaxSize()) {
        val currentOffset = flight.currentOffset

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = currentOffset.x.roundToInt(),
                        y = currentOffset.y.roundToInt()
                    )
                }
                .graphicsLayer {
                    if (flight.isStockFlip) {
                        rotationY = flight.rotationY
                        cameraDistance = 12f * density.density
                    }
                }
        ) {
            val isFaceUp = flight.showCardFace

            flight.cards.forEachIndexed { index, card ->
                key(card.id) {
                    val cardToRender = if (flight.isStockFlip) {
                        card.copy(isFaceUp = isFaceUp)
                    } else {
                        card
                    }

                    val yOffset = dimensions.faceUpPeek * index

                    CardView(
                        card = cardToRender,
                        modifier = Modifier
                            .offset(y = yOffset)
                            .size(dimensions.cardWidth, dimensions.cardHeight),
                        elevation = 10.dp,
                        animateFlip = false
                    )
                }
            }
        }
    }
}
