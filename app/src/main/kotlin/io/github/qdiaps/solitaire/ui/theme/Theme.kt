package io.github.qdiaps.solitaire.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val LocalSolitaireColors = staticCompositionLocalOf { SolitaireColors() }
val LocalSolitaireCardTypography = staticCompositionLocalOf { SolitaireCardTypography() }
val LocalCardDimensions = staticCompositionLocalOf<CardDimensions?> { null }
val LocalCardBackStyle = staticCompositionLocalOf { CardBackStyle.DEFAULT }
val LocalCardFaceStyle = staticCompositionLocalOf { CardFaceStyle.DEFAULT }

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

    val cardBackStyle: CardBackStyle
        @Composable
        @ReadOnlyComposable
        get() = LocalCardBackStyle.current

    val cardFaceStyle: CardFaceStyle
        @Composable
        @ReadOnlyComposable
        get() = LocalCardFaceStyle.current

    val cardDimensions: CardDimensions
        @Composable
        @ReadOnlyComposable
        get() = LocalCardDimensions.current
            ?: error("CardDimensions not provided. Wrap your board inside SolitaireTheme with cardDimensions.")
}

/**
 * Main application theme wrapper for Solitaire.
 *
 * Configures [FeltTheme] palette, [CardBackStyle], [CardFaceStyle], card typography,
 * and Material 3 dark color scheme tailored for green/navy/charcoal/wine felt tables.
 *
 * If [cardDimensions] is not explicitly specified, falls back to the ambient [LocalCardDimensions]
 * or standard phone proportions calculated for 393.dp screen width.
 */
@Composable
fun SolitaireTheme(
    feltTheme: FeltTheme = FeltTheme.CLASSIC_GREEN,
    cardBackStyle: CardBackStyle = CardBackStyle.DEFAULT,
    cardFaceStyle: CardFaceStyle = CardFaceStyle.DEFAULT,
    cardDimensions: CardDimensions? = null,
    content: @Composable () -> Unit
) {
    val solitaireColors = remember(feltTheme) {
        SolitaireColors(feltTheme = feltTheme)
    }
    val cardTypography = remember(cardFaceStyle) {
        createCardTypography(cardFaceStyle)
    }

    val resolvedDimensions = cardDimensions
        ?: LocalCardDimensions.current
        ?: remember { CardDimensions.calculate(availableWidth = 393.dp) }

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
        LocalCardDimensions provides resolvedDimensions,
        LocalCardBackStyle provides cardBackStyle,
        LocalCardFaceStyle provides cardFaceStyle
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SolitaireTypography,
            content = content
        )
    }
}
