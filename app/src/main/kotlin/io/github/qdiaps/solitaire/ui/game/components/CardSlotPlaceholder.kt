package io.github.qdiaps.solitaire.ui.game.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.qdiaps.solitaire.domain.model.Suit
import io.github.qdiaps.solitaire.ui.theme.SolitaireTheme

/**
 * Visual watermark hint rendered in the center of an empty card slot.
 */
sealed interface SlotWatermark {
    data object None : SlotWatermark
    data class FoundationSuit(val suit: Suit) : SlotWatermark
    data object StockRecycle : SlotWatermark
    data object TableauKing : SlotWatermark
}

/**
 * Placeholder component for empty card slots (Tableau columns, Foundations, Stock, Waste).
 */
@Composable
fun CardSlotPlaceholder(
    modifier: Modifier = Modifier,
    watermark: SlotWatermark = SlotWatermark.None,
    onClick: (() -> Unit)? = null
) {
    val colors = SolitaireTheme.colors
    val dimensions = SolitaireTheme.cardDimensions
    val shape = RoundedCornerShape(dimensions.cornerRadius)

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
            .clip(shape)
            .background(colors.slotBackground)
            .border(
                width = 1.dp,
                color = colors.slotBorder,
                shape = shape
            )
            .then(clickableModifier),
        contentAlignment = Alignment.Center
    ) {
        when (watermark) {
            is SlotWatermark.None -> Unit
            is SlotWatermark.FoundationSuit -> {
                val watermarkColor = Color.White.copy(alpha = 0.22f)
                val emblemSize = dimensions.cardWidth * 0.40f
                SuitEmblem(
                    suit = watermark.suit,
                    color = watermarkColor,
                    modifier = Modifier.size(emblemSize)
                )
            }
            is SlotWatermark.StockRecycle -> {
                val iconColor = Color.White.copy(alpha = 0.25f)
                val iconSize = dimensions.cardWidth * 0.42f
                StockRecycleIcon(
                    color = iconColor,
                    modifier = Modifier.size(iconSize)
                )
            }
            is SlotWatermark.TableauKing -> {
                Text(
                    text = "K",
                    color = Color.White.copy(alpha = 0.18f),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Circular recycle arrow icon rendered via Canvas for the stock recycle slot.
 */
@Composable
private fun StockRecycleIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.width * 0.12f
        val arcRadius = (size.width - strokeWidth * 2f) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Draw 300-degree circular sweep arc
        drawArc(
            color = color,
            startAngle = 35f,
            sweepAngle = 280f,
            useCenter = false,
            topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
            size = Size(arcRadius * 2f, arcRadius * 2f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Draw arrowhead at end of arc
        val arrowSize = strokeWidth * 2.2f
        val arrowPath = Path().apply {
            moveTo(center.x + arcRadius, center.y - strokeWidth * 0.2f)
            lineTo(center.x + arcRadius - arrowSize * 0.8f, center.y + arrowSize * 0.8f)
            lineTo(center.x + arcRadius + arrowSize * 0.8f, center.y + arrowSize * 0.6f)
            close()
        }
        drawPath(path = arrowPath, color = color)
    }
}
