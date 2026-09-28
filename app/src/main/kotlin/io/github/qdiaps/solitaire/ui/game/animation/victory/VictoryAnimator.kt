package io.github.qdiaps.solitaire.ui.game.animation.victory

/**
 * Common extensible interface for Solitaire victory celebration animations.
 */
interface VictoryAnimator {
    /**
     * Type identifier of the animation style implemented.
     */
    val animationType: VictoryAnimationType

    /**
     * Whether the animation is currently running.
     */
    val isRunning: Boolean

    /**
     * Whether the animation has completed its sequence.
     */
    val isFinished: Boolean

    /**
     * The active particles to be rendered on canvas.
     */
    val activeParticles: List<BouncingCardParticle>

    /**
     * Initializes and starts the animation with board layout geometry.
     */
    fun start(spec: VictoryStartSpec)

    /**
     * Advances the animation simulation by [deltaTimeSeconds].
     */
    fun update(deltaTimeSeconds: Float)

    /**
     * Stops and cancels the animation immediately.
     */
    fun stop()
}
