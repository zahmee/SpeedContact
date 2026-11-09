package com.speedcontact.app.domain

/** Finds a name however it is spelled, or a number by any part of its digits. */
class ContactSearch(query: String) {
    private val text = TextNormalizer.normalize(query)
    private val digits = PhoneNumbers.digitsOnly(query)

    fun matches(name: String, numbers: List<String>): Boolean {
        if (text.isEmpty()) return true
        return TextNormalizer.normalize(name).contains(text) ||
            (digits.isNotEmpty() && numbers.any { PhoneNumbers.digitsOnly(it).contains(digits) })
    }
}
