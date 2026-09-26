package io.github.qdiaps.solitaire.ui.theme

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CardBackStyleTest {

    @Nested
    @DisplayName("Card back style enumeration tests")
    inner class CardBackStyleEnumTests {

        @Test
        fun `verify exactly 4 card back styles exist`() {
            assertEquals(4, CardBackStyle.entries.size)
            assertTrue(CardBackStyle.entries.contains(CardBackStyle.CLASSIC_LATTICE))
            assertTrue(CardBackStyle.entries.contains(CardBackStyle.CRIMSON_VINTAGE))
            assertTrue(CardBackStyle.entries.contains(CardBackStyle.EMERALD_ART_DECO))
            assertTrue(CardBackStyle.entries.contains(CardBackStyle.OBSIDIAN_MINIMAL))
        }

        @Test
        fun `verify each card back style has unique id and non-blank displayName`() {
            val ids = CardBackStyle.entries.map { it.id }.toSet()
            assertEquals(4, ids.size)

            val names = CardBackStyle.entries.map { it.displayName }.toSet()
            assertEquals(4, names.size)
            CardBackStyle.entries.forEach { style ->
                assertTrue(style.displayName.isNotBlank())
            }
        }

        @Test
        fun `verify each style has distinct primary colors`() {
            val primaryColors = CardBackStyle.entries.map { it.primaryColor }.toSet()
            assertEquals(4, primaryColors.size)

            CardBackStyle.entries.forEach { style ->
                assertNotEquals(style.primaryColor, style.patternColor)
            }
        }

        @Test
        fun `verify fromId resolves valid styles and falls back to default`() {
            assertEquals(CardBackStyle.CLASSIC_LATTICE, CardBackStyle.fromId("classic_lattice"))
            assertEquals(CardBackStyle.CRIMSON_VINTAGE, CardBackStyle.fromId("crimson_vintage"))
            assertEquals(CardBackStyle.EMERALD_ART_DECO, CardBackStyle.fromId("emerald_art_deco"))
            assertEquals(CardBackStyle.OBSIDIAN_MINIMAL, CardBackStyle.fromId("obsidian_minimal"))

            assertEquals(CardBackStyle.DEFAULT, CardBackStyle.fromId(null))
            assertEquals(CardBackStyle.DEFAULT, CardBackStyle.fromId("unknown_style_id"))
        }

        @Test
        fun `verify default style is Classic Lattice`() {
            assertEquals(CardBackStyle.CLASSIC_LATTICE, CardBackStyle.DEFAULT)
        }
    }
}
