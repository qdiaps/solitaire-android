package io.github.qdiaps.solitaire.ui.game.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.qdiaps.solitaire.ui.game.VictorySummary
import io.github.qdiaps.solitaire.ui.theme.SolitaireTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Celebratory modal dialog presented upon game completion.
 *
 * Displays:
 * - Triumphant header with vector golden trophy iconography.
 * - Highlight banner if personal lifetime records were broken.
 * - Core victory metrics grid: Time (with "BEST" record badge), Moves (with "FEWEST" record badge),
 *   and Score (with "RECORD" badge).
 * - Celebratory winning streak notification when current streak exceeds personal best.
 * - Action buttons: "New Game" (prominent primary CTA), "Play Again" (replays same deal),
 *   and "View Board" (dismisses dialog to inspect completed board).
 *
 * @param summary Final metrics and breakthrough records for the won game.
 * @param onNewGame Callback invoked to deal a fresh solvable board.
 * @param onPlayAgain Callback invoked to replay the current game deal from the beginning.
 * @param onDismiss Callback invoked to close the dialog and examine the final board.
 * @param modifier Modifier applied to the dialog container surface.
 */
@Composable
fun VictorySummaryDialog(
    summary: VictorySummary,
    onNewGame: () -> Unit,
    onPlayAgain: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SolitaireTheme.colors.tableSurface,
            border = BorderStroke(1.5.dp, SolitaireTheme.colors.scoreGold.copy(alpha = 0.65f)),
            modifier = modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Celebratory Icon: Golden Trophy
                VictoryTrophyIcon(
                    modifier = Modifier.size(56.dp),
                    tint = SolitaireTheme.colors.scoreGold
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Title: VICTORY!
                Text(
                    text = "VICTORY!",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    ),
                    color = SolitaireTheme.colors.scoreGold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Subtitle / Breakthrough Banner
                if (summary.hasAnyNewRecord) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SolitaireTheme.colors.scoreGold.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, SolitaireTheme.colors.scoreGold.copy(alpha = 0.60f))
                    ) {
                        Text(
                            text = "★ NEW RECORD ACHIEVED! ★",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = SolitaireTheme.colors.scoreGold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Text(
                        text = "Congratulations, you won!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SolitaireTheme.colors.onFeltSubtle,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section: Core Metrics Grid (Time, Moves, Score)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VictoryStatCard(
                        label = "TIME",
                        value = formatTime(summary.timeSeconds.toLong()),
                        badgeText = if (summary.isNewBestTime) "★ BEST" else null,
                        modifier = Modifier.weight(1f)
                    )
                    VictoryStatCard(
                        label = "MOVES",
                        value = summary.movesCount.toString(),
                        badgeText = if (summary.isNewFewestMoves) "★ FEWEST" else null,
                        modifier = Modifier.weight(1f)
                    )
                    VictoryStatCard(
                        label = "SCORE",
                        value = summary.score.toString(),
                        valueColor = SolitaireTheme.colors.scoreGold,
                        badgeText = if (summary.isNewHighScore) "★ RECORD" else null,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Section: Streak Breakthrough Highlight Banner (if applicable)
                if (summary.isNewBestStreak) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SolitaireTheme.colors.scoreGold.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, SolitaireTheme.colors.scoreGold.copy(alpha = 0.50f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "🔥 NEW BEST WIN STREAK ACHIEVED! 🔥",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = SolitaireTheme.colors.scoreGold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Primary CTA: New Game
                Button(
                    onClick = onNewGame,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SolitaireTheme.colors.scoreGold,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "New Game",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary CTAs: Play Again & View Board
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onPlayAgain,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SolitaireTheme.colors.slotBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SolitaireTheme.colors.onFeltText
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text(
                            text = "Play Again",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SolitaireTheme.colors.slotBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SolitaireTheme.colors.onFeltSubtle
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text(
                            text = "View Board",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Metric card container displaying a title label, formatted value, and optional record badge.
 */
@Composable
private fun VictoryStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = SolitaireTheme.colors.onFeltText,
    badgeText: String? = null
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SolitaireTheme.colors.tableSurface.copy(alpha = 0.85f),
        border = BorderStroke(
            width = if (badgeText != null) 1.5.dp else 1.dp,
            color = if (badgeText != null) SolitaireTheme.colors.scoreGold.copy(alpha = 0.70f) else SolitaireTheme.colors.slotBorder
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = SolitaireTheme.typography.statusLabel,
                color = SolitaireTheme.colors.onFeltSubtle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (badgeText != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SolitaireTheme.colors.scoreGold.copy(alpha = 0.20f),
                    border = BorderStroke(1.dp, SolitaireTheme.colors.scoreGold.copy(alpha = 0.70f))
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        ),
                        color = SolitaireTheme.colors.scoreGold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Scalable vector golden trophy icon with side handles, stem, rounded base, and star emblem.
 */
@Composable
fun VictoryTrophyIcon(
    modifier: Modifier = Modifier,
    tint: Color = SolitaireTheme.colors.scoreGold
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Trophy Cup Body
        val cupPath = Path().apply {
            moveTo(w * 0.28f, h * 0.18f)
            lineTo(w * 0.72f, h * 0.18f)
            cubicTo(
                w * 0.72f, h * 0.50f,
                w * 0.62f, h * 0.62f,
                w * 0.50f, h * 0.65f
            )
            cubicTo(
                w * 0.38f, h * 0.62f,
                w * 0.28f, h * 0.50f,
                w * 0.28f, h * 0.18f
            )
            close()
        }
        drawPath(cupPath, color = tint)

        // Left Handle
        val leftHandle = Path().apply {
            moveTo(w * 0.28f, h * 0.24f)
            cubicTo(w * 0.08f, h * 0.24f, w * 0.08f, h * 0.48f, w * 0.31f, h * 0.50f)
        }
        drawPath(leftHandle, color = tint, style = Stroke(width = w * 0.06f, cap = StrokeCap.Round))

        // Right Handle
        val rightHandle = Path().apply {
            moveTo(w * 0.72f, h * 0.24f)
            cubicTo(w * 0.92f, h * 0.24f, w * 0.92f, h * 0.48f, w * 0.69f, h * 0.50f)
        }
        drawPath(rightHandle, color = tint, style = Stroke(width = w * 0.06f, cap = StrokeCap.Round))

        // Stem
        drawLine(
            color = tint,
            start = Offset(w * 0.50f, h * 0.65f),
            end = Offset(w * 0.50f, h * 0.80f),
            strokeWidth = w * 0.08f,
            cap = StrokeCap.Round
        )

        // Base
        val baseCorner = CornerRadius(w * 0.03f, w * 0.03f)
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.30f, h * 0.80f),
            size = Size(w * 0.40f, h * 0.12f),
            cornerRadius = baseCorner
        )

        // Star accent on the cup
        val starCx = w * 0.50f
        val starCy = h * 0.36f
        val starR = w * 0.10f
        val starPath = Path()
        for (i in 0 until 5) {
            val outerAngle = (i * 72 - 90) * (PI / 180.0)
            val innerAngle = ((i * 72 + 36) - 90) * (PI / 180.0)
            val ox = (starCx + cos(outerAngle) * starR).toFloat()
            val oy = (starCy + sin(outerAngle) * starR).toFloat()
            val ix = (starCx + cos(innerAngle) * (starR * 0.42f)).toFloat()
            val iy = (starCy + sin(innerAngle) * (starR * 0.42f)).toFloat()
            if (i == 0) starPath.moveTo(ox, oy) else starPath.lineTo(ox, oy)
            starPath.lineTo(ix, iy)
        }
        starPath.close()
        drawPath(starPath, color = Color.White.copy(alpha = 0.92f))
    }
}
