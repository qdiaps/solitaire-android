package io.github.qdiaps.solitaire.ui.game.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.qdiaps.solitaire.domain.rules.DealDifficulty
import io.github.qdiaps.solitaire.BuildConfig
import io.github.qdiaps.solitaire.domain.rules.DrawMode
import io.github.qdiaps.solitaire.domain.solver.GeneratorDebugStats
import io.github.qdiaps.solitaire.domain.solver.WorkerLogEntry
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import io.github.qdiaps.solitaire.ui.theme.SolitaireColors
import io.github.qdiaps.solitaire.ui.theme.CardBackStyle
import io.github.qdiaps.solitaire.ui.theme.CardFaceStyle
import io.github.qdiaps.solitaire.ui.theme.FeltTheme
import io.github.qdiaps.solitaire.ui.theme.SolitaireTheme

/**
 * Modal bottom sheet presenting Klondike Solitaire gameplay, appearance, feedback, and developer settings.
 *
 * All setting changes take immediate effect in real time without restarting the current game session.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    drawMode: DrawMode,
    isLeftHanded: Boolean,
    autoHintEnabled: Boolean,
    feltTheme: FeltTheme,
    cardBackStyle: CardBackStyle,
    cardFaceStyle: CardFaceStyle,
    soundEnabled: Boolean,
    hapticsEnabled: Boolean,
    onDrawModeChange: (DrawMode) -> Unit,
    onLeftHandedChange: (Boolean) -> Unit,
    onAutoHintChange: (Boolean) -> Unit,
    onFeltThemeChange: (FeltTheme) -> Unit,
    onCardBackStyleChange: (CardBackStyle) -> Unit,
    onCardFaceStyleChange: (CardFaceStyle) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onResetToDefaults: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dealDifficulty: DealDifficulty = DealDifficulty.EASY,
    onDealDifficultyChange: (DealDifficulty) -> Unit = {},
    isDebug: Boolean = false,
    debugStats: GeneratorDebugStats = GeneratorDebugStats(),
    gameSessionId: Long = 1L,
    movesCount: Int = 0,
    score: Int = 0,
    elapsedTimeSeconds: Long = 0L,
    isGameWon: Boolean = false,
    isDeadlocked: Boolean = false,
    isAutoCompleteAvailable: Boolean = false,
    onDevInstantWin: () -> Unit = {},
    onDevStressRefill: () -> Unit = {},
    onDevExportSeeds: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = SolitaireTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.tableSurface,
        contentColor = colors.onFeltText,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = colors.onFeltSubtle.copy(alpha = 0.4f)
            )
        },
        modifier = modifier
    ) {
        SettingsSheetContent(
            drawMode = drawMode,
            isLeftHanded = isLeftHanded,
            autoHintEnabled = autoHintEnabled,
            feltTheme = feltTheme,
            cardBackStyle = cardBackStyle,
            cardFaceStyle = cardFaceStyle,
            soundEnabled = soundEnabled,
            hapticsEnabled = hapticsEnabled,
            onDrawModeChange = onDrawModeChange,
            onLeftHandedChange = onLeftHandedChange,
            onAutoHintChange = onAutoHintChange,
            onFeltThemeChange = onFeltThemeChange,
            onCardBackStyleChange = onCardBackStyleChange,
            onCardFaceStyleChange = onCardFaceStyleChange,
            onSoundChange = onSoundChange,
            onHapticsChange = onHapticsChange,
            onResetToDefaults = onResetToDefaults,
            onDismiss = onDismiss,
            dealDifficulty = dealDifficulty,
            onDealDifficultyChange = onDealDifficultyChange,
            isDebug = isDebug,
            debugStats = debugStats,
            gameSessionId = gameSessionId,
            movesCount = movesCount,
            score = score,
            elapsedTimeSeconds = elapsedTimeSeconds,
            isGameWon = isGameWon,
            isDeadlocked = isDeadlocked,
            isAutoCompleteAvailable = isAutoCompleteAvailable,
            onDevInstantWin = onDevInstantWin,
            onDevStressRefill = onDevStressRefill,
            onDevExportSeeds = onDevExportSeeds
        )
    }
}

/**
 * Core settings content list used both inside [SettingsBottomSheet] and standalone Compose previews.
 */
