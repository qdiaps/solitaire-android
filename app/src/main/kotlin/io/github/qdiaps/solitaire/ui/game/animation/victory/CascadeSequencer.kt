package io.github.qdiaps.solitaire.ui.game.animation.victory

import androidx.compose.runtime.Immutable
import io.github.qdiaps.solitaire.domain.model.Card
import kotlin.random.Random

/**
 * Configuration for cascading card release sequencer.
 *
 * @property releaseIntervalMs Milliseconds between launching consecutive cards from foundations.
 * @property cardWidth Default card width in pixels if not overridden in initialization.
 * @property cardHeight Default card height in pixels if not overridden in initialization.
 */
@Immutable
data class CascadeConfig(
    val releaseIntervalMs: Long = 150L,
    val cardWidth: Float = 72f,
    val cardHeight: Float = 96f
)

/**
 * Deterministic sequencer that queues all foundation cards from top (King) to bottom (Ace),
 * releasing them in timed intervals across the foundation piles into the bouncing physics simulation.
 */
class CascadeSequencer(
    val config: CascadeConfig = CascadeConfig(),
    val physics: BouncingCardsPhysics = BouncingCardsPhysics(),
    private val random: Random = Random.Default
) {
    var queuedCards: List<BouncingCardParticle> = emptyList()
        private set

    var activeCards: List<BouncingCardParticle> = emptyList()
        private set

    var completedCardsCount: Int = 0
        private set

    var elapsedTimeMs: Long = 0L
        private set

    private var nextReleaseTimeMs: Long = 0L

    val isAllReleased: Boolean
        get() = queuedCards.isEmpty()

    val isFinished: Boolean
        get() = isAllReleased && activeCards.isNotEmpty() && activeCards.all { it.isTerminated }

    /**
     * Orders cards from foundations:
     * Rank index from highest (e.g. 12 = King) down to 0 (Ace),
     * cycling across foundations from rightmost (3) down to leftmost (0).
     */
    fun initialize(
        foundations: List<List<Card>>,
        origins: List<FoundationOrigin>,
        screenWidth: Float,
        screenHeight: Float,
        cardWidth: Float = config.cardWidth,
        cardHeight: Float = config.cardHeight
    ) {
        val maxRankIndex = foundations.maxOfOrNull { it.size - 1 } ?: -1
        val particles = mutableListOf<BouncingCardParticle>()

        for (rankIndex in maxRankIndex downTo 0) {
            for (foundationIndex in foundations.indices.reversed()) {
                val foundationCards = foundations.getOrNull(foundationIndex) ?: continue
                val card = foundationCards.getOrNull(rankIndex) ?: continue
                val origin = origins.getOrElse(foundationIndex) {
                    FoundationOrigin(
                        x = (screenWidth / (foundations.size + 1)) * (foundationIndex + 1) - cardWidth / 2f,
                        y = 20f
                    )
                }

                val vx = calculateInitialVx(foundationIndex, foundations.size)
                val vy = calculateInitialVy()

                particles.add(
                    BouncingCardParticle(
                        card = card,
                        initialX = origin.x,
                        initialY = origin.y,
                        width = cardWidth,
                        height = cardHeight,
                        vx = vx,
                        vy = vy,
                        isActive = false,
                        isTerminated = false
                    )
                )
            }
        }

        elapsedTimeMs = 0L
        completedCardsCount = 0

        if (particles.isNotEmpty()) {
            val first = particles.removeAt(0).copy(isActive = true)
            activeCards = listOf(first)
            queuedCards = particles
            nextReleaseTimeMs = config.releaseIntervalMs
        } else {
            activeCards = emptyList()
            queuedCards = emptyList()
            nextReleaseTimeMs = 0L
        }
    }

    /**
     * Steps the simulation forward by [deltaTimeSeconds], releasing queued cards and updating active particles.
     */
    fun step(
        deltaTimeSeconds: Float,
        screenWidth: Float,
        screenHeight: Float
    ) {
        if (isFinished) return

        val dtMs = (deltaTimeSeconds * 1000f).toLong()
        elapsedTimeMs += dtMs

        // Release queued cards if elapsed time has reached or surpassed nextReleaseTimeMs
        if (queuedCards.isNotEmpty()) {
            val toRelease = mutableListOf<BouncingCardParticle>()
            val remainingQueue = queuedCards.toMutableList()

            while (remainingQueue.isNotEmpty() && elapsedTimeMs >= nextReleaseTimeMs) {
                val next = remainingQueue.removeAt(0).copy(isActive = true)
                toRelease.add(next)
                nextReleaseTimeMs += config.releaseIntervalMs
            }

            if (toRelease.isNotEmpty()) {
                queuedCards = remainingQueue
                activeCards = activeCards + toRelease
            }
        }

        // Step active cards
        activeCards = activeCards.map { particle ->
            if (particle.isTerminated) {
                particle
            } else {
                val updated = physics.step(particle, deltaTimeSeconds, screenWidth, screenHeight)
                if (updated.isTerminated && !particle.isTerminated) {
                    completedCardsCount++
                }
                updated
            }
        }
    }

    private fun calculateInitialVx(foundationIndex: Int, foundationCount: Int): Float {
        val speed = random.nextDouble(250.0, 500.0).toFloat()
        val isRightSide = foundationIndex >= foundationCount / 2
        val sign = when {
            foundationIndex == 0 -> 1f
            foundationIndex == foundationCount - 1 -> -1f
            isRightSide -> if (random.nextDouble() < 0.75) -1f else 1f
            else -> if (random.nextDouble() < 0.75) 1f else -1f
        }
        return speed * sign
    }

    private fun calculateInitialVy(): Float {
        return -random.nextDouble(50.0, 200.0).toFloat()
    }
}
