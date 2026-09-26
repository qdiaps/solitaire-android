package io.github.qdiaps.solitaire.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.qdiaps.solitaire.data.model.GameStats
import io.github.qdiaps.solitaire.ui.game.components.StatsDialog
import io.github.qdiaps.solitaire.ui.theme.CardDimensions
import io.github.qdiaps.solitaire.ui.theme.FeltTheme
import io.github.qdiaps.solitaire.ui.theme.SolitaireTheme

private val SAMPLE_EMPTY_STATS = GameStats()

private val SAMPLE_POPULATED_STATS = GameStats(
    gamesPlayed = 42,
    gamesWon = 28,
    currentStreak = 3,
    bestStreak = 7,
    bestTimeSeconds = 135,
    fewestMoves = 82,
    highScore = 780
)

@Preview(name = "1. Stats Dialog - Empty State", showBackground = true, widthDp = 393, heightDp = 800)
@Composable
fun StatsDialogEmptyPreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    SolitaireTheme(cardDimensions = sampleDimensions, feltTheme = FeltTheme.CLASSIC_GREEN) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SolitaireTheme.colors.tableBackground),
            contentAlignment = Alignment.Center
        ) {
            StatsDialog(
                stats = SAMPLE_EMPTY_STATS,
                onResetStats = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(name = "2. Stats Dialog - Populated State (Classic Green)", showBackground = true, widthDp = 393, heightDp = 800)
@Composable
fun StatsDialogPopulatedClassicGreenPreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    SolitaireTheme(cardDimensions = sampleDimensions, feltTheme = FeltTheme.CLASSIC_GREEN) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SolitaireTheme.colors.tableBackground),
            contentAlignment = Alignment.Center
        ) {
            StatsDialog(
                stats = SAMPLE_POPULATED_STATS,
                onResetStats = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(name = "3. Stats Dialog - Populated State (Deep Navy)", showBackground = true, widthDp = 393, heightDp = 800)
@Composable
fun StatsDialogPopulatedDeepNavyPreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    SolitaireTheme(cardDimensions = sampleDimensions, feltTheme = FeltTheme.DEEP_NAVY) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SolitaireTheme.colors.tableBackground),
            contentAlignment = Alignment.Center
        ) {
            StatsDialog(
                stats = SAMPLE_POPULATED_STATS,
                onResetStats = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(name = "4. Stats Dialog - Compact Screen (360dp)", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun StatsDialogCompactScreenPreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 360.dp)
    SolitaireTheme(cardDimensions = sampleDimensions, feltTheme = FeltTheme.DARK_CHARCOAL) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SolitaireTheme.colors.tableBackground),
            contentAlignment = Alignment.Center
        ) {
            StatsDialog(
                stats = SAMPLE_POPULATED_STATS,
                onResetStats = {},
                onDismiss = {}
            )
        }
    }
}
