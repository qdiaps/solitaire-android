package io.github.qdiaps.solitaire.ui.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import io.github.qdiaps.solitaire.R

/**
 * Low-latency audio feedback provider for Solitaire.
 *
 * Utilizes Android [SoundPool] configured with [AudioAttributes.USAGE_GAME] for instantaneous,
 * zero-lag acoustic feedback on card interactions:
 * - [playSlide]: card pickup / drag initiation
 * - [playSnap]: card drop / sequence placement / foundation landing
 * - [playFlip]: stock draw / face-up turnover / 3D flip
 * - [playDeal]: new game deal / shuffle
 */
interface SolitaireAudio {
    var isEnabled: Boolean
    fun playSlide()
    fun playSnap()
    fun playFlip()
    fun playDeal()
    fun release()
}

/**
 * Production implementation of [SolitaireAudio] utilizing [SoundPool].
 */
class AndroidSolitaireAudio(context: Context) : SolitaireAudio {
    override var isEnabled: Boolean = true

    private val soundPool: SoundPool by lazy {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_GAME)
            .build()

        SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(audioAttributes)
            .build()
    }

    private val loadedSoundIds = mutableSetOf<Int>()

    private val slideSoundId: Int
    private val snapSoundId: Int
    private val flipSoundId: Int
    private val dealSoundId: Int

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedSoundIds.add(sampleId)
            }
        }
        slideSoundId = soundPool.load(context, R.raw.card_slide, 1)
        snapSoundId = soundPool.load(context, R.raw.card_snap, 1)
        flipSoundId = soundPool.load(context, R.raw.card_flip, 1)
        dealSoundId = soundPool.load(context, R.raw.card_deal, 1)
    }

    override fun playSlide() {
        if (!isEnabled) return
        if (loadedSoundIds.contains(slideSoundId)) {
            soundPool.play(slideSoundId, 0.65f, 0.65f, 1, 0, 1.0f)
        }
    }

    override fun playSnap() {
        if (!isEnabled) return
        if (loadedSoundIds.contains(snapSoundId)) {
            soundPool.play(snapSoundId, 0.85f, 0.85f, 2, 0, 1.0f)
        }
    }

    override fun playFlip() {
        if (!isEnabled) return
        if (loadedSoundIds.contains(flipSoundId)) {
            soundPool.play(flipSoundId, 0.80f, 0.80f, 1, 0, 1.0f)
        }
    }

    override fun playDeal() {
        if (!isEnabled) return
        if (loadedSoundIds.contains(dealSoundId)) {
            soundPool.play(dealSoundId, 0.85f, 0.85f, 2, 0, 1.0f)
        }
    }

    override fun release() {
        soundPool.release()
    }
}

/**
 * CompositionLocal providing [SolitaireAudio].
 */
val LocalSolitaireAudio: ProvidableCompositionLocal<SolitaireAudio> =
    staticCompositionLocalOf {
        object : SolitaireAudio {
            override var isEnabled: Boolean = true
            override fun playSlide() {}
            override fun playSnap() {}
            override fun playFlip() {}
            override fun playDeal() {}
            override fun release() {}
        }
    }

/**
 * Remembers and lifecycle-manages a platform [SolitaireAudio] instance.
 */
@Composable
fun rememberSolitaireAudio(enabled: Boolean = true): SolitaireAudio {
    if (LocalInspectionMode.current) {
        return remember {
            object : SolitaireAudio {
                override var isEnabled: Boolean = false
                override fun playSlide() {}
                override fun playSnap() {}
                override fun playFlip() {}
                override fun playDeal() {}
                override fun release() {}
            }
        }
    }
    val context = LocalContext.current
    val audio = remember(context) { AndroidSolitaireAudio(context.applicationContext ?: context) }
    DisposableEffect(audio) {
        onDispose {
            audio.release()
        }
    }
    audio.isEnabled = enabled
    return audio
}
