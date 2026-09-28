package io.github.qdiaps.solitaire.ui.game.animation.victory

import androidx.compose.runtime.Immutable
import kotlin.math.abs

/**
 * Configuration parameters for bouncing card dynamics.
 *
 * @property gravity Downward gravitational acceleration in pixels per second squared (px/s²).
 * @property floorRestitution Coefficient of restitution upon hitting the floor (0.0 = completely inelastic, 1.0 = perfectly elastic).
 * @property wallRestitution Coefficient of restitution upon hitting lateral screen boundaries when [bounceWalls] is true.
 * @property bounceWalls True to bounce off left and right screen edges; false to allow cards to exit the screen naturally.
 * @property minBounceVelocity Velocity threshold below which rebound vertical velocity is clamped to zero to prevent microscopic jitter.
 * @property maxDeltaTimeSeconds Clamping threshold for time step to guarantee numerical stability during frame drops.
 */
@Immutable
data class BouncingCardsPhysicsConfig(
    val gravity: Float = 1800f,
    val floorRestitution: Float = 0.85f,
    val wallRestitution: Float = 0.85f,
    val bounceWalls: Boolean = false,
    val minBounceVelocity: Float = 50f,
    val maxDeltaTimeSeconds: Float = 0.05f
)

/**
 * Pure math physics engine simulating card trajectory, gravity, floor impacts, and lateral momentum.
 */
class BouncingCardsPhysics(
    val config: BouncingCardsPhysicsConfig = BouncingCardsPhysicsConfig()
) {
    /**
     * Steps a card particle forward by [dtSeconds] within the viewport bounds ([screenWidth] x [screenHeight]).
     */
    fun step(
        particle: BouncingCardParticle,
        dtSeconds: Float,
        screenWidth: Float,
        screenHeight: Float
    ): BouncingCardParticle {
        if (!particle.isActive || particle.isTerminated) {
            return particle
        }

        val dt = dtSeconds.coerceIn(0f, config.maxDeltaTimeSeconds)
        var vx = particle.vx
        var vy = particle.vy + config.gravity * dt
        var x = particle.x + vx * dt
        var y = particle.y + vy * dt

        val floorY = screenHeight - particle.height

        if (y >= floorY) {
            y = floorY
            vy = -vy * config.floorRestitution
            if (abs(vy) < config.minBounceVelocity) {
                vy = 0f
            }
        }

        var isTerminated = false
        if (config.bounceWalls) {
            val maxRight = screenWidth - particle.width
            if (x <= 0f) {
                x = 0f
                vx = -vx * config.wallRestitution
            } else if (x >= maxRight) {
                x = maxRight
                vx = -vx * config.wallRestitution
            }
        } else {
            // Classic Solitaire mode: card exits off-screen left or right
            if (x < -particle.width || x > screenWidth) {
                isTerminated = true
            }
        }

        return particle.copy(
            x = x,
            y = y,
            vx = vx,
            vy = vy,
            isTerminated = isTerminated
        )
    }
}
