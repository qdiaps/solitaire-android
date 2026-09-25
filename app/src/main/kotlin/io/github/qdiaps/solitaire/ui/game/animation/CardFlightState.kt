package io.github.qdiaps.solitaire.ui.game.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import io.github.qdiaps.solitaire.domain.model.Card

/**
 * CompositionLocal providing access to the screen's [CardFlightState].
 */
val LocalCardFlightState: ProvidableCompositionLocal<CardFlightState?> =
    compositionLocalOf { null }

/**
 * Represents a card or stack in active flight across the screen.
 *
 * @param cards Cards participating in the flight (bottom to top).
 * @param startOffset Starting root coordinate of the top-most card in the flight stack.
 * @param targetOffset Destination root coordinate.
 * @param isStockFlip Whether this flight is from Stock to Waste with a 3D flip.
 * @param durationMillis Duration of the flight animation in milliseconds.
 */
class CardFlight(
    val cards: List<Card>,
    val startOffset: Offset,
    val targetOffset: Offset,
    val isStockFlip: Boolean = false,
    val durationMillis: Int = 180
) {
    val animatable = Animatable(0f)

    /**
     * Current interpolated root coordinate of the flying stack.
     */
    val currentOffset: Offset
        get() {
            val progress = animatable.value
            return Offset(
                x = startOffset.x + (targetOffset.x - startOffset.x) * progress,
                y = startOffset.y + (targetOffset.y - startOffset.y) * progress
            )
        }

    /**
     * Y-axis 3D rotation in degrees (used during Stock -> Waste card flip).
     */
    val rotationY: Float
        get() {
            if (!isStockFlip) return 0f
            val progress = animatable.value
            return if (progress <= 0.5f) {
                progress * 180f
            } else {
                (progress - 1f) * 180f
            }
        }

    /**
     * Whether the flying card should render face-up or face-down.
     */
    val showCardFace: Boolean
        get() {
            if (!isStockFlip) return true
            return animatable.value > 0.5f
        }
}

/**
 * State manager for card flight animations (Smart Tap, Stock Draw flip).
 */
class CardFlightState {

    /**
     * The flight currently in progress, or `null` if idle.
     */
    var activeFlight by mutableStateOf<CardFlight?>(null)
        private set

    /**
     * Checks if the specified [card] is currently flying across the screen.
     * When true, source slots hide this card to prevent duplicate ghost cards.
     */
    fun isCardFlying(card: Card): Boolean {
        return activeFlight?.cards?.any { it.id == card.id } == true
    }

    /**
     * Starts an animated flight for [cards] from [startOffset] to [targetOffset].
     *
     * @param cards The cards being animated.
     * @param startOffset Starting screen coordinate.
     * @param targetOffset Destination screen coordinate.
     * @param isStockFlip Whether to perform a 3D flip (Stock to Waste).
     * @param durationMillis Animation duration (default 180ms).
     * @param onComplete Callback invoked upon arrival at [targetOffset].
     */
    suspend fun startFlight(
        cards: List<Card>,
        startOffset: Offset,
        targetOffset: Offset,
        isStockFlip: Boolean = false,
        durationMillis: Int = 180,
        onComplete: () -> Unit
    ) {
        val flight = CardFlight(
            cards = cards,
            startOffset = startOffset,
            targetOffset = targetOffset,
            isStockFlip = isStockFlip,
            durationMillis = durationMillis
        )
        activeFlight = flight

        try {
            flight.animatable.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = durationMillis,
                    easing = FastOutSlowInEasing
                )
            )
            onComplete()
        } finally {
            activeFlight = null
        }
    }

    /**
     * Immediately cancels any active flight without invoking the arrival callback.
     */
    fun cancelFlight() {
        activeFlight = null
    }
}

/**
 * Remembers an instance of [CardFlightState] across recompositions.
 */
@Composable
fun rememberCardFlightState(): CardFlightState = remember { CardFlightState() }
