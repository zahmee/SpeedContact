package com.speedcontact.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumbersTest {
    @Test
    fun `the same number with and without a country code matches`() {
        assertTrue(PhoneNumbers.isSameNumber("0501234567", "+966 50 123 4567"))
    }

    @Test
    fun `the same number in Arabic digits matches`() {
        assertTrue(PhoneNumbers.isSameNumber("٠٥٠١٢٣٤٥٦٧", "050-123-4567"))
    }

    @Test
    fun `different numbers do not match`() {
        assertFalse(PhoneNumbers.isSameNumber("0501234567", "0501234568"))
    }

    @Test
    fun `short numbers must match exactly`() {
        assertFalse(PhoneNumbers.isSameNumber("9001", "19001"))
        assertTrue(PhoneNumbers.isSameNumber("9001", "9001"))
    }

    @Test
    fun `converts Arabic-Indic digits`() {
        assertEquals("0501234567", PhoneNumbers.toAsciiDigits("٠٥٠١٢٣٤٥٦٧"))
    }

    @Test
    fun `converts Persian digits`() {
        assertEquals("0501234567", PhoneNumbers.toAsciiDigits("۰۵۰۱۲۳۴۵۶۷"))
    }

    @Test
    fun `keeps dialing symbols`() {
        assertEquals("*100#", PhoneNumbers.toAsciiDigits("*100#"))
        assertEquals("+966 50 123", PhoneNumbers.toAsciiDigits("+966 50 123"))
    }

    @Test
    fun `WhatsApp digits drop formatting and the plus sign`() {
        assertEquals("966501234567", PhoneNumbers.whatsAppDigits("+966 50-123-4567"))
    }

    @Test
    fun `WhatsApp digits drop an international 00 prefix`() {
        assertEquals("966501234567", PhoneNumbers.whatsAppDigits("00966501234567"))
    }

    @Test
    fun `WhatsApp digits convert Arabic digits`() {
        assertEquals("966501234567", PhoneNumbers.whatsAppDigits("+٩٦٦٥٠١٢٣٤٥٦٧"))
    }
}
