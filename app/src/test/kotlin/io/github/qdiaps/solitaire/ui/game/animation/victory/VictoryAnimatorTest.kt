package io.github.qdiaps.solitaire.ui.game.animation.victory

import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.random.Random

class VictoryAnimatorTest {

    private fun createStandardWonFoundations(): List<List<Card>> {
        return Suit.entries.map { suit ->
            Rank.entries.map { rank -> Card(suit, rank, isFaceUp = true) }
        }
    }

    private val sampleSpec = VictoryStartSpec(
        foundations = createStandardWonFoundations(),
        foundationOrigins = listOf(
            FoundationOrigin(100f, 50f),
            FoundationOrigin(250f, 50f),
            FoundationOrigin(400f, 50f),
            FoundationOrigin(550f, 50f)
        ),
        screenWidth = 800f,
        screenHeight = 1200f,
        cardWidth = 80f,
        cardHeight = 120f
    )

    @Nested
    @DisplayName("Factory and Type Tests")
    inner class FactoryTests {

        @Test
        @DisplayName("VictoryAnimatorFactory creates ClassicBounceAnimator for CLASSIC_BOUNCE type")
        fun `factory creates ClassicBounceAnimator for CLASSIC_BOUNCE type`() {
            val animator = VictoryAnimatorFactory.create(VictoryAnimationType.CLASSIC_BOUNCE)
            assertNotNull(animator)
            assertEquals(VictoryAnimationType.CLASSIC_BOUNCE, animator.animationType)
            assertFalse(animator.isRunning)
            assertFalse(animator.isFinished)
            assertTrue(animator.activeParticles.isEmpty())
        }

        @Test
        @DisplayName("VictoryAnimationType has CLASSIC_BOUNCE entry")
        fun `VictoryAnimationType enum values include CLASSIC_BOUNCE`() {
            val entries = VictoryAnimationType.entries
            assertTrue(entries.contains(VictoryAnimationType.CLASSIC_BOUNCE))
        }
    }

    @Nested
    @DisplayName("Lifecycle Tests")
    inner class LifecycleTests {

        @Test
        @DisplayName("start initializes simulation and marks isRunning true")
        fun `start initializes simulation and marks isRunning true`() {
            val animator = VictoryAnimatorFactory.create(
                VictoryAnimationType.CLASSIC_BOUNCE,
                random = Random(42)
            )

            animator.start(sampleSpec)

            assertTrue(animator.isRunning)
            assertFalse(animator.isFinished)
            assertEquals(1, animator.activeParticles.size)
        }

        @Test
        @DisplayName("update advances active particle physics")
        fun `update advances active particle physics`() {
            val animator = VictoryAnimatorFactory.create(
                VictoryAnimationType.CLASSIC_BOUNCE,
                random = Random(42)
            )
            animator.start(sampleSpec)
            val initialParticle = animator.activeParticles.first()

            animator.update(0.50f)

            val updatedParticle = animator.activeParticles.first()
            assertTrue(updatedParticle.x != initialParticle.x) // moved laterally
            assertTrue(updatedParticle.y > initialParticle.y) // pulled downward by gravity
        }

        @Test
        @DisplayName("stop immediately halts animation and clears active state")
        fun `stop immediately halts animation and clears active state`() {
            val animator = VictoryAnimatorFactory.create(
                VictoryAnimationType.CLASSIC_BOUNCE,
                random = Random(42)
            )
            animator.start(sampleSpec)
            assertTrue(animator.isRunning)

            animator.stop()

            assertFalse(animator.isRunning)
            assertTrue(animator.isFinished)
        }
    }
}
