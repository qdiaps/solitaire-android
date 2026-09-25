package io.github.qdiaps.solitaire.ui.game.gesture

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
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
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * High-performance tactile haptic feedback provider for Solitaire.
 *
 * Combines hardware [Vibrator] effects (API 26+) with compose [HapticFeedback] fallback,
 * ensuring distinct tactile feedback on pickup, snap drop, and card dealing across all devices.
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    override fun playPickup() {
        if (!tryVibrateEffect(VibrationEffect.EFFECT_CLICK, 25L, 160)) {
            composeHaptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    override fun playSnap() {
        if (!tryVibrateEffect(VibrationEffect.EFFECT_HEAVY_CLICK, 40L, 220)) {
            composeHaptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    override fun playTick() {
        if (!tryVibrateEffect(VibrationEffect.EFFECT_TICK, 15L, 100)) {
            composeHaptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    private fun tryVibrateEffect(predefinedEffect: Int, fallbackDurationMs: Long, fallbackAmplitude: Int): Boolean {
        val vib = vibrator ?: return false
        if (!vib.hasVibrator()) return false

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vib.vibrate(VibrationEffect.createPredefined(predefinedEffect))
                return true
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(fallbackDurationMs, fallbackAmplitude))
                return true
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(fallbackDurationMs)
                return true
            }
        } catch (_: Exception) {
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
    val context = LocalContext.current
    val composeHaptics = LocalHapticFeedback.current
    return remember(context, composeHaptics) {
        AndroidSolitaireHaptics(context.applicationContext, composeHaptics)
    }
}
