package io.github.qdiaps.solitaire.ui.game.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.ui.theme.SolitaireTheme

/**
 * Renders an individual playing card in either face-up or face-down state.
 *
 * @param card Domain [Card] model.
 * @param modifier Compose [Modifier] applied to this card.
 * @param elevation Shadow elevation (defaults to 2.dp, or 12.dp when lifted in drag overlay).
 * @param isHighlighted Whether an active hint border is drawn around the card.
 * @param onClick Optional tap callback.
 */
@Composable
fun CardView(
    card: Card,
    modifier: Modifier = Modifier,
    elevation: Dp = 2.dp,
    isHighlighted: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val dimensions = SolitaireTheme.cardDimensions
    val colors = SolitaireTheme.colors
    val shape = RoundedCornerShape(dimensions.cornerRadius)

    val borderModifier = if (isHighlighted) {
        Modifier.border(width = 2.dp, color = colors.hintHighlight, shape = shape)
    } else {
        Modifier.border(width = 1.dp, color = colors.cardBorder, shape = shape)
    }

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(dimensions.cardWidth, dimensions.cardHeight)
            .shadow(elevation = elevation, shape = shape)
            .clip(shape)
            .then(borderModifier)
            .then(clickableModifier)
    ) {
        if (card.isFaceUp) {
            FaceUpCardContent(card = card)
        } else {
            FaceDownCardContent()
        }
    }
}

/**
 * Face-up card layout showing corner indices (rank + mini suit) and a center emblem.
 */
@Composable
private fun FaceUpCardContent(card: Card) {
    val colors = SolitaireTheme.colors
    val typography = SolitaireTheme.typography
    val dimensions = SolitaireTheme.cardDimensions

    val suitColor = if (card.suit.isRed) colors.cardRed else colors.cardBlack
    val indexPaddingHorizontal = dimensions.cardWidth * 0.08f
    val indexPaddingVertical = dimensions.cardHeight * 0.05f
    val cornerEmblemSize = dimensions.cardWidth * 0.22f
    val centerEmblemSize = dimensions.cardWidth * 0.44f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.cardBackground)
    ) {
        // Top-left index
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = indexPaddingHorizontal, top = indexPaddingVertical),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = card.rank.displayLabel,
                style = typography.cardRank,
                color = suitColor
            )
            SuitEmblem(
                suit = card.suit,
                color = suitColor,
                modifier = Modifier.size(cornerEmblemSize)
            )
        }

        // Center emblem
        SuitEmblem(
            suit = card.suit,
            color = suitColor,
            modifier = Modifier
                .size(centerEmblemSize)
                .align(Alignment.Center)
        )

        // Bottom-right index (mirrored 180 degrees)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = indexPaddingHorizontal, bottom = indexPaddingVertical)
                .graphicsLayer(rotationZ = 180f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = card.rank.displayLabel,
                style = typography.cardRank,
                color = suitColor
            )
            SuitEmblem(
                suit = card.suit,
                color = suitColor,
                modifier = Modifier.size(cornerEmblemSize)
            )
        }
    }
}

/**
 * Face-down card back with deep navy surface, inner margin border, and geometric diamond lattice.
 */
@Composable
private fun FaceDownCardContent() {
    val colors = SolitaireTheme.colors
    val dimensions = SolitaireTheme.cardDimensions

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.cardBackNavy)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val inset = dimensions.cardWidth.toPx() * 0.08f
            val corner = dimensions.cornerRadius.toPx() * 0.6f

            // Inner ornamental border
            drawRoundRect(
                color = Color.White.copy(alpha = 0.35f),
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2f, size.height - inset * 2f),
                cornerRadius = CornerRadius(corner, corner),
                style = Stroke(width = 1.dp.toPx())
            )

            // Inner geometric diamond lattice
            val innerLeft = inset * 1.6f
            val innerTop = inset * 1.6f
            val innerWidth = size.width - innerLeft * 2f
            val innerHeight = size.height - innerTop * 2f
            val step = innerWidth / 4f

            val latticeColor = Color.White.copy(alpha = 0.18f)
            val stroke = Stroke(width = 0.8.dp.toPx())

            // Diagonal lines (\ direction)
            var x = -innerHeight
            while (x < innerWidth + innerHeight) {
                drawLine(
                    color = latticeColor,
                    start = Offset(innerLeft + x, innerTop),
                    end = Offset(innerLeft + x + innerHeight, innerTop + innerHeight),
                    strokeWidth = stroke.width
                )
                x += step
            }

            // Diagonal lines (/ direction)
            x = -innerHeight
            while (x < innerWidth + innerHeight) {
                drawLine(
                    color = latticeColor,
                    start = Offset(innerLeft + x + innerHeight, innerTop),
                    end = Offset(innerLeft + x, innerTop + innerHeight),
                    strokeWidth = stroke.width
                )
                x += step
            }
        }
    }
}
