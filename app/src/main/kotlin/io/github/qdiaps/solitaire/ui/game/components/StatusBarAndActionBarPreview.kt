package io.github.qdiaps.solitaire.ui.game.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.qdiaps.solitaire.ui.theme.CardDimensions
import io.github.qdiaps.solitaire.ui.theme.FeltTheme
import io.github.qdiaps.solitaire.ui.theme.SolitaireTheme

@Preview(name = "1. Top Status Bar - Initial State", showBackground = true)
@Composable
fun TopStatusBarInitialPreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    SolitaireTheme(cardDimensions = sampleDimensions) {
        Column(
            modifier = Modifier
                .background(SolitaireTheme.colors.tableBackground)
                .padding(vertical = 8.dp)
        ) {
            TopStatusBarView(
                score = 0,
                moves = 0,
                timeSeconds = 0L
            )
        }
    }
}

@Preview(name = "2. Top Status Bar - Active Game State", showBackground = true)
@Composable
fun TopStatusBarActivePreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    SolitaireTheme(cardDimensions = sampleDimensions) {
        Column(
            modifier = Modifier
                .background(SolitaireTheme.colors.tableBackground)
                .padding(vertical = 8.dp)
        ) {
            TopStatusBarView(
                score = 520,
                moves = 43,
                timeSeconds = 245L // 04:05
            )
        }
    }
}

@Preview(name = "3. Bottom Action Bar - Default State", showBackground = true)
@Composable
fun BottomActionBarDefaultPreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    SolitaireTheme(cardDimensions = sampleDimensions) {
        Column(
            modifier = Modifier
                .background(SolitaireTheme.colors.tableBackground)
                .padding(vertical = 8.dp)
        ) {
            BottomActionBarView(
                canUndo = true,
                onUndoClick = {},
                onHintClick = {},
                onNewGameClick = {},
                onSettingsClick = {}
            )
        }
    }
}

@Preview(name = "4. Bottom Action Bar - Disabled Undo & Active Hint", showBackground = true)
@Composable
fun BottomActionBarDisabledUndoPreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    SolitaireTheme(cardDimensions = sampleDimensions) {
        Column(
            modifier = Modifier
                .background(SolitaireTheme.colors.tableBackground)
                .padding(vertical = 8.dp)
        ) {
            BottomActionBarView(
                canUndo = false,
                isHintActive = true,
                onUndoClick = {},
                onHintClick = {},
                onNewGameClick = {},
                onSettingsClick = {}
            )
        }
    }
}

@Preview(name = "5. Status & Action Bars - Felt Themes", showBackground = true)
@Composable
fun BarsFeltThemesPreview() {
    val sampleDimensions = CardDimensions.calculate(availableWidth = 393.dp)
    val testThemes = listOf(
        FeltTheme.CLASSIC_GREEN,
        FeltTheme.DEEP_NAVY,
        FeltTheme.DARK_CHARCOAL,
        FeltTheme.WINE_RED
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (theme in testThemes) {
            SolitaireTheme(feltTheme = theme, cardDimensions = sampleDimensions) {
                Column(
                    modifier = Modifier
                        .background(SolitaireTheme.colors.tableBackground)
                        .padding(vertical = 8.dp)
                ) {
                    TopStatusBarView(score = 310, moves = 28, timeSeconds = 162L)
                    BottomActionBarView(
                        canUndo = true,
                        onUndoClick = {},
                        onHintClick = {},
                        onNewGameClick = {},
                        onSettingsClick = {}
                    )
                }
            }
        }
    }
}
