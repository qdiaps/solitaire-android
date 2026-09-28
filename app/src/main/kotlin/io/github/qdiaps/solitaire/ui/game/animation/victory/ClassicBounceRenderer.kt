package io.github.qdiaps.solitaire.ui.game.animation.victory

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection

/**
 * Strategy function stamping active card particles onto an offscreen canvas.
 */
typealias ParticleStamper = (
    canvas: Canvas,
    particles: List<BouncingCardParticle>,
    spriteCache: CardSpriteCache,
    width: Int,
    height: Int,
    density: Density,
    layoutDirection: LayoutDirection
) -> Unit

private class ReusableParticleStamper : ParticleStamper {
    private val reusableDrawScope = CanvasDrawScope()
    private var cachedWidth: Int = -1
    private var cachedHeight: Int = -1
    private var cachedSize: Size = Size.Zero

    override fun invoke(
        canvas: Canvas,
        particles: List<BouncingCardParticle>,
        spriteCache: CardSpriteCache,
        width: Int,
        height: Int,
        density: Density,
        layoutDirection: LayoutDirection
    ) {
        if (cachedWidth != width || cachedHeight != height) {
            cachedWidth = width
            cachedHeight = height
            cachedSize = Size(width.toFloat(), height.toFloat())
        }

        reusableDrawScope.draw(
            density = density,
            layoutDirection = layoutDirection,
            canvas = canvas,
            size = cachedSize
        ) {
            val count = particles.size
            for (i in 0 until count) {
                val particle = particles[i]
                if (particle.isActive && !particle.isTerminated) {
                    val sprite = spriteCache.get(particle.card)
                    if (sprite != null) {
                        drawImage(
                            image = sprite,
                            dstOffset = IntOffset(particle.x.toInt(), particle.y.toInt())
                        )
                    }
                }
            }
        }
    }
}

private val defaultParticleStamper: ParticleStamper = ReusableParticleStamper()

/**
 * Renders the iconic Windows Solitaire classic bouncing card cascade.
 *
 * Maintains an offscreen hardware-backed trail buffer to accumulate persistent motion trails
 * (stamping active card positions on each frame) without clearing historical paths.
 *
 * @param bitmapFactory Factory lambda for creating offscreen [ImageBitmap] instances.
 * @param canvasFactory Factory lambda for creating [Canvas] instances bound to bitmaps.
 * @param particleStamper Strategy lambda for drawing particles onto the offscreen canvas.
 */
class ClassicBounceRenderer(
    private val bitmapFactory: (width: Int, height: Int) -> ImageBitmap = { w, h ->
        ImageBitmap(w, h, ImageBitmapConfig.Argb8888)
    },
    private val canvasFactory: (ImageBitmap) -> Canvas = { bitmap ->
        Canvas(bitmap)
    },
    private val particleStamper: ParticleStamper = defaultParticleStamper
) : VictoryRenderer {

    override val animationType: VictoryAnimationType = VictoryAnimationType.CLASSIC_BOUNCE

    var trailBitmap: ImageBitmap? = null
        private set

    private var trailCanvas: Canvas? = null

    val isTrailInitialized: Boolean
        get() = trailBitmap != null

    override fun render(
        drawScope: DrawScope,
        animator: VictoryAnimator,
        spriteCache: CardSpriteCache
    ) {
        val width = drawScope.size.width.toInt().coerceAtLeast(1)
        val height = drawScope.size.height.toInt().coerceAtLeast(1)

        // 1. Initialize or resize offscreen trail bitmap if needed
        val currentTrail = trailBitmap
        val activeTrailBitmap = if (currentTrail == null || currentTrail.width != width || currentTrail.height != height) {
            val newBitmap = bitmapFactory(width, height)
            trailBitmap = newBitmap
            trailCanvas = canvasFactory(newBitmap)
            newBitmap
        } else {
            currentTrail
        }

        val canvas = trailCanvas ?: return

        // 2. Stamp active particles onto offscreen trail canvas
        val activeParticles = animator.activeParticles
        if (activeParticles.isNotEmpty()) {
            particleStamper(
                canvas,
                activeParticles,
                spriteCache,
                width,
                height,
                drawScope,
                drawScope.layoutDirection
            )
        }

        // 3. Blit trail buffer onto active screen DrawScope
        drawScope.drawImage(activeTrailBitmap)
    }

    override fun reset() {
        trailBitmap = null
        trailCanvas = null
    }
}
