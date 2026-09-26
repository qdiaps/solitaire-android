package io.github.qdiaps.solitaire.ui.game.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.ui.game.audio.LocalSolitaireAudio
import io.github.qdiaps.solitaire.ui.theme.CardBackStyle
import io.github.qdiaps.solitaire.ui.theme.CardFaceStyle
import io.github.qdiaps.solitaire.ui.theme.SolitaireTheme

/**
 * Renders an individual playing card in either face-up or face-down state.
 *
 * Performs a smooth 3D flip animation around the Y-axis when a previously hidden (face-down)
 * card is exposed face-up, accompanied by card turnover sound feedback.
 *
 * @param card Domain [Card] model.
 * @param modifier Compose [Modifier] applied to this card.
 * @param cardBackStyle Visual pattern style applied to face-down card back.
 * @param cardFaceStyle Typography and index scaling style applied to face-up card face.
 * @param elevation Shadow elevation (defaults to 2.dp, or 12.dp when lifted in drag overlay).
 * @param isHighlighted Whether an active hint border is drawn around the card.
 * @param onClick Optional tap callback.
 */
@Composable
fun CardView(
    card: Card,
    modifier: Modifier = Modifier,
    cardBackStyle: CardBackStyle = SolitaireTheme.cardBackStyle,
    cardFaceStyle: CardFaceStyle = SolitaireTheme.cardFaceStyle,
    elevation: Dp = 2.dp,
    isHighlighted: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val dimensions = SolitaireTheme.cardDimensions
    val colors = SolitaireTheme.colors
    val density = LocalDensity.current
    val shape = RoundedCornerShape(dimensions.cornerRadius)
    val solitaireAudio = LocalSolitaireAudio.current

    var previousFaceUp by remember(card.id) { mutableStateOf(card.isFaceUp) }
    val flipAnimatable = remember(card.id) { Animatable(if (card.isFaceUp) 1f else 0f) }

    LaunchedEffect(card.isFaceUp) {
        if (!previousFaceUp && card.isFaceUp) {
            solitaireAudio.playFlip()
            // Animate 3D turnover when card is uncovered
            flipAnimatable.snapTo(0f)
            flipAnimatable.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
            )
            previousFaceUp = true
        } else {
            flipAnimatable.snapTo(if (card.isFaceUp) 1f else 0f)
            previousFaceUp = card.isFaceUp
        }
    }

    val isFlipping = flipAnimatable.value < 1f && card.isFaceUp
    val rotationY = if (isFlipping) {
        if (flipAnimatable.value <= 0.5f) {
            flipAnimatable.value * 180f
        } else {
            (flipAnimatable.value - 1f) * 180f
        }
    } else {
        0f
    }

    val renderFaceUp = if (isFlipping) {
        flipAnimatable.value > 0.5f
    } else {
        card.isFaceUp
    }

    val borderModifier = if (isHighlighted) {
        Modifier.border(width = 2.dp, color = colors.hintHighlight, shape = shape)
    } else {
        Modifier.border(width = 1.dp, color = colors.cardBorder, shape = shape)
    }

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(dimensions.cardWidth, dimensions.cardHeight)
            .shadow(elevation = elevation, shape = shape)
            .graphicsLayer {
                if (isFlipping) {
                    this.rotationY = rotationY
                    cameraDistance = 12f * density.density
                }
            }
            .clip(shape)
            .then(borderModifier)
            .then(clickableModifier)
    ) {
        if (renderFaceUp) {
            FaceUpCardContent(card = card, cardFaceStyle = cardFaceStyle)
        } else {
            CardBackView(style = cardBackStyle)
        }
    }
}

/**
 * Face-up card layout showing corner indices (rank + mini suit) and a center emblem.
 */
@Composable
private fun FaceUpCardContent(
    card: Card,
    cardFaceStyle: CardFaceStyle
) {
    val colors = SolitaireTheme.colors
    val typography = SolitaireTheme.typography
    val dimensions = SolitaireTheme.cardDimensions

    val rankTextStyle = if (cardFaceStyle == SolitaireTheme.cardFaceStyle) {
        typography.cardRank
    } else {
        typography.cardRank.copy(
            fontFamily = cardFaceStyle.fontFamily,
            fontWeight = cardFaceStyle.fontWeight,
            fontSize = (14f * cardFaceStyle.rankTextScale).sp
        )
    }

    val suitColor = if (card.suit.isRed) colors.cardRed else colors.cardBlack
    val indexPaddingHorizontal = dimensions.cardWidth * 0.08f
    val indexPaddingVertical = dimensions.cardHeight * 0.05f
    val cornerEmblemSize = dimensions.cardWidth * 0.22f * cardFaceStyle.cornerEmblemScale
    val centerEmblemSize = dimensions.cardWidth * 0.44f * cardFaceStyle.centerEmblemScale

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.cardBackground)
    ) {
        // Top-left index
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = indexPaddingHorizontal, top = indexPaddingVertical),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = card.rank.displayLabel,
                style = rankTextStyle,
                color = suitColor
            )
            SuitEmblem(
                suit = card.suit,
                color = suitColor,
                modifier = Modifier.size(cornerEmblemSize)
            )
        }

        // Center emblem
        SuitEmblem(
            suit = card.suit,
            color = suitColor,
            modifier = Modifier
                .size(centerEmblemSize)
                .align(Alignment.Center)
        )

        // Bottom-right index (mirrored 180 degrees)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = indexPaddingHorizontal, bottom = indexPaddingVertical)
                .graphicsLayer(rotationZ = 180f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = card.rank.displayLabel,
                style = rankTextStyle,
                color = suitColor
            )
            SuitEmblem(
                suit = card.suit,
                color = suitColor,
                modifier = Modifier.size(cornerEmblemSize)
            )
        }
    }
}
