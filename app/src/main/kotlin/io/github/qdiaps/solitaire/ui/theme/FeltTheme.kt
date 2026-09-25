package io.github.qdiaps.solitaire.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * 4 felt table surface themes representing classic card table cloth textures.
 */
enum class FeltTheme(
    val id: String,
    val displayName: String,
    val primaryColor: Color,
    val darkColor: Color,
    val surfaceColor: Color,
    val onFeltColor: Color = Color.White
) {
    CLASSIC_GREEN(
        id = "classic_green",
        displayName = "Classic Green",
        primaryColor = FeltGreenPrimary,
        darkColor = FeltGreenDark,
        surfaceColor = FeltGreenSurface
    ),
    DEEP_NAVY(
        id = "deep_navy",
        displayName = "Deep Navy",
        primaryColor = FeltNavyPrimary,
        darkColor = FeltNavyDark,
        surfaceColor = FeltNavySurface
    ),
    DARK_CHARCOAL(
        id = "dark_charcoal",
        displayName = "Dark Charcoal",
        primaryColor = FeltCharcoalPrimary,
        darkColor = FeltCharcoalDark,
        surfaceColor = FeltCharcoalSurface
    ),
    WINE_RED(
        id = "wine_red",
        displayName = "Wine Red",
        primaryColor = FeltWinePrimary,
        darkColor = FeltWineDark,
        surfaceColor = FeltWineSurface
    );

    companion object {
        val DEFAULT: FeltTheme = CLASSIC_GREEN

        fun fromId(id: String?): FeltTheme {
            return entries.firstOrNull { it.id == id } ?: DEFAULT
        }
    }
}

/**
 * Palette containing colors for cards, felt tables, and UI indicators.
 */
@Immutable
data class SolitaireColors(
    val feltTheme: FeltTheme = FeltTheme.CLASSIC_GREEN,
    val cardRed: Color = CardRed,
    val cardBlack: Color = CardBlack,
    val cardBackground: Color = CardBackground,
    val cardBorder: Color = CardBorder,
    val cardBackNavy: Color = CardBackNavy,
    val cardBackPattern: Color = CardBackPattern,
    val slotBorder: Color = SlotBorder,
    val slotBackground: Color = SlotBackground,
    val onFeltText: Color = Color.White,
    val onFeltSubtle: Color = Color(0xB3FFFFFF),
    val hintHighlight: Color = HintHighlight,
    val scoreGold: Color = ScoreGold
) {
    val tableBackground: Color get() = feltTheme.primaryColor
    val tableDarkEdge: Color get() = feltTheme.darkColor
    val tableSurface: Color get() = feltTheme.surfaceColor
}
