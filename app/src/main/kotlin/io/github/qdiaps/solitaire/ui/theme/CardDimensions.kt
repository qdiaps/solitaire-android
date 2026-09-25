package io.github.qdiaps.solitaire.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Geometric dimensions and spacing model for the 7-column Klondike Solitaire board layout.
 *
 * All values are computed based on available screen width in portrait mode, preserving
 * the standard 5:7 playing card aspect ratio and optimal vertical cascade peek distances.
 */
@Immutable
data class CardDimensions(
    val cardWidth: Dp,
    val cardHeight: Dp,
    val faceDownPeek: Dp,
    val faceUpPeek: Dp,
    val cornerRadius: Dp,
    val horizontalPadding: Dp,
    val columnSpacing: Dp
) {
    companion object {
        const val NUM_COLUMNS: Int = 7
        const val ASPECT_RATIO_HEIGHT_TO_WIDTH: Float = 1.4f // 5:7 aspect ratio
        const val FACE_DOWN_PEEK_RATIO: Float = 0.20f
        const val FACE_UP_PEEK_RATIO: Float = 0.35f
        const val CORNER_RADIUS_RATIO: Float = 0.08f

        val DEFAULT_COLUMN_SPACING: Dp = 4.dp
        val DEFAULT_MAX_CARD_WIDTH: Dp = 76.dp

        /**
         * Calculates layout dimensions for 7 columns given [availableWidth].
         *
         * @param availableWidth Total width available for the board in dp.
         * @param maxCardWidth Maximum width allowed for a card before horizontal centering takes over.
         * @param columnSpacing Horizontal spacing between adjacent columns.
         * @return [CardDimensions] containing precise card and cascade measurements.
         * @throws IllegalArgumentException if [availableWidth] is not enough to host 7 columns with spacing.
         */
        fun calculate(
            availableWidth: Dp,
            maxCardWidth: Dp = DEFAULT_MAX_CARD_WIDTH,
            columnSpacing: Dp = DEFAULT_COLUMN_SPACING
        ): CardDimensions {
            val minRequiredSpacing = columnSpacing * (NUM_COLUMNS + 1)
            require(availableWidth > minRequiredSpacing) {
                "availableWidth ($availableWidth) must be greater than minimal spacing ($minRequiredSpacing)"
            }

            val defaultHorizontalPadding = columnSpacing
            val unconstrainedCardWidth = (availableWidth - (defaultHorizontalPadding * 2) - (columnSpacing * (NUM_COLUMNS - 1))) / NUM_COLUMNS.toFloat()

            val cardWidth: Dp
            val horizontalPadding: Dp

            if (unconstrainedCardWidth > maxCardWidth) {
                cardWidth = maxCardWidth
                val remainingSpace = availableWidth - (cardWidth * NUM_COLUMNS) - (columnSpacing * (NUM_COLUMNS - 1))
                horizontalPadding = remainingSpace / 2f
            } else {
                cardWidth = unconstrainedCardWidth
                horizontalPadding = defaultHorizontalPadding
            }

            val cardHeight = cardWidth * ASPECT_RATIO_HEIGHT_TO_WIDTH
            val faceDownPeek = cardHeight * FACE_DOWN_PEEK_RATIO
            val faceUpPeek = cardHeight * FACE_UP_PEEK_RATIO
            val cornerRadius = cardWidth * CORNER_RADIUS_RATIO

            return CardDimensions(
                cardWidth = cardWidth,
                cardHeight = cardHeight,
                faceDownPeek = faceDownPeek,
                faceUpPeek = faceUpPeek,
                cornerRadius = cornerRadius,
                horizontalPadding = horizontalPadding,
                columnSpacing = columnSpacing
            )
        }
    }
}
