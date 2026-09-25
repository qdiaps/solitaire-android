package io.github.qdiaps.solitaire.ui.game.gesture

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.CardLocation
import io.github.qdiaps.solitaire.ui.game.audio.LocalSolitaireAudio
import kotlinx.coroutines.launch

/**
 * Modifier enabling smooth drag-and-drop card interaction.
 *
 * Implements ADR 003:
 * - Detects drag gestures past touch slop, allowing quick taps to pass through without delay.
 * - Measures root-relative bounds of the card for accurate hit-testing.
 * - Emits tactile haptic feedback and acoustic audio on card pickup and drop placement.
 * - Dispatches continuous drag delta to [DragDropState].
 * - On release, validates destination via [DropTargetRegistry] and [boardState].
 * - Automatically snaps back to origin with fast spring physics on invalid release.
 *
 * Uses [rememberUpdatedState] to ensure changing card references or board states never result
 * in stale gesture callbacks.
 *
 * @param isEnabled Whether drag interactions are permitted on this card.
 * @param dragDropState The shared [DragDropState] (defaults to ambient [LocalDragDropState]).
 * @param dropTargetRegistry The registry of valid drop targets (defaults to ambient [LocalDropTargetRegistry]).
 * @param boardState Provider lambda for current [BoardState].
 * @param onStartDrag Callback invoked when touch slop is exceeded; returns true if drag was accepted.
 * @param onValidDrop Callback invoked when card stack is legally dropped on a target.
 */
fun Modifier.cardDragTarget(
    isEnabled: Boolean = true,
    dragDropState: DragDropState? = null,
    dropTargetRegistry: DropTargetRegistry? = null,
    boardState: (() -> BoardState)? = null,
    onStartDrag: (originPosition: Offset) -> Boolean,
    onValidDrop: (cards: List<Card>, source: CardLocation, target: CardLocation) -> Unit
): Modifier = composed {
    if (!isEnabled) return@composed this

    val actualDragState = dragDropState ?: LocalDragDropState.current ?: return@composed this
    val actualRegistry = dropTargetRegistry ?: LocalDropTargetRegistry.current ?: return@composed this
    val solitaireHaptics = LocalSolitaireHaptics.current
    val solitaireAudio = LocalSolitaireAudio.current
    val coroutineScope = rememberCoroutineScope()

    val currentBoardState by rememberUpdatedState(boardState)
    val currentOnStartDrag by rememberUpdatedState(onStartDrag)
    val currentOnValidDrop by rememberUpdatedState(onValidDrop)

    var cardBoundsInRoot by remember { mutableStateOf(Rect.Zero) }

    this
        .onGloballyPositioned { coordinates ->
            cardBoundsInRoot = coordinates.boundsInRoot()
        }
        .pointerInput(actualDragState, actualRegistry) {
            detectDragGestures(
                onDragStart = {
                    val origin = cardBoundsInRoot.topLeft
                    val started = currentOnStartDrag(origin)
                    if (started) {
                        solitaireHaptics.playPickup()
                        solitaireAudio.playSlide()
                    }
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    actualDragState.onDragDelta(dragAmount)
                },
                onDragCancel = {
                    coroutineScope.launch {
                        actualDragState.snapBack()
                    }
                },
                onDragEnd = {
                    val state = currentBoardState?.invoke()
                    if (state == null) {
                        coroutineScope.launch { actualDragState.snapBack() }
                        return@detectDragGestures
                    }

                    val currentOffset = actualDragState.dragPosition
                    val stackBounds = Rect(
                        offset = currentOffset,
                        size = cardBoundsInRoot.size
                    )

                    coroutineScope.launch {
                        val dropped = actualDragState.onDropRelease(
                            boardState = state,
                            registry = actualRegistry,
                            draggedBounds = stackBounds,
                            hapticFeedback = null,
                            onValidDrop = currentOnValidDrop
                        )
                        if (dropped) {
                            solitaireHaptics.playSnap()
                            solitaireAudio.playSnap()
                        }
                    }
                }
            )
        }
}
