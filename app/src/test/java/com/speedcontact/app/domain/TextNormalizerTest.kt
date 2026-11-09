package com.speedcontact.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TextNormalizerTest {
    @Test
    fun `treats alef forms alike`() {
        assertEquals(TextNormalizer.normalize("احمد"), TextNormalizer.normalize("أحمد"))
        assertEquals(TextNormalizer.normalize("اسراء"), TextNormalizer.normalize("إسراء"))
    }

    @Test
    fun `treats ta marbuta and ha alike`() {
        assertEquals(TextNormalizer.normalize("فاطمه"), TextNormalizer.normalize("فاطمة"))
    }

    @Test
    fun `treats alef maqsura and ya alike`() {
        assertEquals(TextNormalizer.normalize("مصطفي"), TextNormalizer.normalize("مصطفى"))
    }

    @Test
    fun `ignores diacritics and kashida`() {
        assertEquals(TextNormalizer.normalize("محمد"), TextNormalizer.normalize("مُحَمَّد"))
        assertEquals(TextNormalizer.normalize("محمد"), TextNormalizer.normalize("محـــمد"))
    }

    @Test
    fun `ignores case and surrounding space`() {
        assertEquals("layla", TextNormalizer.normalize("  LAYLA "))
    }
}
