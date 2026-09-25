package io.github.qdiaps.solitaire.ui.game.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.qdiaps.solitaire.ui.theme.SolitaireTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Visual action icon identifiers.
 */
enum class ActionIconType {
    UNDO,
    HINT,
    NEW_GAME,
    SETTINGS
}

/**
 * Dedicated vector Canvas icon for action bar buttons.
 */
@Composable
fun ActionIcon(
    type: ActionIconType,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        when (type) {
            ActionIconType.UNDO -> {
                // Curved counter-clockwise return arrow
                val strokeWidth = 1.75.dp.toPx()
                val radius = w * 0.36f
                val center = Offset(w * 0.52f, h * 0.52f)

                // Arc path from -30 deg to 210 deg
                val arcRect = Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius)
                val arcPath = Path().apply {
                    arcTo(arcRect, -30f, -200f, false)
                }
                drawPath(arcPath, tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))

                // Arrow head at arc start
                val arrowHead = Path().apply {
                    val tipX = center.x - radius * 0.95f
                    val tipY = center.y + radius * 0.35f
                    moveTo(tipX, tipY)
                    lineTo(tipX - 3.5.dp.toPx(), tipY - 4.5.dp.toPx())
                    lineTo(tipX + 3.5.dp.toPx(), tipY - 3.5.dp.toPx())
                    close()
                }
                drawPath(arrowHead, tint, style = Fill)
            }

            ActionIconType.HINT -> {
                // 4-point sparkle / light star
                val cx = w * 0.5f
                val cy = h * 0.5f
                val outerR = w * 0.44f

                val sparklePath = Path().apply {
                    moveTo(cx, cy - outerR)
                    quadraticTo(cx, cy, cx + outerR, cy)
                    quadraticTo(cx, cy, cx, cy + outerR)
                    quadraticTo(cx, cy, cx - outerR, cy)
                    quadraticTo(cx, cy, cx, cy - outerR)
                    close()
                }
                drawPath(sparklePath, tint, style = Fill)
            }

            ActionIconType.NEW_GAME -> {
                // Two overlapping mini cards or deal icon
                val strokeWidth = 1.5.dp.toPx()
                val cardW = w * 0.50f
                val cardH = h * 0.65f

                // Back card
                drawRoundRect(
                    color = tint.copy(alpha = 0.5f),
                    topLeft = Offset(w * 0.35f, h * 0.12f),
                    size = Size(cardW, cardH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                    style = Stroke(width = strokeWidth)
                )

                // Front card
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.15f, h * 0.25f),
                    size = Size(cardW, cardH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                    style = Stroke(width = strokeWidth)
                )

                // Small '+' sign in front card
                val plusCx = w * 0.15f + cardW * 0.5f
                val plusCy = h * 0.25f + cardH * 0.5f
                val arm = cardW * 0.22f
                drawLine(tint, Offset(plusCx - arm, plusCy), Offset(plusCx + arm, plusCy), strokeWidth)
                drawLine(tint, Offset(plusCx, plusCy - arm), Offset(plusCx, plusCy + arm), strokeWidth)
            }

            ActionIconType.SETTINGS -> {
                // 6-tooth gear icon
                val strokeWidth = 1.75.dp.toPx()
                val cx = w * 0.5f
                val cy = h * 0.5f
                val outerR = w * 0.40f
                val innerR = w * 0.28f
                val holeR = w * 0.14f

                // Central ring
                drawCircle(tint, innerR, Offset(cx, cy), style = Stroke(width = strokeWidth))
                // Central hole
                drawCircle(tint, holeR, Offset(cx, cy), style = Stroke(width = strokeWidth))

                // 6 teeth
                for (i in 0 until 6) {
                    val angle = (i * 60.0) * PI / 180.0
                    val x1 = cx + (innerR * cos(angle)).toFloat()
                    val y1 = cy + (innerR * sin(angle)).toFloat()
                    val x2 = cx + (outerR * cos(angle)).toFloat()
                    val y2 = cy + (outerR * sin(angle)).toFloat()
                    drawLine(tint, Offset(x1, y1), Offset(x2, y2), strokeWidth * 1.5f, cap = StrokeCap.Round)
                }
            }
        }
    }
}

/**
 * Individual action button in the bottom control bar.
 */
@Composable
fun ActionButton(
    icon: ActionIconType,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    isHighlighted: Boolean = false
) {
    val colors = SolitaireTheme.colors
    val contentColor = when {
        !isEnabled -> colors.onFeltSubtle.copy(alpha = 0.38f)
        isHighlighted -> colors.scoreGold
        else -> colors.onFeltText
    }
    val backgroundColor = when {
        isHighlighted -> colors.scoreGold.copy(alpha = 0.20f)
        else -> colors.tableSurface.copy(alpha = 0.65f)
    }
    val borderColor = when {
        isHighlighted -> colors.scoreGold.copy(alpha = 0.60f)
        else -> colors.slotBorder
    }

    Surface(
        onClick = onClick,
        enabled = isEnabled,
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.height(48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionIcon(
                type = icon,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = contentColor,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Bottom action control bar providing game action buttons:
 * Undo, Hint, New Game, and Settings.
 *
 * @param canUndo Whether undo action is currently available.
 * @param onUndoClick Callback when Undo is tapped.
 * @param onHintClick Callback when Hint is tapped.
 * @param onNewGameClick Callback when New Game is tapped.
 * @param onSettingsClick Callback when Settings is tapped.
 * @param modifier Compose [Modifier] applied to this bar.
 * @param isHintActive Whether a hint is currently actively highlighted.
 */
@Composable
fun BottomActionBarView(
    canUndo: Boolean,
    onUndoClick: () -> Unit,
    onHintClick: () -> Unit,
    onNewGameClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    isHintActive: Boolean = false
) {
    val dimensions = SolitaireTheme.cardDimensions

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimensions.horizontalPadding, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActionButton(
            icon = ActionIconType.UNDO,
            label = "Undo",
            isEnabled = canUndo,
            onClick = onUndoClick,
            modifier = Modifier.weight(1f)
        )
        ActionButton(
            icon = ActionIconType.HINT,
            label = "Hint",
            isHighlighted = isHintActive,
            onClick = onHintClick,
            modifier = Modifier.weight(1f)
        )
        ActionButton(
            icon = ActionIconType.NEW_GAME,
            label = "New",
            onClick = onNewGameClick,
            modifier = Modifier.weight(1f)
        )
        ActionButton(
            icon = ActionIconType.SETTINGS,
            label = "Settings",
            onClick = onSettingsClick,
            modifier = Modifier.weight(1f)
        )
    }
}
