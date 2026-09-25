package io.github.qdiaps.solitaire.ui.game.gesture

import androidx.compose.ui.geometry.Rect
import io.github.qdiaps.solitaire.domain.model.CardLocation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DropTargetRegistryTest {

    @Nested
    @DisplayName("DropTargetRegistry state management")
    inner class StateManagementTests {

        @Test
        fun `registry starts with empty targets`() {
            val registry = DropTargetRegistry()
            assertTrue(registry.activeTargets.isEmpty())
            assertNull(registry.getBounds(CardLocation.Foundation(0)))
        }

        @Test
        fun `registering target stores bounds`() {
            val registry = DropTargetRegistry()
            val foundationTarget = CardLocation.Foundation(0)
            val bounds = Rect(10f, 20f, 110f, 160f)

            registry.register(foundationTarget, bounds)

            assertEquals(bounds, registry.getBounds(foundationTarget))
            assertEquals(1, registry.activeTargets.size)
            assertEquals(bounds, registry.activeTargets[foundationTarget])
        }

        @Test
        fun `re-registering updates existing target bounds`() {
            val registry = DropTargetRegistry()
            val tableauTarget = CardLocation.Tableau(columnIndex = 2)
            val initialBounds = Rect(50f, 100f, 150f, 240f)
            val updatedBounds = Rect(50f, 100f, 150f, 300f)

            registry.register(tableauTarget, initialBounds)
            assertEquals(initialBounds, registry.getBounds(tableauTarget))

            registry.register(tableauTarget, updatedBounds)
            assertEquals(updatedBounds, registry.getBounds(tableauTarget))
            assertEquals(1, registry.activeTargets.size)
        }

        @Test
        fun `unregistering removes target from registry`() {
            val registry = DropTargetRegistry()
            val target = CardLocation.Foundation(1)
            registry.register(target, Rect(10f, 10f, 100f, 150f))

            registry.unregister(target)

            assertNull(registry.getBounds(target))
            assertTrue(registry.activeTargets.isEmpty())
        }

        @Test
        fun `clear removes all registered targets`() {
            val registry = DropTargetRegistry()
            registry.register(CardLocation.Foundation(0), Rect(0f, 0f, 100f, 140f))
            registry.register(CardLocation.Tableau(0), Rect(0f, 200f, 100f, 500f))

            assertEquals(2, registry.activeTargets.size)

            registry.clear()

            assertTrue(registry.activeTargets.isEmpty())
            assertNull(registry.getBounds(CardLocation.Foundation(0)))
            assertNull(registry.getBounds(CardLocation.Tableau(0)))
        }
    }

    @Nested
    @DisplayName("calculateOverlapArea calculations")
    inner class OverlapAreaTests {

        @Test
        fun `returns exact area when rectangles are identical`() {
            val rect = Rect(10f, 20f, 110f, 160f) // 100 x 140 = 14000
            val area = calculateOverlapArea(rect, rect)
            assertEquals(14000f, area, 0.001f)
        }

        @Test
        fun `returns inner area when one rectangle is fully contained`() {
            val outer = Rect(0f, 0f, 200f, 200f)
            val inner = Rect(20f, 30f, 70f, 80f) // 50 x 50 = 2500

            val area = calculateOverlapArea(outer, inner)
            assertEquals(2500f, area, 0.001f)
        }

        @Test
        fun `calculates partial intersection area correctly`() {
            val rectA = Rect(0f, 0f, 100f, 100f)
            val rectB = Rect(60f, 50f, 160f, 150f)
            // Overlap: x in [60, 100] (width 40), y in [50, 100] (height 50) -> 40 * 50 = 2000
            val area = calculateOverlapArea(rectA, rectB)
            assertEquals(2000f, area, 0.001f)
        }

        @Test
        fun `returns zero when rectangles do not overlap`() {
            val rectA = Rect(0f, 0f, 50f, 50f)
            val rectB = Rect(100f, 100f, 150f, 150f)

            assertEquals(0f, calculateOverlapArea(rectA, rectB), 0.001f)
        }

        @Test
        fun `returns zero when rectangles only touch edges`() {
            val rectA = Rect(0f, 0f, 50f, 50f)
            val rectB = Rect(50f, 0f, 100f, 50f) // Touching along x=50

            assertEquals(0f, calculateOverlapArea(rectA, rectB), 0.001f)
        }

        @Test
        fun `returns zero when one or both rectangles are degenerate or empty`() {
            val valid = Rect(0f, 0f, 100f, 100f)
            val zeroWidth = Rect(50f, 0f, 50f, 100f)
            val inverted = Rect(100f, 100f, 0f, 0f)

            assertEquals(0f, calculateOverlapArea(valid, zeroWidth), 0.001f)
            assertEquals(0f, calculateOverlapArea(valid, inverted), 0.001f)
        }
    }

    @Nested
    @DisplayName("calculateOverlapRatio calculations")
    inner class OverlapRatioTests {

        @Test
        fun `returns 1_0 when dragged rectangle is completely inside target`() {
            val dragged = Rect(20f, 20f, 60f, 70f) // 40 x 50
            val target = Rect(0f, 0f, 200f, 200f)

            val ratio = calculateOverlapRatio(dragged, target)
            assertEquals(1.0f, ratio, 0.001f)
        }

        @Test
        fun `returns 0_5 when half of dragged rectangle overlaps target`() {
            val dragged = Rect(0f, 0f, 100f, 100f) // area 10000
            val target = Rect(50f, 0f, 150f, 100f) // overlap 50 x 100 = 5000

            val ratio = calculateOverlapRatio(dragged, target)
            assertEquals(0.5f, ratio, 0.001f)
        }

        @Test
        fun `returns 0_0 when disjoint or empty`() {
            val dragged = Rect(0f, 0f, 100f, 100f)
            val target = Rect(200f, 200f, 300f, 300f)

            assertEquals(0.0f, calculateOverlapRatio(dragged, target), 0.001f)
            assertEquals(0.0f, calculateOverlapRatio(Rect.Zero, target), 0.001f)
        }
    }

    @Nested
    @DisplayName("findBestDropTarget selection")
    inner class BestTargetSelectionTests {

        private val foundation0 = CardLocation.Foundation(0)
        private val foundation1 = CardLocation.Foundation(1)
        private val tableau0 = CardLocation.Tableau(0)
        private val tableau1 = CardLocation.Tableau(1)

        @Test
        fun `returns null when targets map is empty`() {
            val dragged = Rect(10f, 10f, 110f, 150f)
            assertNull(findBestDropTarget(dragged, emptyMap<CardLocation, Rect>()))
        }

        @Test
        fun `returns null when dragged bounds is empty or zero`() {
            val targets = mapOf(foundation0 to Rect(0f, 0f, 100f, 140f))
            assertNull(findBestDropTarget(Rect.Zero, targets))
        }

        @Test
        fun `returns null when dragged card does not overlap any registered target`() {
            val targets = mapOf(
                foundation0 to Rect(0f, 0f, 100f, 140f),
                tableau0 to Rect(0f, 200f, 100f, 400f)
            )
            val dragged = Rect(300f, 300f, 400f, 440f)

            assertNull(findBestDropTarget(dragged, targets))
        }

        @Test
        fun `returns target when only one target overlaps`() {
            val targets = mapOf(
                foundation0 to Rect(0f, 0f, 100f, 140f),
                foundation1 to Rect(120f, 0f, 220f, 140f)
            )
            // Overlaps only foundation0
            val dragged = Rect(20f, 20f, 100f, 140f)

            val best = findBestDropTarget(dragged, targets)
            assertEquals(foundation0, best)
        }

        @Test
        fun `selects target with largest overlap area when multiple targets intersect`() {
            // Two adjacent tableau columns:
            // Column 0: x in [0..100], y in [200..500]
            // Column 1: x in [120..220], y in [200..500]
            val targets = mapOf(
                tableau0 to Rect(0f, 200f, 100f, 500f),
                tableau1 to Rect(120f, 200f, 220f, 500f)
            )

            // Dragged card positioned at x in [80..180], y in [220..360] (width 100, height 140)
            // Overlap with tableau0: x in [80..100] (20) * y in [220..360] (140) = 2800
            // Overlap with tableau1: x in [120..180] (60) * y in [220..360] (140) = 8400
            val dragged = Rect(80f, 220f, 180f, 360f)

            val best = findBestDropTarget(dragged, targets)
            assertEquals(tableau1, best)
        }

        @Test
        fun `respects minOverlapArea threshold`() {
            val targets = mapOf(
                foundation0 to Rect(0f, 0f, 100f, 140f)
            )
            // Overlap is 10 x 10 = 100
            val dragged = Rect(90f, 130f, 190f, 270f)

            // Overlap 100 exceeds threshold 50 -> matches
            val match = findBestDropTarget(dragged, targets, minOverlapArea = 50f)
            assertEquals(foundation0, match)

            // Overlap 100 does not exceed threshold 200 -> null
            val noMatch = findBestDropTarget(dragged, targets, minOverlapArea = 200f)
            assertNull(noMatch)
        }

        @Test
        fun `registry findBestTarget delegates properly`() {
            val registry = DropTargetRegistry()
            registry.register(foundation0, Rect(0f, 0f, 100f, 140f))
            registry.register(foundation1, Rect(120f, 0f, 220f, 140f))

            val dragged = Rect(10f, 10f, 90f, 130f)
            val result = registry.findBestTarget(dragged)

            assertEquals(foundation0, result)
        }
    }
}
