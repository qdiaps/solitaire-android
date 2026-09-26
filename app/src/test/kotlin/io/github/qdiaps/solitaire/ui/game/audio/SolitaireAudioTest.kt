package io.github.qdiaps.solitaire.ui.game.audio

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SolitaireAudioTest {

    private class RecordingSolitaireAudio : SolitaireAudio {
        override var isEnabled: Boolean = true
        var slideCount = 0
        var snapCount = 0
        var flipCount = 0
        var dealCount = 0
        var releaseCount = 0

        override fun playSlide() {
            if (!isEnabled) return
            slideCount++
        }

        override fun playSnap() {
            if (!isEnabled) return
            snapCount++
        }

        override fun playFlip() {
            if (!isEnabled) return
            flipCount++
        }

        override fun playDeal() {
            if (!isEnabled) return
            dealCount++
        }

        override fun release() {
            releaseCount++
        }
    }

    @Test
    fun `recording audio tracks all playback methods when enabled`() {
        val audio = RecordingSolitaireAudio()
        assertTrue(audio.isEnabled)
        assertEquals(0, audio.slideCount)
        assertEquals(0, audio.snapCount)
        assertEquals(0, audio.flipCount)
        assertEquals(0, audio.dealCount)
        assertEquals(0, audio.releaseCount)

        audio.playSlide()
        assertEquals(1, audio.slideCount)

        audio.playSnap()
        assertEquals(1, audio.snapCount)

        audio.playFlip()
        assertEquals(1, audio.flipCount)

        audio.playDeal()
        assertEquals(1, audio.dealCount)

        audio.release()
        assertEquals(1, audio.releaseCount)
    }

    @Test
    fun `audio playback is silenced when disabled`() {
        val audio = RecordingSolitaireAudio()
        audio.isEnabled = false
        assertFalse(audio.isEnabled)

        audio.playSlide()
        audio.playSnap()
        audio.playFlip()
        audio.playDeal()

        assertEquals(0, audio.slideCount)
        assertEquals(0, audio.snapCount)
        assertEquals(0, audio.flipCount)
        assertEquals(0, audio.dealCount)
    }

    @Test
    fun `default no-op implementation does not throw on any method`() {
        val noOp = object : SolitaireAudio {
            override var isEnabled: Boolean = true
            override fun playSlide() {}
            override fun playSnap() {}
            override fun playFlip() {}
            override fun playDeal() {}
            override fun release() {}
        }

        noOp.playSlide()
        noOp.playSnap()
        noOp.playFlip()
        noOp.playDeal()
        noOp.release()
    }
}
