package com.speedcontact.app.domain

import java.text.Normalizer

object TextNormalizer {
    // Accents once split from their Latin letters, Arabic diacritics, Quranic marks and the kashida.
    private val IGNORED = Regex("[\\u0300-\\u036F\\u0610-\\u061A\\u0640\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]")

    /** Folds case, accents and common Arabic letter variants so searching finds names however spelled. */
    fun normalize(text: String): String {
        val split = Normalizer.normalize(text.trim().lowercase(), Normalizer.Form.NFD)
        val folded = IGNORED.replace(split, "")
        return buildString(folded.length) {
            folded.forEach { char ->
                append(
                    when (char) {
                        'أ', 'إ', 'آ', 'ٱ' -> 'ا'
                        'ى', 'ئ' -> 'ي'
                        'ؤ' -> 'و'
                        'ة' -> 'ه'
                        'ı' -> 'i'
                        else -> char
                    },
                )
            }
        }
    }
}
