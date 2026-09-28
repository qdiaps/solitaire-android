package io.github.qdiaps.solitaire.ui.game.animation.victory

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.unit.Density
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

class CardSpriteCacheTest {

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
        var renderCallCount = 0

        override fun renderCard(
            card: Card,
            widthPx: Int,
            heightPx: Int,
            cardFaceStyle: CardFaceStyle,
            colors: SolitaireColors,
            density: Density
        ): ImageBitmap {
            renderCallCount++
            return FakeImageBitmap(widthPx, heightPx)
        }
    }

    private val widthPx = 180
    private val heightPx = 250

    @Nested
    @DisplayName("Initial State Tests")
    inner class InitialStateTests {

        @Test
        @DisplayName("Newly created cache is empty and not populated")
        fun `newly created cache is empty and not populated`() {
            val renderer = FakeCardSpriteRenderer()
            val cache = CardSpriteCache(renderer = renderer)

            assertEquals(0, cache.size)
            assertTrue(cache.isEmpty)
            assertFalse(cache.isPopulated)
            assertNull(cache.get(Card(Suit.SPADES, Rank.ACE, isFaceUp = true)))
            assertEquals(0, renderer.renderCallCount)
        }
    }

    @Nested
    @DisplayName("Population and Retrieval Tests")
    inner class PopulationAndRetrievalTests {

        @Test
        @DisplayName("Populate renders exactly 52 cards and caches them with matching dimensions")
        fun `populate renders exactly 52 cards and caches them with matching dimensions`() {
            val renderer = FakeCardSpriteRenderer()
            val cache = CardSpriteCache(renderer = renderer)

            cache.populate(widthPx = widthPx, heightPx = heightPx)

            assertEquals(52, cache.size)
            assertFalse(cache.isEmpty)
            assertTrue(cache.isPopulated)
            assertEquals(52, renderer.renderCallCount)

            for (suit in Suit.entries) {
                for (rank in Rank.entries) {
                    val card = Card(suit, rank, isFaceUp = true)
                    val sprite = cache.get(card)
                    assertNotNull(sprite, "Sprite must be present for $card")
                    assertEquals(widthPx, sprite!!.width)
                    assertEquals(heightPx, sprite.height)

                    val spriteBySuitRank = cache.get(suit, rank)
                    assertNotNull(spriteBySuitRank)
                }
            }
        }

        @Test
        @DisplayName("Subsequent populate without forceRefresh does not re-render")
        fun `subsequent populate without forceRefresh does not re-render`() {
            val renderer = FakeCardSpriteRenderer()
            val cache = CardSpriteCache(renderer = renderer)

            cache.populate(widthPx = widthPx, heightPx = heightPx)
            assertEquals(52, renderer.renderCallCount)

            cache.populate(widthPx = widthPx, heightPx = heightPx, forceRefresh = false)
            assertEquals(52, renderer.renderCallCount)
        }

        @Test
        @DisplayName("Populate with forceRefresh true re-renders all 52 cards")
        fun `populate with forceRefresh true re-renders all 52 cards`() {
            val renderer = FakeCardSpriteRenderer()
            val cache = CardSpriteCache(renderer = renderer)

            cache.populate(widthPx = widthPx, heightPx = heightPx)
            assertEquals(52, renderer.renderCallCount)

            val newWidthPx = 240
            val newHeightPx = 320
            cache.populate(widthPx = newWidthPx, heightPx = newHeightPx, forceRefresh = true)
            assertEquals(104, renderer.renderCallCount)
            assertEquals(52, cache.size)

            val aceOfSpades = cache.get(Suit.SPADES, Rank.ACE)
            assertNotNull(aceOfSpades)
            assertEquals(newWidthPx, aceOfSpades!!.width)
            assertEquals(newHeightPx, aceOfSpades.height)
        }
    }

    @Nested
    @DisplayName("Memory Eviction Tests")
    inner class MemoryEvictionTests {

        @Test
        @DisplayName("Clear removes all cached sprites and frees references")
        fun `clear removes all cached sprites and frees references`() {
            val renderer = FakeCardSpriteRenderer()
            val cache = CardSpriteCache(renderer = renderer)

            cache.populate(widthPx = widthPx, heightPx = heightPx)
            assertEquals(52, cache.size)
            assertTrue(cache.isPopulated)

            cache.clear()

            assertEquals(0, cache.size)
            assertTrue(cache.isEmpty)
            assertFalse(cache.isPopulated)
            assertNull(cache.get(Suit.HEARTS, Rank.KING))
        }
    }
}
