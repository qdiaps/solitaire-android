package io.github.qdiaps.solitaire.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.qdiaps.solitaire.ui.game.VictorySummary
import io.github.qdiaps.solitaire.ui.game.components.VictorySummaryDialog
import io.github.qdiaps.solitaire.ui.theme.CardDimensions
import io.github.qdiaps.solitaire.ui.theme.FeltTheme
import io.github.qdiaps.solitaire.ui.theme.SolitaireTheme

private val SAMPLE_STANDARD_SUMMARY = VictorySummary(
    timeSeconds = 185,
    movesCount = 112,
    score = 640,
    isNewBestTime = false,
    isNewFewestMoves = false,
    isNewHighScore = false,
    isNewBestStreak = false
)

private val SAMPLE_RECORD_BREAK_SUMMARY = VictorySummary(
    timeSeconds = 98,
    movesCount = 64,
    score = 850,
    isNewBestTime = true,
    isNewFewestMoves = true,
    isNewHighScore = true,
    isNewBestStreak = true
)

@Preview(name = "1. Victory Summary - Standard Win (Classic Green)", showBackground = true, widthDp = 393, heightDp = 800)
@Composable
fun VictorySummaryDialogStandardPreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    SolitaireTheme(cardDimensions = sampleDimensions, feltTheme = FeltTheme.CLASSIC_GREEN) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SolitaireTheme.colors.tableBackground),
            contentAlignment = Alignment.Center
        ) {
            VictorySummaryDialog(
                summary = SAMPLE_STANDARD_SUMMARY,
                onNewGame = {},
                onPlayAgain = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(name = "2. Victory Summary - Record Breakthrough (Classic Green)", showBackground = true, widthDp = 393, heightDp = 800)
@Composable
fun VictorySummaryDialogRecordBreakPreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    SolitaireTheme(cardDimensions = sampleDimensions, feltTheme = FeltTheme.CLASSIC_GREEN) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SolitaireTheme.colors.tableBackground),
            contentAlignment = Alignment.Center
        ) {
            VictorySummaryDialog(
                summary = SAMPLE_RECORD_BREAK_SUMMARY,
                onNewGame = {},
                onPlayAgain = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(name = "3. Victory Summary - Dark Felt (Dark Charcoal)", showBackground = true, widthDp = 393, heightDp = 800)
@Composable
fun VictorySummaryDialogDarkFeltPreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    SolitaireTheme(cardDimensions = sampleDimensions, feltTheme = FeltTheme.DARK_CHARCOAL) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SolitaireTheme.colors.tableBackground),
            contentAlignment = Alignment.Center
        ) {
            VictorySummaryDialog(
                summary = SAMPLE_RECORD_BREAK_SUMMARY,
                onNewGame = {},
                onPlayAgain = {},
                onDismiss = {}
            )
        }
    }
}
