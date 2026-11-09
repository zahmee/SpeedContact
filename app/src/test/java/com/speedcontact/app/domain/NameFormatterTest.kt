package com.speedcontact.app.domain

import com.speedcontact.app.model.NameOptions
import org.junit.Assert.assertEquals
import org.junit.Test

class NameFormatterTest {
    private val full = NameParts("محمد علي", given = "محمد", family = "علي", nickname = "أبو علي")

    @Test
    fun `shows the full display name by default`() {
        assertEquals("محمد علي", NameFormatter.format(full, NameOptions()))
    }

    @Test
    fun `prefers the nickname when asked and available`() {
        assertEquals("أبو علي", NameFormatter.format(full, NameOptions(preferNickname = true)))
    }

    @Test
    fun `falls back when a nickname is requested but blank`() {
        val parts = full.copy(nickname = "  ")
        assertEquals("محمد علي", NameFormatter.format(parts, NameOptions(preferNickname = true)))
    }

    @Test
    fun `short form uses the given name`() {
        assertEquals("محمد", NameFormatter.format(full, NameOptions(fullName = false)))
    }

    @Test
    fun `short form takes the first word when there is no structured name`() {
        val parts = NameParts("Layla Omar")
        assertEquals("Layla", NameFormatter.format(parts, NameOptions(fullName = false)))
    }

    @Test
    fun `puts the family name first when asked`() {
        assertEquals("علي محمد", NameFormatter.format(full, NameOptions(lastNameFirst = true)))
    }

    @Test
    fun `last name first needs both parts`() {
        val parts = NameParts("Madonna", given = "Madonna")
        assertEquals("Madonna", NameFormatter.format(parts, NameOptions(lastNameFirst = true)))
    }
}
