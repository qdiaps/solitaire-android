package io.github.qdiaps.solitaire.ui.game.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.qdiaps.solitaire.data.model.GameStats
import io.github.qdiaps.solitaire.ui.theme.SolitaireTheme

/**
 * Modal dialog presenting comprehensive player gameplay statistics and lifetime records.
 *
 * Displays formatted overview metrics (Games Played, Games Won, Win Rate Percentage),
 * consecutive win streaks (Current and Best), and personal records (Best Time in "mm:ss",
 * Fewest Moves, and High Score). Also provides an action to reset statistics with a confirmation dialog.
 *
 * @param stats Player statistics snapshot to render.
 * @param onResetStats Callback invoked when the user confirms statistics reset.
 * @param onDismiss Callback invoked when the dialog is closed or dismissed.
 * @param modifier Modifier applied to the dialog container.
 */
@Composable
fun StatsDialog(
    stats: GameStats,
    onResetStats: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showResetConfirmation by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SolitaireTheme.colors.tableSurface,
            border = BorderStroke(1.dp, SolitaireTheme.colors.slotBorder),
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
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ActionIcon(
                        type = ActionIconType.STATS,
                        tint = SolitaireTheme.colors.scoreGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "STATISTICS",
                        style = SolitaireTheme.typography.statusCounter.copy(fontWeight = FontWeight.Bold),
                        color = SolitaireTheme.colors.onFeltText,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section 1: Overview (Played, Won, Win %)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        label = "PLAYED",
                        value = stats.gamesPlayed.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "WON",
                        value = stats.gamesWon.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "WIN RATE",
                        value = "${stats.winRatePercent}%",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Section 2: Streaks (Current Streak, Best Streak)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        label = "CURRENT STREAK",
                        value = stats.currentStreak.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "BEST STREAK",
                        value = stats.bestStreak.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Section 3: Records (Best Time, Fewest Moves, High Score)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        label = "BEST TIME",
                        value = stats.bestTimeSeconds?.let { formatTime(it.toLong()) } ?: "--:--",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "FEWEST MOVES",
                        value = stats.fewestMoves?.toString() ?: "-",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "HIGH SCORE",
                        value = stats.highScore.toString(),
                        valueColor = SolitaireTheme.colors.scoreGold,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons: Done & Reset Statistics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showResetConfirmation = true },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.60f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text(
                            text = "Reset",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SolitaireTheme.colors.scoreGold,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text(
                            text = "Done",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = {
                Text(
                    text = "Reset Statistics?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SolitaireTheme.colors.onFeltText
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to reset all your gameplay statistics and records? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SolitaireTheme.colors.onFeltSubtle
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetStats()
                        showResetConfirmation = false
                    }
                ) {
                    Text(
                        text = "Reset",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text(
                        text = "Cancel",
                        color = SolitaireTheme.colors.onFeltText
                    )
                }
            },
            containerColor = SolitaireTheme.colors.tableSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

/**
 * Metric card container displaying a uppercase title label and large formatted value.
 */
@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = SolitaireTheme.colors.onFeltText
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SolitaireTheme.colors.tableSurface.copy(alpha = 0.70f),
        border = BorderStroke(1.dp, SolitaireTheme.colors.slotBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
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
        }
    }
}
