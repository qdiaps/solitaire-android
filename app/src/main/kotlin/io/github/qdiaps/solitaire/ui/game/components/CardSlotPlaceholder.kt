package io.github.qdiaps.solitaire.ui.game.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
 *
 * @param modifier Compose [Modifier] applied to this component.
 * @param watermark Watermark emblem rendered inside the empty slot.
 * @param isHighlighted Whether an active hint border is drawn around this slot with pulsing glow.
 * @param onClick Optional tap callback.
 */
@Composable
fun CardSlotPlaceholder(
    modifier: Modifier = Modifier,
    watermark: SlotWatermark = SlotWatermark.None,
    isHighlighted: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val colors = SolitaireTheme.colors
    val dimensions = SolitaireTheme.cardDimensions
    val shape = RoundedCornerShape(dimensions.cornerRadius)

    val infiniteTransition = rememberInfiniteTransition(label = "slotHintPulse")
    val pulseProgress by if (isHighlighted) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "slotPulseProgress"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    val pulseAlpha = if (isHighlighted) 0.55f + pulseProgress * 0.45f else 1f

    val borderModifier = if (isHighlighted) {
        Modifier.border(
            width = 3.dp,
            color = colors.hintHighlight.copy(alpha = pulseAlpha),
            shape = shape
        )
    } else {
        Modifier.border(
            width = 1.dp,
            color = colors.slotBorder,
            shape = shape
        )
    }

    val backgroundModifier = if (isHighlighted) {
        Modifier.background(colors.hintHighlight.copy(alpha = 0.16f + pulseProgress * 0.16f))
    } else {
        Modifier.background(colors.slotBackground)
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
            .clip(shape)
            .then(backgroundModifier)
            .then(borderModifier)
            .then(clickableModifier),
        contentAlignment = Alignment.Center
    ) {
        val watermarkTintColor = if (isHighlighted) {
            colors.hintHighlight.copy(alpha = 0.70f + pulseProgress * 0.30f)
        } else null

        when (watermark) {
            is SlotWatermark.None -> Unit
            is SlotWatermark.FoundationSuit -> {
                val watermarkColor = watermarkTintColor ?: Color.White.copy(alpha = 0.22f)
                val emblemSize = dimensions.cardWidth * 0.40f
                SuitEmblem(
                    suit = watermark.suit,
                    color = watermarkColor,
                    modifier = Modifier.size(emblemSize)
                )
            }
            is SlotWatermark.StockRecycle -> {
                val iconColor = watermarkTintColor ?: Color.White.copy(alpha = 0.25f)
                val iconSize = dimensions.cardWidth * 0.42f
                StockRecycleIcon(
                    color = iconColor,
                    modifier = Modifier.size(iconSize)
                )
            }
            is SlotWatermark.TableauKing -> {
                Text(
                    text = "K",
                    color = watermarkTintColor ?: Color.White.copy(alpha = 0.18f),
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
