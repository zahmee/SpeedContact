package com.speedcontact.app.domain

object PhoneNumbers {
    private const val SIGNIFICANT_DIGITS = 9
    private const val MIN_SIGNIFICANT_DIGITS = 7

    /** Whether two numbers reach the same line, ignoring formatting and any country prefix. */
    fun isSameNumber(first: String, second: String): Boolean {
        val a = digitsOnly(first)
        val b = digitsOnly(second)
        if (a == b) return true
        val compared = minOf(a.length, b.length, SIGNIFICANT_DIGITS)
        return compared >= MIN_SIGNIFICANT_DIGITS && a.takeLast(compared) == b.takeLast(compared)
    }

    fun digitsOnly(number: String): String = toAsciiDigits(number).filter { it in '0'..'9' }

    /** Converts Arabic-Indic and Persian digits so the dialer and WhatsApp can read the number. */
    fun toAsciiDigits(number: String): String = buildString(number.length) {
        number.forEach { char ->
            when (char) {
                in '٠'..'٩' -> append('0' + (char - '٠'))
                in '۰'..'۹' -> append('0' + (char - '۰'))
                else -> append(char)
            }
        }
    }

    /** The digits-only international form that WhatsApp links expect. */
    fun whatsAppDigits(number: String): String = digitsOnly(number).removePrefix("00")
}
