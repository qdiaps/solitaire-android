package io.github.qdiaps.solitaire.ui.game.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import io.github.qdiaps.solitaire.ui.theme.CardBackStyle
import io.github.qdiaps.solitaire.ui.theme.SolitaireTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders the back face of a playing card according to the selected [CardBackStyle].
 *
 * All card back patterns are drawn as pure resolution-independent vector Canvas primitives
 * with zero bitmap assets.
 *
 * @param style Visual style specifying primary color, pattern color, and geometric theme.
 * @param modifier Modifier applied to the card back container.
 */
@Composable
fun CardBackView(
    style: CardBackStyle = SolitaireTheme.cardBackStyle,
    modifier: Modifier = Modifier
) {
    val dimensions = SolitaireTheme.cardDimensions
    val cornerRadiusPx = dimensions.cornerRadius.value

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(style.primaryColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            when (style) {
                CardBackStyle.CLASSIC_LATTICE -> drawClassicLattice(
                    primaryColor = style.primaryColor,
                    patternColor = style.patternColor,
                    cornerRadius = cornerRadiusPx * density
                )
                CardBackStyle.CRIMSON_VINTAGE -> drawCrimsonVintage(
                    primaryColor = style.primaryColor,
                    patternColor = style.patternColor,
                    cornerRadius = cornerRadiusPx * density
                )
                CardBackStyle.EMERALD_ART_DECO -> drawEmeraldArtDeco(
                    primaryColor = style.primaryColor,
                    patternColor = style.patternColor
                )
                CardBackStyle.OBSIDIAN_MINIMAL -> drawObsidianMinimal(
                    primaryColor = style.primaryColor,
                    patternColor = style.patternColor,
                    cornerRadius = cornerRadiusPx * density
                )
            }
        }
    }
}

/**
 * Classic diagonal crosshatch diamond lattice pattern with outer rounded border.
 */
private fun DrawScope.drawClassicLattice(
    primaryColor: Color,
    patternColor: Color,
    cornerRadius: Float
) {
    val inset = size.width * 0.08f
    val corner = cornerRadius * 0.6f

    // Outer ornamental border
    drawRoundRect(
        color = patternColor.copy(alpha = 0.55f),
        topLeft = Offset(inset, inset),
        size = Size(size.width - inset * 2f, size.height - inset * 2f),
        cornerRadius = CornerRadius(corner, corner),
        style = Stroke(width = 1.dp.toPx())
    )

    // Inner hairline frame
    val innerInset = inset * 1.35f
    val innerWidth = size.width - innerInset * 2f
    val innerHeight = size.height - innerInset * 2f
    drawRoundRect(
        color = patternColor.copy(alpha = 0.35f),
        topLeft = Offset(innerInset, innerInset),
        size = Size(innerWidth, innerHeight),
        cornerRadius = CornerRadius(corner * 0.7f, corner * 0.7f),
        style = Stroke(width = 0.6.dp.toPx())
    )

    // Diamond crosshatch lines clipped neatly inside inner frame
    clipRect(
        left = innerInset,
        top = innerInset,
        right = innerInset + innerWidth,
        bottom = innerInset + innerHeight
    ) {
        val step = innerWidth / 4.2f
        val stroke = Stroke(width = 0.8.dp.toPx())
        val latticeColor = patternColor.copy(alpha = 0.28f)

        // Diagonal lines (\ direction)
        var x = -innerHeight
        while (x < innerWidth + innerHeight) {
            drawLine(
                color = latticeColor,
                start = Offset(innerInset + x, innerInset),
                end = Offset(innerInset + x + innerHeight, innerInset + innerHeight),
                strokeWidth = stroke.width
            )
            x += step
        }

        // Diagonal lines (/ direction)
        x = -innerHeight
        while (x < innerWidth + innerHeight) {
            drawLine(
                color = latticeColor,
                start = Offset(innerInset + x + innerHeight, innerInset),
                end = Offset(innerInset + x, innerInset + innerHeight),
                strokeWidth = stroke.width
            )
            x += step
        }
    }
}

