package io.github.qdiaps.solitaire.ui.game.animation.victory

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.ui.game.components.displayLabel
import io.github.qdiaps.solitaire.ui.game.components.drawSuitEmblem
import io.github.qdiaps.solitaire.ui.theme.CardFaceStyle
import io.github.qdiaps.solitaire.ui.theme.SolitaireColors

/**
 * Interface responsible for rendering a single playing card face onto an [ImageBitmap].
 */
fun interface CardSpriteRenderer {
    /**
     * Renders [card] face onto an [ImageBitmap] with given dimensions and styling.
     */
    fun renderCard(
        card: Card,
        widthPx: Int,
        heightPx: Int,
        cardFaceStyle: CardFaceStyle,
        colors: SolitaireColors,
        density: Density
    ): ImageBitmap
}

/**
 * Default hardware-backed renderer utilizing Compose [CanvasDrawScope], vector [drawSuitEmblem],
 * and typography text layout.
 */
class DefaultCardSpriteRenderer(
    private val textMeasurer: TextMeasurer? = null
) : CardSpriteRenderer {

    override fun renderCard(
        card: Card,
        widthPx: Int,
        heightPx: Int,
        cardFaceStyle: CardFaceStyle,
        colors: SolitaireColors,
        density: Density
    ): ImageBitmap {
        val bitmap = ImageBitmap(widthPx, heightPx, ImageBitmapConfig.Argb8888)
        val canvas = Canvas(bitmap)
        val drawScope = CanvasDrawScope()

        drawScope.draw(
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = Size(widthPx.toFloat(), heightPx.toFloat())
        ) {
            val cornerRadiusPx = with(density) { 6.dp.toPx() }

            // 1. Draw solid rounded card background
            drawRoundRect(
                color = colors.cardBackground,
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                size = size
            )

            // 2. Draw card border
            val strokeWidthPx = with(density) { 1.dp.toPx() }
            drawRoundRect(
                color = colors.cardBorder,
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                style = Stroke(width = strokeWidthPx),
                size = size
            )

            val suitColor = if (card.suit.isRed) colors.cardRed else colors.cardBlack
            val cornerEmblemSize = size.width * 0.22f * cardFaceStyle.cornerEmblemScale
            val centerEmblemSize = size.width * 0.44f * cardFaceStyle.centerEmblemScale
            val indexPaddingH = size.width * 0.08f
            val indexPaddingV = size.height * 0.05f

            // 3. Center suit emblem
            val centerLeft = (size.width - centerEmblemSize) / 2f
            val centerTop = (size.height - centerEmblemSize) / 2f
            withTransform({
                translate(left = centerLeft, top = centerTop)
            }) {
                drawSuitEmblem(
                    suit = card.suit,
                    color = suitColor,
                    width = centerEmblemSize,
                    height = centerEmblemSize
                )
            }

            // 4. Corner indices (Rank + Suit Emblem)
            val rankTextHeight = if (textMeasurer != null) {
                val fontSizeSp = (14f * cardFaceStyle.rankTextScale).sp
                val textStyle = TextStyle(
                    color = suitColor,
                    fontSize = fontSizeSp,
                    fontWeight = cardFaceStyle.fontWeight,
                    fontFamily = cardFaceStyle.fontFamily
                )
                val textLayoutResult = textMeasurer.measure(
                    text = card.rank.displayLabel,
                    style = textStyle
                )

                // Top-left rank text
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(indexPaddingH, indexPaddingV)
                )

                // Bottom-right rank text (mirrored 180 degrees)
                withTransform({
                    rotate(180f, pivot = Offset(size.width / 2f, size.height / 2f))
                }) {
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(indexPaddingH, indexPaddingV)
                    )
                }

                textLayoutResult.size.height.toFloat()
            } else {
                cornerEmblemSize * 0.8f
            }

            // Top-left corner emblem (below rank text)
            withTransform({
                translate(left = indexPaddingH, top = indexPaddingV + rankTextHeight)
            }) {
                drawSuitEmblem(
                    suit = card.suit,
                    color = suitColor,
                    width = cornerEmblemSize,
                    height = cornerEmblemSize
                )
            }

            // Bottom-right corner emblem (mirrored 180 degrees)
            withTransform({
                rotate(180f, pivot = Offset(size.width / 2f, size.height / 2f))
                translate(left = indexPaddingH, top = indexPaddingV + rankTextHeight)
            }) {
                drawSuitEmblem(
                    suit = card.suit,
                    color = suitColor,
                    width = cornerEmblemSize,
                    height = cornerEmblemSize
                )
            }
        }

        return bitmap
    }
}
