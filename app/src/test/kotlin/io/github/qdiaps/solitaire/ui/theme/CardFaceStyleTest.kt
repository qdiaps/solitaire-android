package io.github.qdiaps.solitaire.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CardFaceStyleTest {

    @Nested
    @DisplayName("Card face style enumeration tests")
    inner class CardFaceStyleEnumTests {

        @Test
        fun `verify canonical Modern Clean face style exists`() {
            assertEquals(1, CardFaceStyle.entries.size)
            assertTrue(CardFaceStyle.entries.contains(CardFaceStyle.MODERN_CLEAN))
        }

        @Test
        fun `verify Modern Clean has valid id and displayName`() {
            assertEquals("modern_clean", CardFaceStyle.MODERN_CLEAN.id)
            assertEquals("Modern Clean", CardFaceStyle.MODERN_CLEAN.displayName)
            assertTrue(CardFaceStyle.MODERN_CLEAN.displayName.isNotBlank())
        }

        @Test
        fun `verify font family and weight assignments`() {
            assertEquals(FontFamily.SansSerif, CardFaceStyle.MODERN_CLEAN.fontFamily)
            assertEquals(FontWeight.Bold, CardFaceStyle.MODERN_CLEAN.fontWeight)
        }

        @Test
        fun `verify canonical scale factors`() {
            assertEquals(1.0f, CardFaceStyle.MODERN_CLEAN.rankTextScale)
            assertEquals(1.0f, CardFaceStyle.MODERN_CLEAN.cornerEmblemScale)
            assertEquals(1.0f, CardFaceStyle.MODERN_CLEAN.centerEmblemScale)
        }

        @Test
        fun `verify fromId resolves valid styles and falls back to default`() {
            assertEquals(CardFaceStyle.MODERN_CLEAN, CardFaceStyle.fromId("modern_clean"))
            assertEquals(CardFaceStyle.DEFAULT, CardFaceStyle.fromId(null))
            assertEquals(CardFaceStyle.DEFAULT, CardFaceStyle.fromId("unknown_face_style"))
            assertEquals(CardFaceStyle.MODERN_CLEAN, CardFaceStyle.DEFAULT)
        }
    }

    @Nested
    @DisplayName("Card typography generation tests")
    inner class CardTypographyGenerationTests {

        @Test
        fun `verify createCardTypography produces matching styles for Modern Clean`() {
            val typography = createCardTypography(CardFaceStyle.MODERN_CLEAN)
            assertNotNull(typography)
            assertEquals(FontFamily.SansSerif, typography.cardRank.fontFamily)
            assertEquals(FontWeight.Bold, typography.cardRank.fontWeight)
            assertEquals(14.sp, typography.cardRank.fontSize)
        }
    }
}
