package io.github.qdiaps.solitaire.ui.game.animation

import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.geometry.Offset
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private class TestFrameClock(private val frameTimeNanos: Long = 16_000_000L) : MonotonicFrameClock {
    private var time = 0L
    override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
        time += frameTimeNanos
        return onFrame(time)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CardFlightStateTest {

    private val cardA = Card(suit = Suit.HEARTS, rank = Rank.ACE, isFaceUp = true)
    private val cardK = Card(suit = Suit.SPADES, rank = Rank.KING, isFaceUp = false)

    @Test
    fun `initial state has no active flight`() {
        val state = CardFlightState()
        assertNull(state.activeFlight)
        assertFalse(state.isCardFlying(cardA))
    }

    @Test
    fun `cancelFlight resets active flight`() {
        val state = CardFlightState()
        state.cancelFlight()
        assertNull(state.activeFlight)
        assertFalse(state.isCardFlying(cardA))
    }

    @Test
    fun `CardFlight calculates linear interpolation between offsets`() {
        val flight = CardFlight(
            cards = listOf(cardA),
            startOffset = Offset(0f, 100f),
            targetOffset = Offset(200f, 500f)
        )

        // At progress 0
        assertEquals(Offset(0f, 100f), flight.currentOffset)
        assertEquals(0f, flight.rotationY, 0.001f)
        assertTrue(flight.showCardFace)
    }

    @Test
    fun `CardFlight with stock flip calculates 3D rotation and face visibility`() {
        val flight = CardFlight(
            cards = listOf(cardK),
            startOffset = Offset(0f, 0f),
            targetOffset = Offset(100f, 0f),
            isStockFlip = true
        )

        // At initial progress (0.0): rotation 0 deg, shows card back (not face)
        assertEquals(0f, flight.rotationY, 0.001f)
        assertFalse(flight.showCardFace)
    }

    @Test
    fun `startFlight executes to completion and invokes onComplete`() = runTest {
        val state = CardFlightState()
        var completed = false
        var wasFlyingDuringComplete = false

        withContext(TestFrameClock()) {
            state.startFlight(
                cards = listOf(cardA),
                startOffset = Offset(0f, 0f),
                targetOffset = Offset(100f, 200f),
                durationMillis = 180
            ) {
                wasFlyingDuringComplete = state.isCardFlying(cardA)
                completed = true
            }
        }

        assertTrue(completed)
        assertTrue(wasFlyingDuringComplete)
        assertNull(state.activeFlight)
        assertFalse(state.isCardFlying(cardA))
    }
}
