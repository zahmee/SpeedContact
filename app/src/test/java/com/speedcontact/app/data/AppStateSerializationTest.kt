package com.speedcontact.app.data

import com.speedcontact.app.model.AppState
import com.speedcontact.app.model.ContactSource
import com.speedcontact.app.model.FolderIcon
import com.speedcontact.app.model.ListOptions
import com.speedcontact.app.model.ListSpacing
import com.speedcontact.app.model.SelectedContact
import com.speedcontact.app.model.TapAction
import com.speedcontact.app.model.WidgetConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class AppStateSerializationTest {
    @Test
    fun `state survives a round trip`() {
        val state = AppState(
            widgets = mapOf(
                7 to WidgetConfig(
                    title = "العائلة",
                    source = ContactSource.MANUAL,
                    contacts = listOf(SelectedContact("key", 12L, "+966501234567")),
                    tapAction = TapAction.WHATSAPP_CHAT,
                ),
            ),
            lastCalled = mapOf("key" to 1000L),
        )
        val json = AppJson.encodeToString(AppState.serializer(), state)
        assertEquals(state, AppJson.decodeFromString(AppState.serializer(), json))
    }

    @Test
    fun `widgets saved by an older version gain the newer options with their defaults`() {
        val json = """{"widgets":{"7":{"title":"x"}},"lastCalled":{}}"""
        val config = AppJson.decodeFromString(AppState.serializer(), json).widgets.getValue(7)
        assertEquals(ListOptions(), config.list)
        assertEquals(false, config.name.fullNameWithoutPhoto)
        assertEquals(TapAction.entries.toSet(), config.secondaryActions)
        assertEquals(true, config.showPageBar)
        assertEquals(FolderIcon.APP, config.folderIcon)
        assertEquals(null, config.customIconFile)
    }

    @Test
    fun `a number already shown by an older version stays below the name`() {
        val json = """{"widgets":{"7":{"list":{"showNumber":true}}},"lastCalled":{}}"""
        val options = AppJson.decodeFromString(AppState.serializer(), json).widgets.getValue(7).list
        assertEquals(true, options.showsNumberBelow)
        assertEquals(false, options.showsNumberBeside)
    }

    @Test
    fun `list and options menu settings survive a round trip`() {
        val state = AppState(
            widgets = mapOf(
                7 to WidgetConfig(
                    list = ListOptions(fontSizeSp = 26, boldName = true, spacing = ListSpacing.WIDE),
                    secondaryActions = setOf(TapAction.SMS),
                ),
            ),
        )
        val json = AppJson.encodeToString(AppState.serializer(), state)
        assertEquals(state, AppJson.decodeFromString(AppState.serializer(), json))
    }

    @Test
    fun `settings from a newer version do not break reading`() {
        val json = """{"widgets":{"7":{"title":"x","futureSetting":true}},"lastCalled":{}}"""
        val state = AppJson.decodeFromString(AppState.serializer(), json)
        assertEquals("x", state.widgets.getValue(7).title)
    }

    @Test
    fun `an unknown enum value falls back to the default`() {
        val json = """{"widgets":{"7":{"tapAction":"TELEPATHY"}},"lastCalled":{}}"""
        val state = AppJson.decodeFromString(AppState.serializer(), json)
        assertEquals(TapAction.CALL, state.widgets.getValue(7).tapAction)
    }
}
