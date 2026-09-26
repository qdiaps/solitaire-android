package io.github.qdiaps.solitaire.data.model

import androidx.compose.runtime.Immutable
import io.github.qdiaps.solitaire.domain.rules.DrawMode
import io.github.qdiaps.solitaire.ui.theme.CardBackStyle
import io.github.qdiaps.solitaire.ui.theme.CardFaceStyle
import io.github.qdiaps.solitaire.ui.theme.FeltTheme

/**
 * Immutable user configuration and preferences for Solitaire.
 *
 * @property drawMode Number of cards drawn per tap from the stock pile (Draw 1 or Draw 3).
 * @property isLeftHanded Mirrors top row layout placing stock and waste on the right.
 * @property feltTheme Table cloth visual theme.
 * @property cardBackStyle Graphic design on card backs.
 * @property cardFaceStyle Typography and scale of card faces.
 * @property soundEnabled Audio feedback toggle.
 * @property hapticsEnabled Tactile feedback toggle.
 * @property autoHintEnabled Automatically highlight productive moves when idle.
 */
@Immutable
data class GameSettings(
    val drawMode: DrawMode = DrawMode.DRAW_ONE,
    val isLeftHanded: Boolean = false,
    val feltTheme: FeltTheme = FeltTheme.DEFAULT,
    val cardBackStyle: CardBackStyle = CardBackStyle.DEFAULT,
    val cardFaceStyle: CardFaceStyle = CardFaceStyle.DEFAULT,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val autoHintEnabled: Boolean = false
)
