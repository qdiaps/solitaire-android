package io.github.qdiaps.solitaire.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * 4 distinct visual styles for face-down card backs.
 *
 * @property id Unique persistent identifier used in settings DataStore.
 * @property displayName Human-readable label for UI selection.
 * @property primaryColor Base background color of the card back.
 * @property patternColor Contrasting color for geometric and ornamental vector lines.
 */
@Immutable
enum class CardBackStyle(
    val id: String,
    val displayName: String,
    val primaryColor: Color,
    val patternColor: Color
) {
    CLASSIC_LATTICE(
        id = "classic_lattice",
        displayName = "Classic Lattice",
        primaryColor = Color(0xFF1A365D),
        patternColor = Color(0x33FFFFFF)
    ),
    CRIMSON_VINTAGE(
        id = "crimson_vintage",
        displayName = "Crimson Vintage",
        primaryColor = Color(0xFF7A1526),
        patternColor = Color(0x40FFD700)
    ),
    EMERALD_ART_DECO(
        id = "emerald_art_deco",
        displayName = "Emerald Art Deco",
        primaryColor = Color(0xFF064E3B),
        patternColor = Color(0x40FBBF24)
    ),
    OBSIDIAN_MINIMAL(
        id = "obsidian_minimal",
        displayName = "Obsidian Minimal",
        primaryColor = Color(0xFF18181B),
        patternColor = Color(0x2EFFFFFF)
    );

    companion object {
        val DEFAULT: CardBackStyle = CLASSIC_LATTICE

        fun fromId(id: String?): CardBackStyle {
            return entries.firstOrNull { it.id == id } ?: DEFAULT
        }
    }
}
