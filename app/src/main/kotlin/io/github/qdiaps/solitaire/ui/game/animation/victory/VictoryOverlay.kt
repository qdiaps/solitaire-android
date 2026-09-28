package io.github.qdiaps.solitaire.ui.game.animation.victory

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Full-screen hardware-accelerated overlay that executes and displays the victory animation sequence.
 *
 * Implements:
 * - Direct frame loop driven by [withFrameNanos] targeting 60/120 FPS.
 * - Draw-phase invalidation via frame-tick state to eliminate Composable recomposition overhead.
 * - Full-screen touch barrier intercepting taps for immediate tap-to-skip dismissal.
 * - Lifecycle-safe memory reclamation of frame buffers upon disposal.
 *
 * @param animator The [VictoryAnimator] driving particle mathematics and sequencing.
 * @param spriteCache The [CardSpriteCache] providing hardware-backed card face textures.
 * @param renderer The [VictoryRenderer] handling visual composition onto the Canvas.
 * @param onDismiss Invoked when the user taps anywhere on the screen to skip the celebration.
 * @param onAnimationFinished Invoked when all cards have naturally completed their bounce sequences.
 * @param modifier Compose [Modifier] applied to the overlay container.
 */
@Composable
fun VictoryOverlay(
    animator: VictoryAnimator,
    spriteCache: CardSpriteCache,
    modifier: Modifier = Modifier,
    renderer: VictoryRenderer = remember { ClassicBounceRenderer() },
    onDismiss: () -> Unit = {},
    onAnimationFinished: () -> Unit = onDismiss
) {
    val frameTick = remember { mutableLongStateOf(0L) }

    LaunchedEffect(animator, animator.isRunning) {
        var lastFrameTimeNanos = 0L
        while (animator.isRunning && !animator.isFinished) {
            withFrameNanos { frameTimeNanos ->
                if (lastFrameTimeNanos != 0L) {
                    val dtSeconds = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f
                    // Sub-step update internal physics and active particles
                    animator.update(dtSeconds)
                }
                lastFrameTimeNanos = frameTimeNanos
                frameTick.longValue = frameTimeNanos
            }
        }

        if (animator.isFinished) {
            onAnimationFinished()
        }
    }

    DisposableEffect(renderer) {
        onDispose {
            renderer.reset()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    animator.stop()
                    onDismiss()
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Read frameTick in draw phase to invalidate only the canvas draw pass (skips recomposition)
            @Suppress("UNUSED_VARIABLE")
            val tick = frameTick.longValue

            renderer.render(
                drawScope = this,
                animator = animator,
                spriteCache = spriteCache
            )
        }
    }
}
