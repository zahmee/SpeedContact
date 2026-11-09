package com.speedcontact.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactSearchTest {
    private val numbers = listOf("+966 50 123 4567")

    @Test
    fun `an empty search finds everyone`() {
        assertTrue(ContactSearch("  ").matches("أحمد", numbers))
    }

    @Test
    fun `a name is found however its letters are spelled`() {
        assertTrue(ContactSearch("احمد").matches("أحمد علي", numbers))
        assertTrue(ContactSearch("فاطمه").matches("فاطمة", numbers))
        assertTrue(ContactSearch("SARA").matches("Sara Khan", numbers))
    }

    @Test
    fun `a name is found without its accents`() {
        assertTrue(ContactSearch("jose").matches("José", numbers))
        assertTrue(ContactSearch("joao").matches("João Silva", numbers))
        assertTrue(ContactSearch("ibrahim").matches("İbrahim", numbers))
        assertTrue(ContactSearch("isik").matches("Işık", numbers))
    }

    @Test
    fun `a number is found by any part of its digits`() {
        assertTrue(ContactSearch("1234").matches("أحمد", numbers))
        assertFalse(ContactSearch("9999").matches("أحمد", numbers))
    }

    @Test
    fun `letters that match nothing find nobody`() {
        assertFalse(ContactSearch("خالد").matches("أحمد", numbers))
    }
}