@Composable
fun SettingsSheetContent(
    drawMode: DrawMode,
    isLeftHanded: Boolean,
    autoHintEnabled: Boolean,
    feltTheme: FeltTheme,
    cardBackStyle: CardBackStyle,
    cardFaceStyle: CardFaceStyle,
    soundEnabled: Boolean,
    hapticsEnabled: Boolean,
    onDrawModeChange: (DrawMode) -> Unit,
    onLeftHandedChange: (Boolean) -> Unit,
    onAutoHintChange: (Boolean) -> Unit,
    onFeltThemeChange: (FeltTheme) -> Unit,
    onCardBackStyleChange: (CardBackStyle) -> Unit,
    onCardFaceStyleChange: (CardFaceStyle) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onResetToDefaults: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dealDifficulty: DealDifficulty = DealDifficulty.EASY,
    onDealDifficultyChange: (DealDifficulty) -> Unit = {},
    isDebug: Boolean = false,
    debugStats: GeneratorDebugStats = GeneratorDebugStats(),
    gameSessionId: Long = 1L,
    movesCount: Int = 0,
    score: Int = 0,
    elapsedTimeSeconds: Long = 0L,
    isGameWon: Boolean = false,
    isDeadlocked: Boolean = false,
    isAutoCompleteAvailable: Boolean = false,
    onDevInstantWin: () -> Unit = {},
    onDevStressRefill: () -> Unit = {},
    onDevExportSeeds: () -> Unit = {}
) {
    val colors = SolitaireTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
    ) {
        // Header: Title & Close Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.scoreGold
                )
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(36.dp)
            ) {
                CloseCanvasIcon(
                    tint = colors.onFeltSubtle,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // SECTION 1: GAMEPLAY
        SettingsSectionHeader(title = "GAMEPLAY")

        Spacer(modifier = Modifier.height(8.dp))

        // Deal Difficulty Segmented Control
        Text(
            text = "Deal Difficulty",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = colors.onFeltText
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = when (dealDifficulty) {
                DealDifficulty.EASY -> "100% winnable deals with high opening mobility"
                DealDifficulty.MEDIUM -> "100% winnable deals with standard challenge"
                DealDifficulty.RANDOM -> "Classic authentic random shuffle"
            },
            style = MaterialTheme.typography.bodySmall.copy(
                color = colors.onFeltSubtle
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        SegmentedChoiceRow(
            items = listOf(DealDifficulty.EASY, DealDifficulty.MEDIUM, DealDifficulty.RANDOM),
            selectedItem = dealDifficulty,
            onItemSelected = onDealDifficultyChange,
            labelProvider = { mode ->
                when (mode) {
                    DealDifficulty.EASY -> "Easy"
                    DealDifficulty.MEDIUM -> "Medium"
                    DealDifficulty.RANDOM -> "Random"
                }
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Draw Mode Segmented Control
        Text(
            text = "Draw Mode",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = colors.onFeltText
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        SegmentedChoiceRow(
            items = listOf(DrawMode.DRAW_ONE, DrawMode.DRAW_THREE),
            selectedItem = drawMode,
            onItemSelected = onDrawModeChange,
            labelProvider = { mode ->
                when (mode) {
                    DrawMode.DRAW_ONE -> "Draw 1 Card"
                    DrawMode.DRAW_THREE -> "Draw 3 Cards"
                }
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Left-Handed Toggle
        SettingsToggleRow(
            title = "Left-Handed Mode",
            subtitle = "Mirrors stock and foundation positions for easier left-hand reach",
            checked = isLeftHanded,
            onCheckedChange = onLeftHandedChange
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Auto-Hint Toggle
        SettingsToggleRow(
            title = "Auto-Hints",
            subtitle = "Automatically highlights available moves when idle",
            checked = autoHintEnabled,
            onCheckedChange = onAutoHintChange
        )

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = colors.slotBorder)
        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 2: APPEARANCE
        SettingsSectionHeader(title = "APPEARANCE")

        Spacer(modifier = Modifier.height(10.dp))

        // Felt Table Theme Selection
        Text(
            text = "Table Felt Cloth",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = colors.onFeltText
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        FeltThemeSelector(
            selectedTheme = feltTheme,
            onThemeSelected = onFeltThemeChange
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Card Back Style Selection
        Text(
            text = "Card Back Style",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = colors.onFeltText
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        CardBackSelector(
            selectedStyle = cardBackStyle,
            onStyleSelected = onCardBackStyleChange
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Card Face Style Selection
        Text(
            text = "Card Face Style",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = colors.onFeltText
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        SegmentedChoiceRow(
            items = listOf(CardFaceStyle.MODERN_CLEAN),
            selectedItem = cardFaceStyle,
            onItemSelected = onCardFaceStyleChange,
            labelProvider = { it.displayName }
        )

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = colors.slotBorder)
        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 3: FEEDBACK
        SettingsSectionHeader(title = "FEEDBACK")

        Spacer(modifier = Modifier.height(10.dp))

        // Sound Effects Toggle
        SettingsToggleRow(
            title = "Sound Effects",
            subtitle = "Play card slide, flip, snap, and deal audio effects",
            checked = soundEnabled,
            onCheckedChange = onSoundChange
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Haptic Feedback Toggle
        SettingsToggleRow(
            title = "Haptic Feedback",
            subtitle = "Tactile vibration pulses on valid moves and drops",
            checked = hapticsEnabled,
            onCheckedChange = onHapticsChange
        )

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = colors.slotBorder)
        Spacer(modifier = Modifier.height(20.dp))

        // SECTION 4: RESET TO DEFAULTS
        Surface(
            onClick = onResetToDefaults,
            shape = RoundedCornerShape(12.dp),
            color = colors.cardRed.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, colors.cardRed.copy(alpha = 0.40f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "Reset Settings to Defaults",
                    color = colors.cardRed,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        // SECTION 5: DEVELOPER / DEBUG TOOLS (Exclusive to DEBUG build)
        if (isDebug) {
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = colors.slotBorder)
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsSectionHeader(title = "DEVELOPER / DEBUG TOOLS")
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = colors.scoreGold.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, colors.scoreGold.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "DEBUG BUILD",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.scoreGold,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Telemetry Panel
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.slotBackground,
                border = BorderStroke(1.dp, colors.slotBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Seed Bank & Solver Telemetry",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.scoreGold
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    TelemetryItem(
                        label = "Seed Bank Pools",
                        value = "Easy [${debugStats.easyBankCount}/100] • Medium [${debugStats.mediumBankCount}/100]"
                    )
                    TelemetryItem(
                        label = "Replenishment Workers",
                        value = if (debugStats.activeWorkersCount > 0) {
                            "Refilling (${debugStats.activeWorkersCount} active)"
                        } else {
                            "Idle (0% CPU)"
                        }
                    )
                    TelemetryItem(
                        label = "Candidate Solvability",
                        value = "${debugStats.totalSolvableFound} / ${debugStats.totalCandidatesEvaluated} checked (${(debugStats.rejectionRate * 100).toInt()}% rejected)"
                    )
                    TelemetryItem(
                        label = "Last Solve Duration",
                        value = "${debugStats.lastSolveDurationMs} ms"
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = colors.slotBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Active Session Diagnostics",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.scoreGold
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    TelemetryItem(
                        label = "Session Info",
                        value = "#$gameSessionId • $movesCount moves • $score pts • ${elapsedTimeSeconds}s"
                    )
                    TelemetryItem(
                        label = "Game State Flags",
                        value = "Won: $isGameWon • Deadlocked: $isDeadlocked • AutoComplete: $isAutoCompleteAvailable"
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Worker Discovery Log Panel
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.slotBackground,
                border = BorderStroke(1.dp, colors.slotBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Worker Discovery Log",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.scoreGold
                            )
                        )
                        if (debugStats.recentWorkerLogs.isNotEmpty()) {
                            Text(
                                text = "${debugStats.recentWorkerLogs.size} recent",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = colors.onFeltSubtle,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (debugStats.recentWorkerLogs.isEmpty()) {
                        Text(
                            text = "No worker discoveries recorded yet (Bank idle)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = colors.onFeltSubtle,
                                fontStyle = FontStyle.Italic,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        debugStats.recentWorkerLogs.take(10).forEach { entry ->
                            WorkerLogRow(entry = entry, colors = colors)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action: Instant Win
            Surface(
                onClick = onDevInstantWin,
                shape = RoundedCornerShape(10.dp),
                color = colors.scoreGold.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, colors.scoreGold.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "⚡ Force Instant Win (Foundation Complete)",
                        color = colors.scoreGold,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action: Stress Refill / Flush 90%
            Surface(
                onClick = onDevStressRefill,
                shape = RoundedCornerShape(10.dp),
                color = colors.cardRed.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, colors.cardRed.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "🔥 Flush 90% Seeds (Trigger Deep Refill)",
                        color = colors.cardRed,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action: View / Export Seeds
            Surface(
                onClick = onDevExportSeeds,
                shape = RoundedCornerShape(10.dp),
                color = colors.tableSurface,
                border = BorderStroke(1.dp, colors.slotBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "📋 Export Seed Bank to Clipboard",
                        color = colors.onFeltText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun WorkerLogRow(
    entry: WorkerLogEntry,
    colors: SolitaireColors
) {
    val timeFormatted = remember(entry.timestampMs) {
        if (entry.timestampMs > 0L) {
            val date = java.util.Date(entry.timestampMs)
            val format = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            format.format(date)
        } else {
            "--:--:--"
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(colors.tableBackground.copy(alpha = 0.4f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "[$timeFormatted]",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = colors.onFeltSubtle
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "W#${entry.workerId}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = colors.scoreGold
                )
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "➔",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = colors.onFeltSubtle
                )
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (entry.difficulty == DealDifficulty.EASY) "Easy" else "Medium",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = if (entry.difficulty == DealDifficulty.EASY) Color(0xFF81C784) else Color(0xFFFFB74D)
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "#${entry.seed}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = colors.onFeltText
                )
            )
        }
        Text(
            text = "${entry.durationMs}ms",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = colors.onFeltSubtle
            )
        )
    }
}

@Composable
private fun TelemetryItem(label: String, value: String) {
    val colors = SolitaireTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = colors.onFeltSubtle,
                fontSize = 11.sp
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                color = colors.onFeltText,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp
            )
        )
    }
}

/**
 * Uppercase section header with golden accent typography.
 */
@Composable
private fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = SolitaireTheme.colors.scoreGold
        ),
        modifier = modifier
    )
}

/**
 * Toggle row containing title, descriptive subtitle, and Material Switch.
 */
@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SolitaireTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onFeltText
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = colors.onFeltSubtle
                )
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.scoreGold,
                checkedTrackColor = colors.scoreGold.copy(alpha = 0.35f),
                checkedBorderColor = colors.scoreGold,
                uncheckedThumbColor = colors.onFeltSubtle,
                uncheckedTrackColor = colors.slotBackground,
                uncheckedBorderColor = colors.slotBorder
            )
        )
    }
}

/**
 * Horizontal row of selectable Felt Cloth theme swatches.
 */
@Composable
private fun FeltThemeSelector(
    selectedTheme: FeltTheme,
    onThemeSelected: (FeltTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    val themes = FeltTheme.entries
    val colors = SolitaireTheme.colors

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        themes.forEach { theme ->
            val isSelected = theme == selectedTheme
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onThemeSelected(theme) }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(theme.primaryColor)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) colors.scoreGold else colors.slotBorder,
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    if (isSelected) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0x80000000))
                        ) {
                            CheckmarkCanvasIcon(
                                tint = colors.scoreGold,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = theme.displayName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) colors.scoreGold else colors.onFeltSubtle,
                        fontSize = 10.sp
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Horizontal row of selectable Card Back miniature previews.
 */
@Composable
private fun CardBackSelector(
    selectedStyle: CardBackStyle,
    onStyleSelected: (CardBackStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    val styles = CardBackStyle.entries
    val colors = SolitaireTheme.colors

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        styles.forEach { style ->
            val isSelected = style == selectedStyle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onStyleSelected(style) }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) colors.scoreGold else colors.slotBorder,
                            shape = RoundedCornerShape(6.dp)
                        )
                ) {
                    CardBackView(
                        style = style,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isSelected) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xB3000000))
                        ) {
                            CheckmarkCanvasIcon(
                                tint = colors.scoreGold,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = style.displayName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) colors.scoreGold else colors.onFeltSubtle,
                        fontSize = 10.sp
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}

/**
 * Segmented selection row matching card-felt aesthetic.
 */
@Composable
private fun <T> SegmentedChoiceRow(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    labelProvider: (T) -> String,
    modifier: Modifier = Modifier
) {
    val colors = SolitaireTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.slotBackground)
            .border(1.dp, colors.slotBorder, RoundedCornerShape(10.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEach { item ->
            val isSelected = item == selectedItem
            Surface(
                onClick = { onItemSelected(item) },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) colors.scoreGold.copy(alpha = 0.22f) else Color.Transparent,
                border = if (isSelected) BorderStroke(1.dp, colors.scoreGold) else null,
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = labelProvider(item),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) colors.scoreGold else colors.onFeltText
                        )
                    )
                }
            }
        }
    }
}

/**
 * Clean Canvas-drawn checkmark vector icon.
 */
@Composable
private fun CheckmarkCanvasIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.18f, h * 0.52f)
            lineTo(w * 0.42f, h * 0.78f)
            lineTo(w * 0.82f, h * 0.22f)
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

/**
 * Clean Canvas-drawn close (X) vector icon.
 */
@Composable
private fun CloseCanvasIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 2.dp.toPx()
        drawLine(
            color = tint,
            start = Offset(w * 0.22f, h * 0.22f),
            end = Offset(w * 0.78f, h * 0.78f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.78f, h * 0.22f),
            end = Offset(w * 0.22f, h * 0.78f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}
