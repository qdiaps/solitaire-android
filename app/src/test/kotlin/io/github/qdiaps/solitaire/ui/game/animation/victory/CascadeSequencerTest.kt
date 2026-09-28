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
import kotlin.random.Random

class CascadeSequencerTest {

    private fun createStandardWonFoundations(): List<List<Card>> {
        return Suit.entries.map { suit ->
            Rank.entries.map { rank -> Card(suit, rank, isFaceUp = true) }
        }
    }

    private val sampleOrigins = listOf(
        FoundationOrigin(100f, 50f),
        FoundationOrigin(250f, 50f),
        FoundationOrigin(400f, 50f),
        FoundationOrigin(550f, 50f)
    )

    private val screenWidth = 800f
    private val screenHeight = 1200f
    private val cardWidth = 80f
    private val cardHeight = 120f

    @Nested
    @DisplayName("Initialization and Ordering Tests")
    inner class InitializationTests {

        @Test
        @DisplayName("Initializes exactly 52 cards ordered from Kings down to Aces cycling foundations")
        fun `initializes exactly 52 cards ordered from Kings down to Aces cycling foundations`() {
            val sequencer = CascadeSequencer(
                config = CascadeConfig(releaseIntervalMs = 150L, cardWidth = cardWidth, cardHeight = cardHeight),
                random = Random(42)
            )

            val foundations = createStandardWonFoundations()
            sequencer.initialize(
                foundations = foundations,
                origins = sampleOrigins,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                cardWidth = cardWidth,
                cardHeight = cardHeight
            )

            // Total 52 cards: 1 active immediately at t=0, 51 queued
            assertEquals(1, sequencer.activeCards.size)
            assertEquals(51, sequencer.queuedCards.size)
            assertFalse(sequencer.isAllReleased)
            assertFalse(sequencer.isFinished)

            // First active card is the King of foundation 3 (or foundation 0 depending on right-to-left)
            assertEquals(Rank.KING, sequencer.activeCards.first().card.rank)
            assertTrue(sequencer.activeCards.first().isActive)
        }

        @Test
        @DisplayName("Cards receive origin coordinates matching their foundation index")
        fun `cards receive origin coordinates matching their foundation index`() {
            val sequencer = CascadeSequencer(random = Random(42))
            val foundations = createStandardWonFoundations()

            sequencer.initialize(
                foundations = foundations,
                origins = sampleOrigins,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                cardWidth = cardWidth,
                cardHeight = cardHeight
            )

            val firstCard = sequencer.activeCards.first()
            val matchingOrigin = sampleOrigins.any { it.x == firstCard.x && it.y == firstCard.y }
            assertTrue(matchingOrigin)
        }
    }

    @Nested
    @DisplayName("Timed Release Tests")
    inner class TimedReleaseTests {

        @Test
        @DisplayName("Stepping time advances elapsed time and releases queued cards at configured intervals")
        fun `stepping time advances elapsed time and releases queued cards at configured intervals`() {
            val sequencer = CascadeSequencer(
                config = CascadeConfig(releaseIntervalMs = 150L),
                random = Random(42)
            )
            sequencer.initialize(
                foundations = createStandardWonFoundations(),
                origins = sampleOrigins,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                cardWidth = cardWidth,
                cardHeight = cardHeight
            )

            assertEquals(1, sequencer.activeCards.size)
            assertEquals(51, sequencer.queuedCards.size)

            // Advance by 100ms: not enough for next release
            sequencer.step(deltaTimeSeconds = 0.10f, screenWidth, screenHeight)
            assertEquals(1, sequencer.activeCards.size)
            assertEquals(51, sequencer.queuedCards.size)

            // Advance by another 60ms (total 160ms >= 150ms): releases 2nd card
            sequencer.step(deltaTimeSeconds = 0.06f, screenWidth, screenHeight)
            assertEquals(2, sequencer.activeCards.size)
            assertEquals(50, sequencer.queuedCards.size)

            // Advance by 300ms (2 more intervals): releases 2 more cards -> total 4 active
            sequencer.step(deltaTimeSeconds = 0.30f, screenWidth, screenHeight)
            assertEquals(4, sequencer.activeCards.size)
            assertEquals(48, sequencer.queuedCards.size)
        }

        @Test
        @DisplayName("Releases all 52 cards over total release duration and flags isAllReleased")
        fun `releases all 52 cards over total release duration and flags isAllReleased`() {
            val sequencer = CascadeSequencer(
                config = CascadeConfig(releaseIntervalMs = 100L),
                random = Random(42)
            )
            sequencer.initialize(
                foundations = createStandardWonFoundations(),
                origins = sampleOrigins,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                cardWidth = cardWidth,
                cardHeight = cardHeight
            )

            // Total 51 remaining releases * 100ms = 5.1s
            sequencer.step(deltaTimeSeconds = 5.5f, screenWidth, screenHeight)

            assertEquals(52, sequencer.activeCards.size)
            assertEquals(0, sequencer.queuedCards.size)
            assertTrue(sequencer.isAllReleased)
        }
    }

    @Nested
    @DisplayName("Completion and Lifecycle Tests")
    inner class CompletionAndLifecycleTests {

        @Test
        @DisplayName("isFinished becomes true once all cards are released and all have terminated")
        fun `isFinished becomes true once all cards are released and all have terminated`() {
            val physics = BouncingCardsPhysics(
                BouncingCardsPhysicsConfig(bounceWalls = false, gravity = 2000f)
            )
            val sequencer = CascadeSequencer(
                config = CascadeConfig(releaseIntervalMs = 50L),
                physics = physics,
                random = Random(42)
            )
            sequencer.initialize(
                foundations = createStandardWonFoundations(),
                origins = sampleOrigins,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                cardWidth = cardWidth,
                cardHeight = cardHeight
            )

            // Run simulation long enough for all cards to be released and bounce off screen
            for (step in 1..400) {
                sequencer.step(deltaTimeSeconds = 0.05f, screenWidth, screenHeight)
                if (sequencer.isFinished) break
            }

            assertTrue(sequencer.isAllReleased)
            assertTrue(sequencer.isFinished)
            assertTrue(sequencer.activeCards.all { it.isTerminated })
        }
    }
}
