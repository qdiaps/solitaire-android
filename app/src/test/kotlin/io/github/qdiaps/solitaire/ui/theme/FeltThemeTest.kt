package io.github.qdiaps.solitaire.ui.theme

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FeltThemeTest {

    @Nested
    @DisplayName("Felt theme enumeration tests")
    inner class FeltThemeEnumTests {

        @Test
        fun `verify exactly 4 felt table themes exist`() {
            assertEquals(4, FeltTheme.entries.size)
            assertTrue(FeltTheme.entries.contains(FeltTheme.CLASSIC_GREEN))
            assertTrue(FeltTheme.entries.contains(FeltTheme.DEEP_NAVY))
            assertTrue(FeltTheme.entries.contains(FeltTheme.DARK_CHARCOAL))
            assertTrue(FeltTheme.entries.contains(FeltTheme.WINE_RED))
        }

        @Test
        fun `verify each theme has unique id and distinct primary colors`() {
            val ids = FeltTheme.entries.map { it.id }.toSet()
            assertEquals(4, ids.size)

            val primaryColors = FeltTheme.entries.map { it.primaryColor }.toSet()
            assertEquals(4, primaryColors.size)
        }

        @Test
        fun `verify fromId resolves valid themes and falls back to default`() {
            assertEquals(FeltTheme.CLASSIC_GREEN, FeltTheme.fromId("classic_green"))
            assertEquals(FeltTheme.DEEP_NAVY, FeltTheme.fromId("deep_navy"))
            assertEquals(FeltTheme.DARK_CHARCOAL, FeltTheme.fromId("dark_charcoal"))
            assertEquals(FeltTheme.WINE_RED, FeltTheme.fromId("wine_red"))

            assertEquals(FeltTheme.DEFAULT, FeltTheme.fromId(null))
            assertEquals(FeltTheme.DEFAULT, FeltTheme.fromId("unknown_id"))
        }
    }

    @Nested
    @DisplayName("Solitaire colors tests")
    inner class SolitaireColorsTests {

        @Test
        fun `verify suit colors are distinct from background`() {
            val colors = SolitaireColors()
            assertNotEquals(colors.cardRed, colors.cardBlack)
            assertNotEquals(colors.cardRed, colors.cardBackground)
            assertNotEquals(colors.cardBlack, colors.cardBackground)
        }

        @Test
        fun `verify table colors mirror active felt theme`() {
            for (theme in FeltTheme.entries) {
                val colors = SolitaireColors(feltTheme = theme)
                assertEquals(theme.primaryColor, colors.tableBackground)
                assertEquals(theme.darkColor, colors.tableDarkEdge)
                assertEquals(theme.surfaceColor, colors.tableSurface)
            }
        }
    }
}
