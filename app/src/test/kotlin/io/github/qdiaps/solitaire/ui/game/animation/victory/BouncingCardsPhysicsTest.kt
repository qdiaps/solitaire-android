package io.github.qdiaps.solitaire.ui.game.animation.victory

import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BouncingCardsPhysicsTest {

    private val sampleCard = Card(Suit.SPADES, Rank.KING, isFaceUp = true)
    private val screenWidth = 1000f
    private val screenHeight = 1000f
    private val cardWidth = 100f
    private val cardHeight = 150f
    private val floorY = screenHeight - cardHeight // 850f

    @Nested
    @DisplayName("Gravity and Motion Tests")
    inner class GravityAndMotionTests {

        @Test
        @DisplayName("Gravity accelerates active falling card downward")
        fun `gravity accelerates active falling card downward`() {
            val physics = BouncingCardsPhysics(BouncingCardsPhysicsConfig(gravity = 1000f))
            val initial = BouncingCardParticle(
                card = sampleCard,
                initialX = 100f,
                initialY = 100f,
                width = cardWidth,
                height = cardHeight,
                vx = 200f,
                vy = 0f,
                isActive = true
            )

            val stepped = physics.step(initial, dtSeconds = 0.05f, screenWidth, screenHeight)

            // vy = 0 + 1000 * 0.05 = 50f
            assertEquals(50f, stepped.vy, 0.01f)
            // x = 100 + 200 * 0.05 = 110f
            assertEquals(110f, stepped.x, 0.01f)
            // y = 100 + 50 * 0.05 = 102.5f
            assertEquals(102.5f, stepped.y, 0.01f)
            assertFalse(stepped.isTerminated)
        }

        @Test
        @DisplayName("Inactive or terminated particles are not modified by physics step")
        fun `inactive or terminated particles are not modified by physics step`() {
            val physics = BouncingCardsPhysics()
            val inactive = BouncingCardParticle(
                card = sampleCard,
                initialX = 100f,
                initialY = 100f,
                width = cardWidth,
                height = cardHeight,
                vx = 200f,
                vy = 100f,
                isActive = false
            )
            val steppedInactive = physics.step(inactive, dtSeconds = 0.05f, screenWidth, screenHeight)
            assertEquals(inactive, steppedInactive)

            val terminated = inactive.copy(isActive = true, isTerminated = true)
            val steppedTerminated = physics.step(terminated, dtSeconds = 0.05f, screenWidth, screenHeight)
            assertEquals(terminated, steppedTerminated)
        }
    }

    @Nested
    @DisplayName("Floor Bounce Tests")
    inner class FloorBounceTests {

        @Test
        @DisplayName("Card bouncing off floor inverts vertical velocity scaled by restitution and clamps y to floor")
        fun `card bouncing off floor inverts vertical velocity scaled by restitution and clamps y to floor`() {
            val config = BouncingCardsPhysicsConfig(
                gravity = 1000f,
                floorRestitution = 0.8f
            )
            val physics = BouncingCardsPhysics(config)

            // Position card just above floor so next step penetrates floorY (850f)
            val initial = BouncingCardParticle(
                card = sampleCard,
                initialX = 100f,
                initialY = 840f,
                width = cardWidth,
                height = cardHeight,
                vx = 100f,
                vy = 500f,
                isActive = true
            )

            val stepped = physics.step(initial, dtSeconds = 0.05f, screenWidth, screenHeight)

            // Without bounce: vy = 500 + 1000 * 0.05 = 550f; y = 840 + 550 * 0.05 = 867.5f > 850f
            // With floor bounce: y clamped to floorY = 850f
            assertEquals(floorY, stepped.y, 0.01f)
            // vy inverted with 0.8 restitution: -550 * 0.8 = -440f
            assertEquals(-440f, stepped.vy, 0.01f)
            assertFalse(stepped.isTerminated)
        }

        @Test
        @DisplayName("When bounce velocity is below threshold, vertical velocity is clamped to zero to prevent jitter")
        fun `when bounce velocity is below threshold vertical velocity is clamped to zero`() {
            val config = BouncingCardsPhysicsConfig(
                gravity = 100f,
                floorRestitution = 0.5f,
                minBounceVelocity = 50f
            )
            val physics = BouncingCardsPhysics(config)

            // Very small downward velocity hitting the floor
            val initial = BouncingCardParticle(
                card = sampleCard,
                initialX = 100f,
                initialY = 849f,
                width = cardWidth,
                height = cardHeight,
                vx = 100f,
                vy = 20f,
                isActive = true
            )

            val stepped = physics.step(initial, dtSeconds = 0.05f, screenWidth, screenHeight)

            // vy = 20 + 100 * 0.05 = 25f. Inverted * 0.5 = -12.5f. Magnitude 12.5 < minBounceVelocity (50f) -> clamped to 0f
            assertEquals(floorY, stepped.y, 0.01f)
            assertEquals(0f, stepped.vy, 0.01f)
        }
    }

    @Nested
    @DisplayName("Boundary and Termination Tests")
    inner class BoundaryAndTerminationTests {

        @Test
        @DisplayName("In classic mode, card moving right terminates when exiting screen width")
        fun `in classic mode card moving right terminates when exiting screen width`() {
            val physics = BouncingCardsPhysics(BouncingCardsPhysicsConfig(bounceWalls = false))
            val initial = BouncingCardParticle(
                card = sampleCard,
                initialX = 990f,
                initialY = 500f,
                width = cardWidth,
                height = cardHeight,
                vx = 300f,
                vy = 0f,
                isActive = true
            )

            val stepped = physics.step(initial, dtSeconds = 0.05f, screenWidth, screenHeight)

            // x = 990 + 300 * 0.05 = 1005f > screenWidth (1000f)
            assertTrue(stepped.x > screenWidth)
            assertTrue(stepped.isTerminated)
        }

        @Test
        @DisplayName("In classic mode, card moving left terminates when exiting completely past zero")
        fun `in classic mode card moving left terminates when exiting completely past zero`() {
            val physics = BouncingCardsPhysics(BouncingCardsPhysicsConfig(bounceWalls = false))
            val initial = BouncingCardParticle(
                card = sampleCard,
                initialX = -90f,
                initialY = 500f,
                width = cardWidth,
                height = cardHeight,
                vx = -300f,
                vy = 0f,
                isActive = true
            )

            val stepped = physics.step(initial, dtSeconds = 0.05f, screenWidth, screenHeight)

            // x = -90 + (-300) * 0.05 = -105f < -cardWidth (-100f)
            assertTrue(stepped.x < -cardWidth)
            assertTrue(stepped.isTerminated)
        }

        @Test
        @DisplayName("When bounceWalls is true, card rebounds off left and right screen edges")
        fun `when bounceWalls is true card rebounds off screen edges`() {
            val physics = BouncingCardsPhysics(
                BouncingCardsPhysicsConfig(
                    bounceWalls = true,
                    wallRestitution = 0.9f
                )
            )

            // Hitting left wall
            val headingLeft = BouncingCardParticle(
                card = sampleCard,
                initialX = 5f,
                initialY = 500f,
                width = cardWidth,
                height = cardHeight,
                vx = -200f,
                vy = 0f,
                isActive = true
            )
            val bouncedLeft = physics.step(headingLeft, dtSeconds = 0.05f, screenWidth, screenHeight)
            assertEquals(0f, bouncedLeft.x, 0.01f)
            assertEquals(180f, bouncedLeft.vx, 0.01f) // -(-200) * 0.9 = 180f
            assertFalse(bouncedLeft.isTerminated)

            // Hitting right wall
            val maxRight = screenWidth - cardWidth // 900f
            val headingRight = BouncingCardParticle(
                card = sampleCard,
                initialX = 895f,
                initialY = 500f,
                width = cardWidth,
                height = cardHeight,
                vx = 200f,
                vy = 0f,
                isActive = true
            )
            val bouncedRight = physics.step(headingRight, dtSeconds = 0.05f, screenWidth, screenHeight)
            assertEquals(maxRight, bouncedRight.x, 0.01f)
            assertEquals(-180f, bouncedRight.vx, 0.01f) // -200 * 0.9 = -180f
            assertFalse(bouncedRight.isTerminated)
        }
    }
}
