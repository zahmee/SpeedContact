package com.speedcontact.app.ui.home

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.speedcontact.app.R
import java.util.Locale

/** Each language is written in itself, so people can find their own whatever the app shows now. */
enum class AppLanguage(val tag: String, val nativeName: String) {
    ENGLISH("en", "English"),
    ARABIC("ar", "العربية"),
    URDU("ur", "اردو"),
    HINDI("hi", "हिन्दी"),
    INDONESIAN("id", "Bahasa Indonesia"),
    TURKISH("tr", "Türkçe"),
    SPANISH("es", "Español"),
    PORTUGUESE("pt", "Português"),
}

/** Null means following the phone. Compared by language, as system settings may add a region. */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun Context.currentAppLanguage(): AppLanguage? {
    val chosen = getSystemService(LocaleManager::class.java).applicationLocales
    if (chosen.isEmpty) return null
    return AppLanguage.entries.firstOrNull { Locale.forLanguageTag(it.tag).language == chosen[0].language }
}

/**
 * Android reopens the screen in the new language and tells [com.speedcontact.app.widget.LocaleChangedReceiver].
 * Android 12 and lower do not get this choice: their widgets would stay in the phone's language.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun Context.setAppLanguage(language: AppLanguage?) {
    getSystemService(LocaleManager::class.java).setApplicationLocales(
        if (language == null) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(language.tag),
    )
}

@Composable
fun LanguageDialog(
    selected: AppLanguage?,
    onSelect: (AppLanguage?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .selectableGroup()
                    .verticalScroll(rememberScrollState()),
            ) {
                LanguageRow(stringResource(R.string.language_system), selected == null) { onSelect(null) }
                AppLanguage.entries.forEach { language ->
                    LanguageRow(language.nativeName, selected == language) { onSelect(language) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun LanguageRow(name: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(name, Modifier.padding(start = 16.dp))
    }
}
