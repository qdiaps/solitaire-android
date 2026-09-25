package io.github.qdiaps.solitaire.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalSolitaireColors = staticCompositionLocalOf { SolitaireColors() }
val LocalSolitaireCardTypography = staticCompositionLocalOf { SolitaireCardTypography() }
val LocalCardDimensions = staticCompositionLocalOf<CardDimensions?> { null }

/**
 * Accessor for Solitaire theme attributes.
 */
object SolitaireTheme {
    val colors: SolitaireColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSolitaireColors.current

    val typography: SolitaireCardTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalSolitaireCardTypography.current

    val cardDimensions: CardDimensions
        @Composable
        @ReadOnlyComposable
        get() = LocalCardDimensions.current
            ?: error("CardDimensions not provided. Wrap your board inside SolitaireTheme with cardDimensions.")
}

/**
 * Main application theme wrapper for Solitaire.
 *
 * Configures [FeltTheme] palette, card typography, and Material 3 dark color scheme
 * tailored for green/navy/charcoal/wine felt tables.
 */
@Composable
fun SolitaireTheme(
    feltTheme: FeltTheme = FeltTheme.CLASSIC_GREEN,
    cardDimensions: CardDimensions? = null,
    content: @Composable () -> Unit
) {
    val solitaireColors = remember(feltTheme) {
        SolitaireColors(feltTheme = feltTheme)
    }
    val cardTypography = remember { SolitaireCardTypography() }

    val colorScheme = remember(feltTheme) {
        darkColorScheme(
            primary = solitaireColors.scoreGold,
            onPrimary = Color.Black,
            surface = solitaireColors.tableSurface,
            onSurface = solitaireColors.onFeltText,
            background = solitaireColors.tableBackground,
            onBackground = solitaireColors.onFeltText
        )
    }

    CompositionLocalProvider(
        LocalSolitaireColors provides solitaireColors,
        LocalSolitaireCardTypography provides cardTypography,
        LocalCardDimensions provides cardDimensions
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SolitaireTypography,
            content = content
        )
    }
}
