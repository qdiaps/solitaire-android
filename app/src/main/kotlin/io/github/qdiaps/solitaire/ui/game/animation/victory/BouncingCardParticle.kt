package io.github.qdiaps.solitaire.ui.game.animation.victory

import androidx.compose.runtime.Immutable
import io.github.qdiaps.solitaire.domain.model.Card

/**
 * Immutable particle state for a single bouncing card in victory physics simulations.
 *
 * @property card The card domain entity.
 * @property initialX The starting horizontal position (e.g. from the foundation pile).
 * @property initialY The starting vertical position (e.g. from the foundation pile).
 * @property width The rendered card width in pixels.
 * @property height The rendered card height in pixels.
 * @property x The current horizontal position in pixels.
 * @property y The current vertical position in pixels.
 * @property vx Current horizontal velocity in pixels per second.
 * @property vy Current vertical velocity in pixels per second.
 * @property isActive True if the card has been launched into the simulation.
 * @property isTerminated True if the card has completed its bounce sequence and exited the screen.
 */
@Immutable
data class BouncingCardParticle(
    val card: Card,
    val initialX: Float,
    val initialY: Float,
    val width: Float,
    val height: Float,
    val x: Float = initialX,
    val y: Float = initialY,
    val vx: Float = 0f,
    val vy: Float = 0f,
    val isActive: Boolean = false,
    val isTerminated: Boolean = false
)