/**
 * Crimson vintage pattern featuring double ornate borders and a central radiating rosette.
 */
private fun DrawScope.drawCrimsonVintage(
    primaryColor: Color,
    patternColor: Color,
    cornerRadius: Float
) {
    val inset1 = size.width * 0.07f
    val corner1 = cornerRadius * 0.6f
    val strokeWidth1 = 1.2.dp.toPx()
    val strokeWidth2 = 0.8.dp.toPx()

    // Outer border
    drawRoundRect(
        color = patternColor.copy(alpha = 0.65f),
        topLeft = Offset(inset1, inset1),
        size = Size(size.width - inset1 * 2f, size.height - inset1 * 2f),
        cornerRadius = CornerRadius(corner1, corner1),
        style = Stroke(width = strokeWidth1)
    )

    // Inner border
    val inset2 = size.width * 0.12f
    val corner2 = cornerRadius * 0.4f
    drawRoundRect(
        color = patternColor.copy(alpha = 0.45f),
        topLeft = Offset(inset2, inset2),
        size = Size(size.width - inset2 * 2f, size.height - inset2 * 2f),
        cornerRadius = CornerRadius(corner2, corner2),
        style = Stroke(width = strokeWidth2)
    )

    // Corner decorative accents between borders
    val cornerAccentColor = patternColor.copy(alpha = 0.5f)
    val accentLen = size.width * 0.06f
    // Top-left
    drawLine(cornerAccentColor, Offset(inset1, inset1 + accentLen), Offset(inset2, inset2), strokeWidth2)
    drawLine(cornerAccentColor, Offset(inset1 + accentLen, inset1), Offset(inset2, inset2), strokeWidth2)
    // Top-right
    val right1 = size.width - inset1
    val right2 = size.width - inset2
    drawLine(cornerAccentColor, Offset(right1, inset1 + accentLen), Offset(right2, inset2), strokeWidth2)
    drawLine(cornerAccentColor, Offset(right1 - accentLen, inset1), Offset(right2, inset2), strokeWidth2)
    // Bottom-left
    val bottom1 = size.height - inset1
    val bottom2 = size.height - inset2
    drawLine(cornerAccentColor, Offset(inset1, bottom1 - accentLen), Offset(inset2, bottom2), strokeWidth2)
    drawLine(cornerAccentColor, Offset(inset1 + accentLen, bottom1), Offset(inset2, bottom2), strokeWidth2)
    // Bottom-right
    drawLine(cornerAccentColor, Offset(right1, bottom1 - accentLen), Offset(right2, bottom2), strokeWidth2)
    drawLine(cornerAccentColor, Offset(right1 - accentLen, bottom1), Offset(right2, bottom2), strokeWidth2)

    // Central Rosette Medallion
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val center = Offset(centerX, centerY)
    val radius = size.width * 0.22f

    // Concentric medallion circles
    drawCircle(
        color = patternColor.copy(alpha = 0.6f),
        radius = radius,
        center = center,
        style = Stroke(width = strokeWidth1)
    )
    drawCircle(
        color = patternColor.copy(alpha = 0.4f),
        radius = radius * 0.6f,
        center = center,
        style = Stroke(width = strokeWidth2)
    )
    drawCircle(
        color = patternColor.copy(alpha = 0.7f),
        radius = radius * 0.2f,
        center = center
    )

    // 8-point radiating floral spokes
    val spokeColor = patternColor.copy(alpha = 0.55f)
    for (i in 0 until 8) {
        val angle = i * (PI / 4.0)
        val startX = centerX + (cos(angle) * radius * 0.22).toFloat()
        val startY = centerY + (sin(angle) * radius * 0.22).toFloat()
        val endX = centerX + (cos(angle) * radius * 0.95).toFloat()
        val endY = centerY + (sin(angle) * radius * 0.95).toFloat()

        drawLine(spokeColor, Offset(startX, startY), Offset(endX, endY), strokeWidth2)

        // Outer dot on each spoke tip
        drawCircle(
            color = patternColor.copy(alpha = 0.65f),
            radius = 1.2.dp.toPx(),
            center = Offset(endX, endY)
        )
    }

    // Top and bottom fleur florets
    val floretColor = patternColor.copy(alpha = 0.45f)
    val floretOffset = radius * 1.55f
    // Top floret diamond
    drawDiamond(
        center = Offset(centerX, centerY - floretOffset),
        width = size.width * 0.08f,
        height = size.width * 0.08f,
        color = floretColor,
        strokeWidth = strokeWidth2
    )
    // Bottom floret diamond
    drawDiamond(
        center = Offset(centerX, centerY + floretOffset),
        width = size.width * 0.08f,
        height = size.width * 0.08f,
        color = floretColor,
        strokeWidth = strokeWidth2
    )
}

