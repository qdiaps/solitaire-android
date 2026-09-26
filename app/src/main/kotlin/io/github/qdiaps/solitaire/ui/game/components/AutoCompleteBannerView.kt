package io.github.qdiaps.solitaire.ui.game.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.qdiaps.solitaire.ui.theme.SolitaireTheme

/**
 * Animated pill banner / button prompting the player that all remaining cards
 * can be automatically resolved to the foundations.
 *
 * Appears when [isVisible] is true (i.e. stock and waste are empty, and all tableau cards are face-up).
 *
 * @param isVisible Whether auto-complete condition is met.
 * @param onClick Callback when the player taps the banner.
 * @param modifier Compose [Modifier] applied to the banner container.
 */
@Composable
fun AutoCompleteBannerView(
    isVisible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
        modifier = modifier
    ) {
        val colors = SolitaireTheme.colors
        val accentGold = colors.scoreGold
        val backgroundColor = colors.tableSurface.copy(alpha = 0.92f)

        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(20.dp),
            color = backgroundColor,
            border = BorderStroke(1.5.dp, accentGold),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .height(40.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AutoCompleteIcon(
                    tint = accentGold,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Auto Complete",
                    color = accentGold,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

/**
 * Canvas vector icon representing fast-forward / auto cascade (two right-pointing chevrons).
 */
@Composable
fun AutoCompleteIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val chevronWidth = w * 0.42f
        val gap = w * 0.16f

        // First chevron (left)
        val path1 = Path().apply {
            moveTo(0f, 0f)
            lineTo(chevronWidth, h * 0.5f)
            lineTo(0f, h)
            lineTo(chevronWidth * 0.4f, h * 0.5f)
            close()
        }
        drawPath(path1, tint, style = Fill)

        // Second chevron (right)
        val startX2 = chevronWidth * 0.4f + gap
        val path2 = Path().apply {
            moveTo(startX2, 0f)
            lineTo(startX2 + chevronWidth, h * 0.5f)
            lineTo(startX2, h)
            lineTo(startX2 + chevronWidth * 0.4f, h * 0.5f)
            close()
        }
        drawPath(path2, tint, style = Fill)
    }
}
