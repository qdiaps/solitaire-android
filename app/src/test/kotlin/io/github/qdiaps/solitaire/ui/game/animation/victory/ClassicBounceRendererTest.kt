package io.github.qdiaps.solitaire.ui.game.animation.victory

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import io.github.qdiaps.solitaire.ui.theme.CardFaceStyle
import io.github.qdiaps.solitaire.ui.theme.SolitaireColors
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy

class ClassicBounceRendererTest {

    private class FakeImageBitmap(
        override val width: Int,
        override val height: Int,
        override val config: ImageBitmapConfig = ImageBitmapConfig.Argb8888,
        override val colorSpace: ColorSpace = ColorSpaces.Srgb,
        override val hasAlpha: Boolean = true
    ) : ImageBitmap {
        override fun readPixels(
            buffer: IntArray,
            startX: Int,
            startY: Int,
            width: Int,
            height: Int,
            bufferOffset: Int,
            stride: Int
        ) {}
        override fun prepareToDraw() {}
    }

    private class FakeCardSpriteRenderer : CardSpriteRenderer {
        override fun renderCard(
            card: Card,
            widthPx: Int,
            heightPx: Int,
            cardFaceStyle: CardFaceStyle,
            colors: SolitaireColors,
            density: Density
        ): ImageBitmap = FakeImageBitmap(widthPx, heightPx)
    }

    private fun createFakeCanvas(): Canvas {
        return Proxy.newProxyInstance(
            Canvas::class.java.classLoader,
            arrayOf(Canvas::class.java)
        ) { _, method, _ ->
            when (method.returnType) {
                java.lang.Boolean.TYPE -> false
                java.lang.Integer.TYPE -> 0
                java.lang.Float.TYPE -> 0f
                java.lang.Long.TYPE -> 0L
                java.lang.Double.TYPE -> 0.0
                else -> null
            }
        } as Canvas
    }

    private fun createFakeDrawScope(
        width: Float = 800f,
        height: Float = 1200f,
        onDrawImage: (ImageBitmap) -> Unit = {}
    ): DrawScope {
        val packedSize = (java.lang.Float.floatToRawIntBits(width).toLong() shl 32) or
                (java.lang.Float.floatToRawIntBits(height).toLong() and 0xFFFFFFFFL)

        return Proxy.newProxyInstance(
            DrawScope::class.java.classLoader,
            arrayOf(DrawScope::class.java)
        ) { _, method, args ->
            if (method.name.startsWith("drawImage")) {
                val bitmap = args?.firstOrNull { it is ImageBitmap } as? ImageBitmap
                if (bitmap != null) onDrawImage(bitmap)
                null
            } else when (method.name) {
                "getSize-NH-jbRc" -> packedSize
                "getSize" -> Size(width, height)
                "getLayoutDirection" -> LayoutDirection.Ltr
                "getDensity" -> 1f
                "getFontScale" -> 1f
                else -> when (method.returnType) {
                    java.lang.Boolean.TYPE -> false
                    java.lang.Integer.TYPE -> 0
                    java.lang.Float.TYPE -> 0f
                    java.lang.Long.TYPE -> 0L
                    java.lang.Double.TYPE -> 0.0
                    else -> null
                }
            }
        } as DrawScope
    }

    @Nested
    @DisplayName("Configuration and Type Tests")
    inner class ConfigurationTests {

        @Test
        @DisplayName("Renderer targets CLASSIC_BOUNCE animation type")
        fun `renderer targets CLASSIC_BOUNCE animation type`() {
            val renderer = ClassicBounceRenderer(
                bitmapFactory = { w, h -> FakeImageBitmap(w, h) },
                canvasFactory = { createFakeCanvas() }
            )
            assertEquals(VictoryAnimationType.CLASSIC_BOUNCE, renderer.animationType)
            assertFalse(renderer.isTrailInitialized)
        }
    }

    @Nested
    @DisplayName("Reset and Memory Eviction Tests")
    inner class ResetTests {

        @Test
        @DisplayName("reset clears trail bitmap buffer and resets initialization state")
        fun `reset clears trail bitmap buffer and resets initialization state`() {
            var createdBitmapCount = 0
            var stampedParticleCount = 0
            var drawnImageCount = 0

            val renderer = ClassicBounceRenderer(
                bitmapFactory = { w, h ->
                    createdBitmapCount++
                    FakeImageBitmap(w, h)
                },
                canvasFactory = { createFakeCanvas() },
                particleStamper = { _, particles, spriteCache, _, _, _, _ ->
                    for (p in particles) {
                        if (p.isActive && !p.isTerminated) {
                            val sprite = spriteCache.get(p.card)
                            if (sprite != null) {
                                stampedParticleCount++
                            }
                        }
                    }
                }
            )

            assertFalse(renderer.isTrailInitialized)
            assertNull(renderer.trailBitmap)

            val spriteCache = CardSpriteCache(renderer = FakeCardSpriteRenderer())
            spriteCache.populate(widthPx = 100, heightPx = 150)

            val animator = ClassicBounceAnimator()
            val sampleCard = Card(Suit.SPADES, Rank.KING, isFaceUp = true)
            animator.start(
                VictoryStartSpec(
                    foundations = listOf(listOf(sampleCard)),
                    foundationOrigins = listOf(FoundationOrigin(100f, 100f)),
                    screenWidth = 800f,
                    screenHeight = 1200f,
                    cardWidth = 100f,
                    cardHeight = 150f
                )
            )

            val fakeDrawScope = createFakeDrawScope { drawnImageCount++ }
            renderer.render(fakeDrawScope, animator, spriteCache)

            assertTrue(renderer.isTrailInitialized)
            assertNotNull(renderer.trailBitmap)
            assertEquals(1, createdBitmapCount)
            assertEquals(1, stampedParticleCount)
            assertEquals(1, drawnImageCount)

            // Resetting must evict buffer
            renderer.reset()
            assertFalse(renderer.isTrailInitialized)
            assertNull(renderer.trailBitmap)
        }
    }
}
