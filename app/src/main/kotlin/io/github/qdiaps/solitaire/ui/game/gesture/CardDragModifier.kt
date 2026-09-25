package io.github.qdiaps.solitaire.ui.game.gesture

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.CardLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Modifier enabling drag-and-drop interactions for playable cards on the board.
 *
 * Captures root-relative screen bounds of the card via [onGloballyPositioned].
 * When dragged past touch slop:
 * - Triggers [onStartDrag] passing the exact origin position in root coordinates.
 * - Emits a tactile haptic feedback impulse on pickup.
 * - Tracks drag displacement via [DragDropState.onDragDelta].
 * - On release, hit-tests against [DropTargetRegistry] and domain rules via [DragDropState.onDropRelease],
 *   triggering [onValidDrop] on legal destinations or [DragDropState.snapBack] on invalid drops.
 * - On gesture cancel, smoothly returns the lifted card(s) to origin via [DragDropState.snapBack].
 *
 * If the touch gesture does not exceed touch slop, no drag is initiated and standard tap clicks
 * pass through unaffected.
 *
 * @param isEnabled Whether drag interactions are permitted (e.g. true only for face-up cards).
 * @param boardState Current game board state provider.
 * @param onStartDrag Callback initiating card drag in [DragDropState] given origin coordinates in root.
 * @param onValidDrop Callback invoked when card stack is legally dropped on a target destination.
 * @param dragDropState Optional state instance (defaults to [LocalDragDropState.current]).
 * @param dropTargetRegistry Optional target registry (defaults to [LocalDropTargetRegistry.current]).
 * @param hapticFeedback Optional haptic feedback provider (defaults to [LocalHapticFeedback.current]).
 * @param coroutineScope Coroutine scope for running drop resolution and snap-back animations.
 */
@Composable
fun Modifier.cardDragTarget(
    isEnabled: Boolean,
    boardState: () -> BoardState,
    onStartDrag: (originInRoot: Offset) -> Boolean,
    onValidDrop: (cards: List<Card>, source: CardLocation, target: CardLocation) -> Unit,
    dragDropState: DragDropState? = LocalDragDropState.current,
    dropTargetRegistry: DropTargetRegistry? = LocalDropTargetRegistry.current,
    hapticFeedback: HapticFeedback = LocalHapticFeedback.current,
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): Modifier {
    if (!isEnabled || dragDropState == null || dropTargetRegistry == null) {
        return this
    }

    var cardBoundsInRoot by remember { mutableStateOf(Rect.Zero) }

    return this
        .onGloballyPositioned { coordinates ->
            cardBoundsInRoot = coordinates.boundsInRoot()
        }
        .pointerInput(dragDropState, dropTargetRegistry, isEnabled) {
            detectDragGestures(
                onDragStart = {
                    val origin = cardBoundsInRoot.topLeft
                    val started = onStartDrag(origin)
                    if (started) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    if (dragDropState.isDragging) {
                        dragDropState.onDragDelta(dragAmount)
                    }
                },
                onDragEnd = {
                    if (dragDropState.isDragging) {
                        val cardWidthPx = cardBoundsInRoot.width
                        val cardHeightPx = cardBoundsInRoot.height
                        val currentPos = dragDropState.dragPosition
                        val stackBounds = Rect(
                            left = currentPos.x,
                            top = currentPos.y,
                            right = currentPos.x + cardWidthPx,
                            bottom = currentPos.y + cardHeightPx
                        )
                        coroutineScope.launch {
                            dragDropState.onDropRelease(
                                boardState = boardState(),
                                registry = dropTargetRegistry,
                                draggedBounds = stackBounds,
                                hapticFeedback = hapticFeedback,
                                onValidDrop = onValidDrop
                            )
                        }
                    }
                },
                onDragCancel = {
                    if (dragDropState.isDragging) {
                        coroutineScope.launch {
                            dragDropState.snapBack()
                        }
                    }
                }
            )
        }
}
