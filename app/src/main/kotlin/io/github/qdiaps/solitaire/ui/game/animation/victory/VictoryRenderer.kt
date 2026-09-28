package io.github.qdiaps.solitaire.ui.game.animation.victory

import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Pluggable rendering strategy for Solitaire victory animations.
 *
 * Designed with an extensible interface to support diverse visual styles
 * (e.g. Classic Windows bouncing trails, fireworks, card spirals).
 */
interface VictoryRenderer {
    /**
     * The specific victory animation type handled by this renderer.
     */
    val animationType: VictoryAnimationType

    /**
     * Renders the current frame of the victory animation onto the provided [drawScope].
     *
     * @param drawScope Active Compose drawing scope.
     * @param animator The victory animator providing active particle states.
     * @param spriteCache Pre-rendered card face sprites cache.
     */
    fun render(
        drawScope: DrawScope,
        animator: VictoryAnimator,
        spriteCache: CardSpriteCache
    )

    /**
     * Resets any offscreen frame buffers or cached state, freeing graphics resources.
     */
    fun reset()
}
