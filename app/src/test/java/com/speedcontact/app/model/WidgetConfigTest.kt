package com.speedcontact.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetConfigTest {
    @Test
    fun `the options menu never repeats what a quick tap does`() {
        val config = WidgetConfig(tapAction = TapAction.CALL)
        assertEquals(
            listOf(
                TapAction.WHATSAPP_CHAT,
                TapAction.WHATSAPP_CALL,
                TapAction.SMS,
                TapAction.CONTACT_CARD,
            ),
            config.menuActions,
        )
    }

    @Test
    fun `calling joins the options menu once a quick tap does something else`() {
        val config = WidgetConfig(tapAction = TapAction.SMS)
        assertTrue(TapAction.CALL in config.menuActions)
        assertFalse(TapAction.SMS in config.menuActions)
    }

    @Test
    fun `cleared options leave the menu`() {
        val config = WidgetConfig(secondaryActions = setOf(TapAction.SMS))
        assertEquals(listOf(TapAction.SMS), config.menuActions)
        assertTrue(WidgetConfig(secondaryActions = emptySet()).menuActions.isEmpty())
    }

    @Test
    fun `a full name for people without a photo only applies while names are hidden`() {
        assertTrue(NameOptions(showName = false, fullNameWithoutPhoto = true).showsFullNameWithoutPhoto)
        assertFalse(NameOptions(showName = true, fullNameWithoutPhoto = true).showsFullNameWithoutPhoto)
        assertFalse(NameOptions(showName = false, fullNameWithoutPhoto = false).showsFullNameWithoutPhoto)
    }

    @Test
    fun `text size changes in steps and stays within its limits`() {
        assertEquals(20, ListOptions(fontSizeSp = 18).withFontStep(1).fontSizeSp)
        assertEquals(16, ListOptions(fontSizeSp = 18).withFontStep(-1).fontSizeSp)
        assertEquals(ListOptions.MAX_FONT_SP, ListOptions(fontSizeSp = ListOptions.MAX_FONT_SP).withFontStep(1).fontSizeSp)
        assertEquals(ListOptions.MIN_FONT_SP, ListOptions(fontSizeSp = ListOptions.MIN_FONT_SP).withFontStep(-1).fontSizeSp)
    }
}
