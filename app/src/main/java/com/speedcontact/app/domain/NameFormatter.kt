package com.speedcontact.app.domain

import com.speedcontact.app.model.NameOptions

data class NameParts(
    val display: String,
    val given: String? = null,
    val family: String? = null,
    val nickname: String? = null,
)

object NameFormatter {
    fun format(parts: NameParts, options: NameOptions): String {
        val nickname = parts.nickname?.trim()?.takeIf { it.isNotEmpty() }
        if (options.preferNickname && nickname != null) return nickname

        val given = parts.given?.trim()?.takeIf { it.isNotEmpty() }
        val family = parts.family?.trim()?.takeIf { it.isNotEmpty() }
        val display = parts.display.trim()

        return when {
            !options.fullName -> given ?: display.substringBefore(' ')
            options.lastNameFirst && given != null && family != null -> "$family $given"
            else -> display
        }
    }
}
