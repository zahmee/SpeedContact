package com.speedcontact.app.domain

import com.speedcontact.app.model.SortOrder
import java.text.Collator
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class ContactSorterTest {
    private data class Item(val key: String, val name: String)

    private val anna = Item("a", "Anna")
    private val zaid = Item("z", "Zaid")
    private val omar = Item("o", "Omar")
    private val items = listOf(zaid, anna, omar)

    private fun sort(order: SortOrder, lastCalled: Map<String, Long> = emptyMap()) =
        ContactSorter.sort(
            items = items,
            order = order,
            key = Item::key,
            name = Item::name,
            lastCalled = lastCalled,
            collator = Collator.getInstance(Locale.ENGLISH),
        )

    @Test
    fun `custom order leaves the list untouched`() {
        assertEquals(items, sort(SortOrder.CUSTOM))
    }

    @Test
    fun `alphabetical sorts by name`() {
        assertEquals(listOf(anna, omar, zaid), sort(SortOrder.ALPHABETICAL))
    }

    @Test
    fun `recent puts the latest call first and the rest alphabetically`() {
        val result = sort(SortOrder.RECENT, mapOf("o" to 100L))
        assertEquals(listOf(omar, anna, zaid), result)
    }

    @Test
    fun `custom order places known keys first and keeps new ones after`() {
        val result = ContactSorter.applyCustomOrder(items, listOf("o", "z"), Item::key)
        assertEquals(listOf(omar, zaid, anna), result)
    }

    @Test
    fun `empty custom order keeps the input`() {
        assertEquals(items, ContactSorter.applyCustomOrder(items, emptyList(), Item::key))
    }
}
