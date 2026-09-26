package io.github.qdiaps.solitaire.ui.game.gesture

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * High-performance tactile haptic feedback provider for Solitaire.
 *
 * Designed with universal hardware compatibility:
 * - On devices with high-end LRA motors (Pixel, Galaxy S), plays hardware predefined waveforms (EFFECT_CLICK, etc.).
 * - On rugged devices with heavy chassis and ERM rotor motors (Hotwav Cyber X, Oukitel, Blackview),
 *   detects lack of predefined waveform support via [Vibrator.areAllEffectsSupported] and falls back to
 *   calibrated one-shot pulses (45-90ms) with [VibrationAttributes.USAGE_HARDWARE_FEEDBACK] and [VibrationEffect.DEFAULT_AMPLITUDE].
 * - Falls back to Compose [HapticFeedback] if hardware vibrator is unavailable.
 */
interface SolitaireHaptics {
    fun playPickup()
    fun playSnap()
    fun playTick()
}

/**
 * Production implementation of [SolitaireHaptics] utilizing system vibrator and compose haptics.
 */
class AndroidSolitaireHaptics(
    private val context: Context,
    private val composeHaptics: HapticFeedback
) : SolitaireHaptics {

    private val vibrator: Vibrator? by lazy {
        @Suppress("DEPRECATION")
        val systemVibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (systemVibrator != null && systemVibrator.hasVibrator()) {
            systemVibrator
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            val defaultVibrator = vibratorManager?.defaultVibrator
            if (defaultVibrator != null && defaultVibrator.hasVibrator()) {
                defaultVibrator
            } else {
                systemVibrator
            }
        } else {
            systemVibrator
        }
    }

    private val legacyAudioAttributes: AudioAttributes by lazy {
        AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_GAME)
            .build()
    }

    override fun playPickup() {
        val played = tryVibrate(
            predefinedEffect = VibrationEffect.EFFECT_CLICK,
            durationMs = 65L,
            amplitude = 180
        )
        if (!played) {
            composeHaptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    override fun playSnap() {
        val played = tryVibrate(
            predefinedEffect = VibrationEffect.EFFECT_HEAVY_CLICK,
            durationMs = 90L,
            amplitude = 255
        )
        if (!played) {
            composeHaptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    override fun playTick() {
        val played = tryVibrate(
            predefinedEffect = VibrationEffect.EFFECT_TICK,
            durationMs = 45L,
            amplitude = 140
        )
        if (!played) {
            composeHaptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    private fun tryVibrate(
        predefinedEffect: Int,
        durationMs: Long,
        amplitude: Int
    ): Boolean {
        val vib = vibrator ?: return false
        if (!vib.hasVibrator()) return false

        try {
            // Check if hardware genuinely supports predefined effects (e.g. Pixel / premium LRA motors)
            val isPredefinedSupported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    vib.areAllEffectsSupported(predefinedEffect) == Vibrator.VIBRATION_EFFECT_SUPPORT_YES
                } catch (_: Throwable) {
                    false
                }
            } else {
                false
            }

            val effect = if (isPredefinedSupported && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                VibrationEffect.createPredefined(predefinedEffect)
            } else {
                // Fallback for ERM motors (Hotwav Cyber X, rugged devices, etc.) and devices without predefined effect support
                val effectiveAmplitude = if (vib.hasAmplitudeControl()) {
                    amplitude
                } else {
                    VibrationEffect.DEFAULT_AMPLITUDE
                }
                VibrationEffect.createOneShot(durationMs, effectiveAmplitude)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                vib.vibrate(
                    effect,
                    VibrationAttributes.createForUsage(VibrationAttributes.USAGE_HARDWARE_FEEDBACK)
                )
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(effect, legacyAudioAttributes)
            }
            return true
        } catch (_: Throwable) {
            return false
        }
    }
}

/**
 * CompositionLocal providing [SolitaireHaptics].
 */
val LocalSolitaireHaptics: ProvidableCompositionLocal<SolitaireHaptics> =
    staticCompositionLocalOf {
        object : SolitaireHaptics {
            override fun playPickup() {}
            override fun playSnap() {}
            override fun playTick() {}
        }
    }

/**
 * Remembers a platform [SolitaireHaptics] instance.
 */
@Composable
fun rememberSolitaireHaptics(): SolitaireHaptics {
    if (LocalInspectionMode.current) {
        return remember {
            object : SolitaireHaptics {
                override fun playPickup() {}
                override fun playSnap() {}
                override fun playTick() {}
            }
        }
    }
    val context = LocalContext.current
    val composeHaptics = LocalHapticFeedback.current
    return remember(context, composeHaptics) {
        AndroidSolitaireHaptics(context.applicationContext ?: context, composeHaptics)
    }
}
