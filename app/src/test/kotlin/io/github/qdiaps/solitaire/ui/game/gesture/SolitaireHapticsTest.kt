package io.github.qdiaps.solitaire.ui.game.gesture

import android.content.Context
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SolitaireHapticsTest {

    private class FakeHapticFeedback : HapticFeedback {
        val performedFeedbackTypes = mutableListOf<HapticFeedbackType>()
        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            performedFeedbackTypes.add(hapticFeedbackType)
        }
    }

    private class RecordingSolitaireHaptics : SolitaireHaptics {
        var pickupCount = 0
        var snapCount = 0
        var tickCount = 0

        override fun playPickup() {
            pickupCount++
        }

        override fun playSnap() {
            snapCount++
        }

        override fun playTick() {
            tickCount++
        }
    }

    @Test
    fun `recording haptics tracks all invocation types`() {
        val haptics = RecordingSolitaireHaptics()
        assertEquals(0, haptics.pickupCount)
        assertEquals(0, haptics.snapCount)
        assertEquals(0, haptics.tickCount)

        haptics.playPickup()
        assertEquals(1, haptics.pickupCount)

        haptics.playSnap()
        assertEquals(1, haptics.snapCount)

        haptics.playTick()
        assertEquals(1, haptics.tickCount)
    }

    @Test
    fun `default no-op implementation does not throw on any method`() {
        val noOp = object : SolitaireHaptics {
            override fun playPickup() {}
            override fun playSnap() {}
            override fun playTick() {}
        }

        noOp.playPickup()
        noOp.playSnap()
        noOp.playTick()
    }
}
