package io.github.qdiaps.solitaire.ui.game.animation.victory

import kotlin.random.Random

/**
 * Factory for creating [VictoryAnimator] instances corresponding to [VictoryAnimationType].
 */
object VictoryAnimatorFactory {

    /**
     * Creates a new [VictoryAnimator] for the requested [type].
     */
    fun create(
        type: VictoryAnimationType = VictoryAnimationType.CLASSIC_BOUNCE,
        random: Random = Random.Default
    ): VictoryAnimator {
        return when (type) {
            VictoryAnimationType.CLASSIC_BOUNCE -> ClassicBounceAnimator(random = random)
        }
    }
}
