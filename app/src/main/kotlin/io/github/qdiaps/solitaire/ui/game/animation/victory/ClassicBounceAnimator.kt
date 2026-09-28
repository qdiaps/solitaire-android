package io.github.qdiaps.solitaire.ui.game.animation.victory

import kotlin.math.min
import kotlin.random.Random

/**
 * Implementation of [VictoryAnimator] rendering classic cascading bouncing cards.
 */
class ClassicBounceAnimator(
    config: CascadeConfig = CascadeConfig(),
    physics: BouncingCardsPhysics = BouncingCardsPhysics(),
    random: Random = Random.Default
) : VictoryAnimator {

    override val animationType: VictoryAnimationType = VictoryAnimationType.CLASSIC_BOUNCE

    private val sequencer = CascadeSequencer(config = config, physics = physics, random = random)
    private var currentSpec: VictoryStartSpec? = null

    private var _isRunning: Boolean = false
    override val isRunning: Boolean
        get() = _isRunning

    private var _isStoppedManually: Boolean = false
    override val isFinished: Boolean
        get() = _isStoppedManually || sequencer.isFinished

    override val activeParticles: List<BouncingCardParticle>
        get() = sequencer.activeCards

    override fun start(spec: VictoryStartSpec) {
        currentSpec = spec
        _isStoppedManually = false
        _isRunning = true
        sequencer.initialize(
            foundations = spec.foundations,
            origins = spec.foundationOrigins,
            screenWidth = spec.screenWidth,
            screenHeight = spec.screenHeight,
            cardWidth = spec.cardWidth,
            cardHeight = spec.cardHeight
        )
    }

    override fun update(deltaTimeSeconds: Float) {
        if (!_isRunning || isFinished) return

        val spec = currentSpec ?: return
        var remaining = deltaTimeSeconds
        val maxStep = 0.033f

        while (remaining > 0f && _isRunning && !isFinished) {
            val stepDt = min(remaining, maxStep)
            sequencer.step(stepDt, spec.screenWidth, spec.screenHeight)
            remaining -= stepDt
        }

        if (sequencer.isFinished) {
            _isRunning = false
        }
    }

    override fun stop() {
        _isRunning = false
        _isStoppedManually = true
    }
}
