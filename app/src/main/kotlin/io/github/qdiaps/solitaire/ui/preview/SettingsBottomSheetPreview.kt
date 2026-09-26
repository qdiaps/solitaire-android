package io.github.qdiaps.solitaire.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.qdiaps.solitaire.domain.deck.KlondikeDealer
import io.github.qdiaps.solitaire.domain.rules.DrawMode
import io.github.qdiaps.solitaire.ui.game.SolitaireGameScreen
import io.github.qdiaps.solitaire.ui.game.components.SettingsSheetContent
import io.github.qdiaps.solitaire.ui.theme.CardBackStyle
import io.github.qdiaps.solitaire.ui.theme.CardFaceStyle
import io.github.qdiaps.solitaire.ui.theme.FeltTheme
import io.github.qdiaps.solitaire.ui.theme.SolitaireTheme
import kotlin.random.Random

@Preview(name = "1. Settings Sheet - Classic Green (Standard Height)", device = "id:pixel_7", showBackground = true)
@Composable
fun SettingsSheetClassicGreenPreview() {
    SolitaireTheme(feltTheme = FeltTheme.CLASSIC_GREEN) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SolitaireTheme.colors.tableSurface)
        ) {
            SettingsSheetContent(
                drawMode = DrawMode.DRAW_ONE,
                isLeftHanded = false,
                autoHintEnabled = false,
                feltTheme = FeltTheme.CLASSIC_GREEN,
                cardBackStyle = CardBackStyle.CLASSIC_LATTICE,
                cardFaceStyle = CardFaceStyle.MODERN_CLEAN,
                soundEnabled = true,
                hapticsEnabled = true,
                onDrawModeChange = {},
                onLeftHandedChange = {},
                onAutoHintChange = {},
                onFeltThemeChange = {},
                onCardBackStyleChange = {},
                onCardFaceStyleChange = {},
                onSoundChange = {},
                onHapticsChange = {},
                onResetToDefaults = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(name = "2. Settings Sheet - Compact Height (600dp)", widthDp = 360, heightDp = 600, showBackground = true)
@Composable
fun SettingsSheetCompactHeightPreview() {
    SolitaireTheme(feltTheme = FeltTheme.CLASSIC_GREEN) {
        Box(
            modifier = Modifier
                .height(600.dp)
                .background(SolitaireTheme.colors.tableSurface)
        ) {
            SettingsSheetContent(
                drawMode = DrawMode.DRAW_ONE,
                isLeftHanded = false,
                autoHintEnabled = true,
                feltTheme = FeltTheme.CLASSIC_GREEN,
                cardBackStyle = CardBackStyle.CLASSIC_LATTICE,
                cardFaceStyle = CardFaceStyle.MODERN_CLEAN,
                soundEnabled = true,
                hapticsEnabled = false,
                onDrawModeChange = {},
                onLeftHandedChange = {},
                onAutoHintChange = {},
                onFeltThemeChange = {},
                onCardBackStyleChange = {},
                onCardFaceStyleChange = {},
                onSoundChange = {},
                onHapticsChange = {},
                onResetToDefaults = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(name = "3. Settings Sheet - Deep Navy Theme", device = "id:pixel_7", showBackground = true)
@Composable
fun SettingsSheetDeepNavyPreview() {
    SolitaireTheme(feltTheme = FeltTheme.DEEP_NAVY) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SolitaireTheme.colors.tableSurface)
        ) {
            SettingsSheetContent(
                drawMode = DrawMode.DRAW_THREE,
                isLeftHanded = true,
                autoHintEnabled = false,
                feltTheme = FeltTheme.DEEP_NAVY,
                cardBackStyle = CardBackStyle.EMERALD_ART_DECO,
                cardFaceStyle = CardFaceStyle.MODERN_CLEAN,
                soundEnabled = true,
                hapticsEnabled = true,
                onDrawModeChange = {},
                onLeftHandedChange = {},
                onAutoHintChange = {},
                onFeltThemeChange = {},
                onCardBackStyleChange = {},
                onCardFaceStyleChange = {},
                onSoundChange = {},
                onHapticsChange = {},
                onResetToDefaults = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(name = "4. Settings Sheet - Dark Charcoal Theme", device = "id:pixel_7", showBackground = true)
@Composable
fun SettingsSheetDarkCharcoalPreview() {
    SolitaireTheme(feltTheme = FeltTheme.DARK_CHARCOAL) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SolitaireTheme.colors.tableSurface)
        ) {
            SettingsSheetContent(
                drawMode = DrawMode.DRAW_ONE,
                isLeftHanded = false,
                autoHintEnabled = false,
                feltTheme = FeltTheme.DARK_CHARCOAL,
                cardBackStyle = CardBackStyle.OBSIDIAN_MINIMAL,
                cardFaceStyle = CardFaceStyle.MODERN_CLEAN,
                soundEnabled = false,
                hapticsEnabled = false,
                onDrawModeChange = {},
                onLeftHandedChange = {},
                onAutoHintChange = {},
                onFeltThemeChange = {},
                onCardBackStyleChange = {},
                onCardFaceStyleChange = {},
                onSoundChange = {},
                onHapticsChange = {},
                onResetToDefaults = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(name = "5. Settings Sheet - Wine Red Theme", device = "id:pixel_7", showBackground = true)
@Composable
fun SettingsSheetWineRedPreview() {
    SolitaireTheme(feltTheme = FeltTheme.WINE_RED) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SolitaireTheme.colors.tableSurface)
        ) {
            SettingsSheetContent(
                drawMode = DrawMode.DRAW_THREE,
                isLeftHanded = true,
                autoHintEnabled = true,
                feltTheme = FeltTheme.WINE_RED,
                cardBackStyle = CardBackStyle.CRIMSON_VINTAGE,
                cardFaceStyle = CardFaceStyle.MODERN_CLEAN,
                soundEnabled = true,
                hapticsEnabled = true,
                onDrawModeChange = {},
                onLeftHandedChange = {},
                onAutoHintChange = {},
                onFeltThemeChange = {},
                onCardBackStyleChange = {},
                onCardFaceStyleChange = {},
                onSoundChange = {},
                onHapticsChange = {},
                onResetToDefaults = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(name = "6. Solitaire Screen with Settings Open", device = "id:pixel_7", showBackground = true)
@Composable
fun SolitaireScreenWithSettingsOpenPreview() {
    val deal = KlondikeDealer.dealShuffled(Random(123))
    SolitaireGameScreen(
        boardState = deal,
        timeSeconds = 45L,
        canUndo = true,
        isSettingsOpen = true,
        feltTheme = FeltTheme.CLASSIC_GREEN,
        cardBackStyle = CardBackStyle.CLASSIC_LATTICE
    )
}
