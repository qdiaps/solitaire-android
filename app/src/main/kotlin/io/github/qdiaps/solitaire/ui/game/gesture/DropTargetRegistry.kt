package io.github.qdiaps.solitaire.ui.game.gesture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import io.github.qdiaps.solitaire.domain.model.CardLocation

/**
 * CompositionLocal providing access to the screen's [DropTargetRegistry] across the Compose hierarchy.
 */
val LocalDropTargetRegistry: ProvidableCompositionLocal<DropTargetRegistry?> =
    compositionLocalOf { null }

/**
 * Registry storing screen hitboxes for drop target destinations (Foundation slots and Tableau columns).
 *
 * Coordinates are recorded as root-relative [Rect]s, enabling the global drag overlay
 * to evaluate drop destinations via bounding box geometric intersection.
 */
class DropTargetRegistry {
    private val targets = LinkedHashMap<CardLocation, Rect>()

    /**
     * Registers or updates the bounding box for a given [CardLocation].
     *
     * @param location Drop destination target.
     * @param bounds Absolute root-relative screen bounds of the target slot or column.
     */
    fun register(location: CardLocation, bounds: Rect) {
        targets[location] = bounds
    }

    /**
     * Unregisters a [CardLocation] from the registry.
     *
     * @param location Drop destination target to remove.
     */
    fun unregister(location: CardLocation) {
        targets.remove(location)
    }

    /**
     * Returns the bounding box of the specified [location], or `null` if not registered.
     */
    fun getBounds(location: CardLocation): Rect? = targets[location]

    /**
     * Clears all registered hitboxes.
     */
    fun clear() {
        targets.clear()
    }

    /**
     * An immutable snapshot of all currently active drop target hitboxes.
     */
    val activeTargets: Map<CardLocation, Rect>
        get() = targets.toMap()

    /**
     * Finds the best drop target matching [draggedBounds] based on maximum intersection area.
     *
     * @param draggedBounds Screen bounding box of the dragged card or stack.
     * @param minOverlapArea Minimum overlap area required to consider a target (defaults to 0f).
     * @return The best matching [CardLocation], or `null` if no target meets the criteria.
     */
    fun findBestTarget(draggedBounds: Rect, minOverlapArea: Float = 0f): CardLocation? {
        return findBestDropTarget(draggedBounds, targets, minOverlapArea)
    }
}

/**
 * Calculates the rectangular intersection area between two [Rect] bounds.
 *
 * Returns 0f if the rectangles do not overlap, are degenerate (width <= 0 or height <= 0),
 * or touch only along an edge/corner.
 *
 * @param rectA First rectangle.
 * @param rectB Second rectangle.
 * @return Positive overlap area in square pixels, or 0f if no intersection.
 */
fun calculateOverlapArea(rectA: Rect, rectB: Rect): Float {
    if (rectA.isEmpty || rectB.isEmpty) return 0f

    val overlapLeft = maxOf(rectA.left, rectB.left)
    val overlapTop = maxOf(rectA.top, rectB.top)
    val overlapRight = minOf(rectA.right, rectB.right)
    val overlapBottom = minOf(rectA.bottom, rectB.bottom)

    val width = overlapRight - overlapLeft
    val height = overlapBottom - overlapTop

    return if (width > 0f && height > 0f) width * height else 0f
}

/**
 * Calculates the proportion of [draggedBounds] that is covered by [targetBounds].
 *
 * @param draggedBounds The bounding box of the moving card.
 * @param targetBounds The bounding box of the candidate drop target slot.
 * @return Ratio in range [0.0..1.0].
 */
fun calculateOverlapRatio(draggedBounds: Rect, targetBounds: Rect): Float {
    if (draggedBounds.isEmpty || targetBounds.isEmpty) return 0f
    val draggedArea = draggedBounds.width * draggedBounds.height
    if (draggedArea <= 0f) return 0f
    return calculateOverlapArea(draggedBounds, targetBounds) / draggedArea
}

/**
 * Finds the drop target from [targets] that shares the largest intersection area with [draggedBounds].
 *
 * @param draggedBounds The bounding box of the moving card or stack.
 * @param targets Map of candidate [CardLocation] drop targets and their absolute screen bounds.
 * @param minOverlapArea Minimum intersection area required for a target to be considered.
 *                       Defaults to 0f (any non-zero overlap is eligible).
 * @return The [CardLocation] with the greatest overlap area exceeding [minOverlapArea],
 *         or `null` if no candidate target meets the condition.
 */
fun <T : CardLocation> findBestDropTarget(
    draggedBounds: Rect,
    targets: Map<out T, Rect>,
    minOverlapArea: Float = 0f
): T? {
    if (draggedBounds.isEmpty || targets.isEmpty()) return null

    var bestTarget: T? = null
    var maxOverlap = minOverlapArea

    for ((location, targetBounds) in targets) {
        val overlap = calculateOverlapArea(draggedBounds, targetBounds)
        if (overlap > maxOverlap) {
            maxOverlap = overlap
            bestTarget = location
        }
    }

    return bestTarget
}

/**
 * Compose [Modifier] extension registering the element's root-relative bounds in [registry]
 * under the specified [location].
 *
 * @param location [CardLocation] identifying this drop target.
 * @param registry The active [DropTargetRegistry] instance.
 */
fun Modifier.dropTarget(
    location: CardLocation,
    registry: DropTargetRegistry
): Modifier = this.onGloballyPositioned { coordinates ->
    if (coordinates.isAttached) {
        registry.register(location, coordinates.boundsInRoot())
    }
}

/**
 * Compose [Modifier] extension that automatically registers the element's bounds
 * in [LocalDropTargetRegistry.current] under the specified [location], if the registry is provided.
 *
 * @param location [CardLocation] identifying this drop target.
 */
@Composable
fun Modifier.dropTarget(location: CardLocation): Modifier {
    val registry = LocalDropTargetRegistry.current ?: return this
    return this.dropTarget(location, registry)
}

/**
 * Remembers a newly instantiated [DropTargetRegistry] across recompositions.
 */
@Composable
fun rememberDropTargetRegistry(): DropTargetRegistry = remember { DropTargetRegistry() }
