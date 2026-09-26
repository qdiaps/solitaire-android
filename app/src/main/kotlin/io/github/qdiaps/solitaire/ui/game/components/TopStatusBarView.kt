package io.github.qdiaps.solitaire.ui.game.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.ui.theme.SolitaireTheme

/**
 * Top status bar displaying Score, Moves counter, and formatted active play Timer.
 *
 * Sits at the top of the game screen above the cards, aligning horizontally with
 * [SolitaireTheme.cardDimensions.horizontalPadding].
 *
 * @param score Current game score.
 * @param moves Total valid moves made in the current session.
 * @param timeSeconds Elapsed active play time in seconds.
 * @param modifier Compose [Modifier] applied to this status bar.
 */
@Composable
fun TopStatusBarView(
    score: Int,
    moves: Int,
    timeSeconds: Long,
    modifier: Modifier = Modifier,
    onStatsClick: (() -> Unit)? = null
) {
    val dimensions = SolitaireTheme.cardDimensions
    val rowModifier = if (onStatsClick != null) {
        modifier
            .fillMaxWidth()
            .padding(horizontal = dimensions.horizontalPadding, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClickLabel = "View statistics", onClick = onStatsClick)
            .padding(horizontal = 4.dp, vertical = 4.dp)
    } else {
        modifier
            .fillMaxWidth()
            .padding(horizontal = dimensions.horizontalPadding, vertical = 8.dp)
    }

    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusItem(
            label = "SCORE",
            value = score.toString(),
            valueColor = SolitaireTheme.colors.scoreGold
        )
        StatusItem(
            label = "MOVES",
            value = moves.toString()
        )
        StatusItem(
            label = "TIME",
            value = formatTime(timeSeconds)
        )
    }
}

/**
 * Convenience overload of [TopStatusBarView] extracting score and moves from [BoardState].
 */
@Composable
fun TopStatusBarView(
    boardState: BoardState,
    timeSeconds: Long,
    modifier: Modifier = Modifier,
    onStatsClick: (() -> Unit)? = null
) {
    TopStatusBarView(
        score = boardState.score,
        moves = boardState.movesCount,
        timeSeconds = timeSeconds,
        modifier = modifier,
        onStatsClick = onStatsClick
    )
}

@Composable
private fun StatusItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = SolitaireTheme.colors.onFeltText
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = SolitaireTheme.typography.statusLabel,
            color = SolitaireTheme.colors.onFeltSubtle
        )
        Text(
            text = value,
            style = SolitaireTheme.typography.statusCounter,
            color = valueColor
        )
    }
}