/**
 * Emerald Art Deco pattern with chamfered geometric borders and sunburst ray fans.
 */
private fun DrawScope.drawEmeraldArtDeco(
    primaryColor: Color,
    patternColor: Color
) {
    val inset1 = size.width * 0.08f
    val bevel1 = size.width * 0.09f
    val strokeWidth1 = 1.2.dp.toPx()
    val strokeWidth2 = 0.8.dp.toPx()

    // Outer chamfered (beveled) polygon border
    val outerPath = Path().apply {
        moveTo(inset1 + bevel1, inset1)
        lineTo(size.width - inset1 - bevel1, inset1)
        lineTo(size.width - inset1, inset1 + bevel1)
        lineTo(size.width - inset1, size.height - inset1 - bevel1)
        lineTo(size.width - inset1 - bevel1, size.height - inset1)
        lineTo(inset1 + bevel1, size.height - inset1)
        lineTo(inset1, size.height - inset1 - bevel1)
        lineTo(inset1, inset1 + bevel1)
        close()
    }
    drawPath(outerPath, color = patternColor.copy(alpha = 0.65f), style = Stroke(width = strokeWidth1))

    // Inner chamfered polygon border
    val inset2 = size.width * 0.13f
    val bevel2 = size.width * 0.06f
    val innerPath = Path().apply {
        moveTo(inset2 + bevel2, inset2)
        lineTo(size.width - inset2 - bevel2, inset2)
        lineTo(size.width - inset2, inset2 + bevel2)
        lineTo(size.width - inset2, size.height - inset2 - bevel2)
        lineTo(size.width - inset2 - bevel2, size.height - inset2)
        lineTo(inset2 + bevel2, size.height - inset2)
        lineTo(inset2, size.height - inset2 - bevel2)
        lineTo(inset2, inset2 + bevel2)
        close()
    }
    drawPath(innerPath, color = patternColor.copy(alpha = 0.4f), style = Stroke(width = strokeWidth2))

    // Central Art Deco elongated diamond
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val diamondW = size.width * 0.22f
    val diamondH = size.height * 0.22f

    drawDiamond(
        center = Offset(centerX, centerY),
        width = diamondW,
        height = diamondH,
        color = patternColor.copy(alpha = 0.7f),
        strokeWidth = strokeWidth1
    )
    drawDiamond(
        center = Offset(centerX, centerY),
        width = diamondW * 0.6f,
        height = diamondH * 0.6f,
        color = patternColor.copy(alpha = 0.5f),
        strokeWidth = strokeWidth2
    )
    drawDiamond(
        center = Offset(centerX, centerY),
        width = diamondW * 0.25f,
        height = diamondH * 0.25f,
        color = patternColor.copy(alpha = 0.8f),
        filled = true
    )

    // Art Deco radiating chevron/ray fan lines (top and bottom)
    val rayColor = patternColor.copy(alpha = 0.35f)
    val rayCount = 5
    for (i in -rayCount..rayCount) {
        val dx = (i.toFloat() / rayCount) * (size.width * 0.25f)
        // Top fan
        drawLine(
            color = rayColor,
            start = Offset(centerX, centerY - diamondH * 0.85f),
            end = Offset(centerX + dx, inset2 + strokeWidth2),
            strokeWidth = strokeWidth2
        )
        // Bottom fan
        drawLine(
            color = rayColor,
            start = Offset(centerX, centerY + diamondH * 0.85f),
            end = Offset(centerX + dx, size.height - inset2 - strokeWidth2),
            strokeWidth = strokeWidth2
        )
    }
}

