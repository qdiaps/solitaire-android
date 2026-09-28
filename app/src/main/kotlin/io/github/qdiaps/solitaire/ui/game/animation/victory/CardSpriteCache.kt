package io.github.qdiaps.solitaire.ui.game.animation.victory

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import io.github.qdiaps.solitaire.ui.theme.CardDimensions
import io.github.qdiaps.solitaire.ui.theme.CardFaceStyle
import io.github.qdiaps.solitaire.ui.theme.SolitaireColors
import io.github.qdiaps.solitaire.ui.theme.SolitaireTheme
import java.util.concurrent.ConcurrentHashMap

/**
 * Cache holding pre-rendered [ImageBitmap] card face sprites for all 52 standard cards.
 *
 * Pre-rendering eliminates vector path recalculations and typography layouts during the 60/120 FPS
 * victory animation loop.
 */
class CardSpriteCache(
    private val renderer: CardSpriteRenderer = DefaultCardSpriteRenderer()
) {
    private val sprites = ConcurrentHashMap<Pair<Suit, Rank>, ImageBitmap>(52)

    val size: Int
        get() = sprites.size

    val isPopulated: Boolean
        get() = sprites.size == 52

    val isEmpty: Boolean
        get() = sprites.isEmpty()

    /**
     * Retrieves the cached sprite for the given [card], or null if not yet populated.
     */
    fun get(card: Card): ImageBitmap? {
        return sprites[Pair(card.suit, card.rank)]
    }

    /**
     * Retrieves the cached sprite for the given [suit] and [rank], or null if not yet populated.
     */
    fun get(suit: Suit, rank: Rank): ImageBitmap? {
        return sprites[Pair(suit, rank)]
    }

    /**
     * Pre-renders and caches all 52 card face sprites.
     *
     * @param widthPx Rendered pixel width of the card.
     * @param heightPx Rendered pixel height of the card.
     * @param cardFaceStyle Typography scaling and emblem styling.
     * @param colors Theme palette colors.
     * @param density Screen density for layout conversion.
     * @param forceRefresh When true, clears existing sprites and re-renders all 52 cards.
     */
    fun populate(
        widthPx: Int,
        heightPx: Int,
        cardFaceStyle: CardFaceStyle = CardFaceStyle.DEFAULT,
        colors: SolitaireColors = SolitaireColors(),
        density: Density = Density(1f),
        forceRefresh: Boolean = false
    ) {
        if (!forceRefresh && isPopulated) return

        if (forceRefresh) {
            sprites.clear()
        }

        for (suit in Suit.entries) {
            for (rank in Rank.entries) {
                val card = Card(suit, rank, isFaceUp = true)
                val bitmap = renderer.renderCard(
                    card = card,
                    widthPx = widthPx,
                    heightPx = heightPx,
                    cardFaceStyle = cardFaceStyle,
                    colors = colors,
                    density = density
                )
                sprites[Pair(suit, rank)] = bitmap
            }
        }
    }

    /**
     * Evicts all cached sprites to immediately reclaim memory.
     */
    fun clear() {
        sprites.clear()
    }
}

/**
 * Creates and remembers a [CardSpriteCache] bound to the active [SolitaireTheme] and composable lifecycle.
 *
 * Automatically populates the 52 card sprites upon composition and safely evicts memory on disposal.
 */
@Composable
fun rememberCardSpriteCache(
    cardDimensions: CardDimensions = SolitaireTheme.cardDimensions,
    cardFaceStyle: CardFaceStyle = SolitaireTheme.cardFaceStyle,
    colors: SolitaireColors = SolitaireTheme.colors
): CardSpriteCache {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val widthPx = with(density) { cardDimensions.cardWidth.roundToPx() }
    val heightPx = with(density) { cardDimensions.cardHeight.roundToPx() }

    val cache = remember {
        CardSpriteCache(renderer = DefaultCardSpriteRenderer(textMeasurer))
    }

    LaunchedEffect(widthPx, heightPx, cardFaceStyle, colors) {
        cache.populate(
            widthPx = widthPx,
            heightPx = heightPx,
            cardFaceStyle = cardFaceStyle,
            colors = colors,
            density = density,
            forceRefresh = true
        )
    }

    DisposableEffect(cache) {
        onDispose {
            cache.clear()
        }
    }

    return cache
}
