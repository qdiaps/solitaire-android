package io.github.qdiaps.solitaire.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * Visual styling and typography for face-up playing card faces.
 *
 * Designed with an extensible enum architecture to allow seamless addition of future
 * face themes (e.g. Classic Serif, Jumbo/Large Print, Minimalist) while maintaining
 * Modern Clean as the primary canonical style.
 *
 * @property id Unique persistent identifier used in settings DataStore.
 * @property displayName Human-readable label for UI selection.
 * @property fontFamily Font family applied to card rank labels.
 * @property fontWeight Weight applied to card rank labels.
 * @property rankTextScale Relative scale multiplier for rank font size (1.0f = default 14.sp).
 * @property cornerEmblemScale Relative scale multiplier for corner mini suit emblems.
 * @property centerEmblemScale Relative scale multiplier for center suit emblem.
 */
@Immutable
enum class CardFaceStyle(
    val id: String,
    val displayName: String,
    val fontFamily: FontFamily = FontFamily.SansSerif,
    val fontWeight: FontWeight = FontWeight.Bold,
    val rankTextScale: Float = 1.0f,
    val cornerEmblemScale: Float = 1.0f,
    val centerEmblemScale: Float = 1.0f
) {
    MODERN_CLEAN(
        id = "modern_clean",
        displayName = "Modern Clean",
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        rankTextScale = 1.0f,
        cornerEmblemScale = 1.0f,
        centerEmblemScale = 1.0f
    );

    companion object {
        val DEFAULT: CardFaceStyle = MODERN_CLEAN

        fun fromId(id: String?): CardFaceStyle {
            return entries.firstOrNull { it.id == id } ?: DEFAULT
        }
    }
}
