package io.github.qdiaps.solitaire.ui.game.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import io.github.qdiaps.solitaire.domain.model.Suit
import io.github.qdiaps.solitaire.ui.theme.SolitaireTheme

/**
 * Draws crisp, resolution-independent vector emblems for playing card suits.
 */
@Composable
fun SuitEmblem(
    suit: Suit,
    modifier: Modifier = Modifier,
    color: Color? = null
) {
    val defaultColor = if (suit.isRed) {
        SolitaireTheme.colors.cardRed
    } else {
        SolitaireTheme.colors.cardBlack
    }
    val effectiveColor = color ?: defaultColor

    Canvas(modifier = modifier) {
        drawSuitEmblem(
            suit = suit,
            color = effectiveColor,
            width = size.width,
            height = size.height
        )
    }
}

/**
 * DrawScope extension function to render vector suit geometry onto a Canvas.
 */
fun DrawScope.drawSuitEmblem(
    suit: Suit,
    color: Color,
    width: Float,
    height: Float
) {
    val path = Path().apply {
        when (suit) {
            Suit.HEARTS -> {
                moveTo(width * 0.5f, height * 0.25f)
                cubicTo(
                    width * 0.40f, 0f,
                    0f, 0f,
                    0f, height * 0.38f
                )
                cubicTo(
                    0f, height * 0.65f,
                    width * 0.35f, height * 0.82f,
                    width * 0.5f, height
                )
                cubicTo(
                    width * 0.65f, height * 0.82f,
                    width, height * 0.65f,
                    width, height * 0.38f
                )
                cubicTo(
                    width, 0f,
                    width * 0.60f, 0f,
                    width * 0.5f, height * 0.25f
                )
                close()
            }
            Suit.DIAMONDS -> {
                moveTo(width * 0.5f, 0f)
                lineTo(width, height * 0.5f)
                lineTo(width * 0.5f, height)
                lineTo(0f, height * 0.5f)
                close()
            }
            Suit.CLUBS -> {
                val r = width * 0.22f
                addOval(Rect(width * 0.5f - r, height * 0.26f - r, width * 0.5f + r, height * 0.26f + r))
                addOval(Rect(width * 0.28f - r, height * 0.56f - r, width * 0.28f + r, height * 0.56f + r))
                addOval(Rect(width * 0.72f - r, height * 0.56f - r, width * 0.72f + r, height * 0.56f + r))
                addOval(Rect(width * 0.5f - r * 0.7f, height * 0.44f - r * 0.7f, width * 0.5f + r * 0.7f, height * 0.44f + r * 0.7f))
                moveTo(width * 0.48f, height * 0.54f)
                lineTo(width * 0.36f, height)
                lineTo(width * 0.64f, height)
                lineTo(width * 0.52f, height * 0.54f)
                close()
            }
            Suit.SPADES -> {
                moveTo(width * 0.5f, 0f)
                cubicTo(
                    width * 0.40f, height * 0.26f,
                    0f, height * 0.30f,
                    0f, height * 0.60f
                )
                cubicTo(
                    0f, height * 0.76f,
                    width * 0.26f, height * 0.82f,
                    width * 0.44f, height * 0.72f
                )
                lineTo(width * 0.38f, height)
                lineTo(width * 0.62f, height)
                lineTo(width * 0.56f, height * 0.72f)
                cubicTo(
                    width * 0.74f, height * 0.82f,
                    width, height * 0.76f,
                    width, height * 0.60f
                )
                cubicTo(
                    width, height * 0.30f,
                    width * 0.60f, height * 0.26f,
                    width * 0.5f, 0f
                )
                close()
            }
        }
    }
    drawPath(path = path, color = color)
}