/**
 * Obsidian Minimal pattern featuring triple concentric hairline frames and fine center insignia.
 */
private fun DrawScope.drawObsidianMinimal(
    primaryColor: Color,
    patternColor: Color,
    cornerRadius: Float
) {
    val strokeWidth = 0.8.dp.toPx()
    val frame1Inset = size.width * 0.08f
    val frame2Inset = size.width * 0.14f
    val frame3Inset = size.width * 0.20f

    // Frame 1
    drawRoundRect(
        color = patternColor.copy(alpha = 0.6f),
        topLeft = Offset(frame1Inset, frame1Inset),
        size = Size(size.width - frame1Inset * 2f, size.height - frame1Inset * 2f),
        cornerRadius = CornerRadius(cornerRadius * 0.65f, cornerRadius * 0.65f),
        style = Stroke(width = 1.dp.toPx())
    )

    // Frame 2
    drawRoundRect(
        color = patternColor.copy(alpha = 0.35f),
        topLeft = Offset(frame2Inset, frame2Inset),
        size = Size(size.width - frame2Inset * 2f, size.height - frame2Inset * 2f),
        cornerRadius = CornerRadius(cornerRadius * 0.45f, cornerRadius * 0.45f),
        style = Stroke(width = strokeWidth)
    )

    // Frame 3
    drawRoundRect(
        color = patternColor.copy(alpha = 0.22f),
        topLeft = Offset(frame3Inset, frame3Inset),
        size = Size(size.width - frame3Inset * 2f, size.height - frame3Inset * 2f),
        cornerRadius = CornerRadius(cornerRadius * 0.30f, cornerRadius * 0.30f),
        style = Stroke(width = strokeWidth)
    )

    // Modernist center insignia: subtle diamond with micro crosshair
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val diamondSize = size.width * 0.12f

    drawDiamond(
        center = Offset(centerX, centerY),
        width = diamondSize,
        height = diamondSize,
        color = patternColor.copy(alpha = 0.6f),
        strokeWidth = strokeWidth
    )

    // Central micro-dot
    drawCircle(
        color = patternColor.copy(alpha = 0.75f),
        radius = 1.5.dp.toPx(),
        center = Offset(centerX, centerY)
    )

    // Subtle crosshairs extending from diamond points
    val crosshairLen = size.width * 0.05f
    val crosshairColor = patternColor.copy(alpha = 0.4f)
    // Left & Right
    drawLine(crosshairColor, Offset(centerX - diamondSize / 2f - crosshairLen, centerY), Offset(centerX - diamondSize / 2f, centerY), strokeWidth)
    drawLine(crosshairColor, Offset(centerX + diamondSize / 2f, centerY), Offset(centerX + diamondSize / 2f + crosshairLen, centerY), strokeWidth)
    // Top & Bottom
    drawLine(crosshairColor, Offset(centerX, centerY - diamondSize / 2f - crosshairLen), Offset(centerX, centerY - diamondSize / 2f), strokeWidth)
    drawLine(crosshairColor, Offset(centerX, centerY + diamondSize / 2f), Offset(centerX, centerY + diamondSize / 2f + crosshairLen), strokeWidth)
}

/**
 * Utility helper to draw an aligned diamond shape.
 */
private fun DrawScope.drawDiamond(
    center: Offset,
    width: Float,
    height: Float,
    color: Color,
    strokeWidth: Float = 1f,
    filled: Boolean = false
) {
    val halfW = width / 2f
    val halfH = height / 2f
    val path = Path().apply {
        moveTo(center.x, center.y - halfH)
        lineTo(center.x + halfW, center.y)
        lineTo(center.x, center.y + halfH)
        lineTo(center.x - halfW, center.y)
        close()
    }
    if (filled) {
        drawPath(path, color = color)
    } else {
        drawPath(path, color = color, style = Stroke(width = strokeWidth))
    }
}
