package io.github.qdiaps.solitaire.ui.game.animation.victory

import androidx.compose.runtime.Immutable
import io.github.qdiaps.solitaire.domain.model.Card

/**
 * Top-left screen coordinates of a foundation pile.
 */
@Immutable
data class FoundationOrigin(
    val x: Float,
    val y: Float
)

/**
 * Specification providing full layout geometry and card data required to initialize victory celebration animations.
 *
 * @property foundations Current foundation piles (each containing cards from bottom to top, Ace to King).
 * @property foundationOrigins Top-left coordinates for each of the foundation piles.
 * @property screenWidth Width of the animation viewport in pixels.
 * @property screenHeight Height of the animation viewport in pixels.
 * @property cardWidth Width of an individual card in pixels.
 * @property cardHeight Height of an individual card in pixels.
 */
@Immutable
data class VictoryStartSpec(
    val foundations: List<List<Card>>,
    val foundationOrigins: List<FoundationOrigin>,
    val screenWidth: Float,
    val screenHeight: Float,
    val cardWidth: Float,
    val cardHeight: Float
)
