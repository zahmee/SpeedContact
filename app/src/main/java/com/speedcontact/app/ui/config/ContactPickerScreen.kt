@file:OptIn(ExperimentalMaterial3Api::class)

package com.speedcontact.app.ui.config

import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.speedcontact.app.R
import com.speedcontact.app.data.ContactPhone
import com.speedcontact.app.data.DeviceContact
import com.speedcontact.app.domain.ContactSearch
import com.speedcontact.app.model.SelectedContact
import com.speedcontact.app.ui.asLeftToRight

@Composable
fun ContactPickerScreen(
    contacts: List<DeviceContact>,
    initialSelection: List<SelectedContact>,
    onCancel: () -> Unit,
    onDone: (List<SelectedContact>) -> Unit,
) {
    val selection = remember { mutableStateListOf(*initialSelection.toTypedArray()) }
    var query by rememberSaveable { mutableStateOf("") }
    var choosingNumberFor by remember { mutableStateOf<DeviceContact?>(null) }

    val visibleContacts = remember(contacts, query) { filterContacts(contacts, query) }

    fun select(contact: DeviceContact, number: String) {
        selection.add(SelectedContact(contact.lookupKey, contact.id, number))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.picker_title)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            painterResource(R.drawable.ic_back),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = { onDone(selection.toList()) },
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text(stringResource(R.string.picker_done, selection.size))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding(),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.picker_search)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            if (visibleContacts.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.picker_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(visibleContacts, key = { it.lookupKey }) { contact ->
                        val selected = selection.firstOrNull { it.lookupKey == contact.lookupKey }
                        ContactRow(
                            contact = contact,
                            selectedNumber = selected?.number,
                            onClick = {
                                when {
                                    selected != null -> selection.remove(selected)
                                    contact.phones.size > 1 -> choosingNumberFor = contact
                                    else -> select(contact, contact.defaultNumber)
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    choosingNumberFor?.let { contact ->
        NumberChooserDialog(
            contact = contact,
            onDismiss = { choosingNumberFor = null },
            onChoose = { number ->
                select(contact, number)
                choosingNumberFor = null
            },
        )
    }
}

@Composable
private fun ContactRow(
    contact: DeviceContact,
    selectedNumber: String?,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        ContactPhoto(contact.thumbnailUri)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = contact.displayName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = (selectedNumber ?: contact.defaultNumber).asLeftToRight(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Checkbox(checked = selectedNumber != null, onCheckedChange = null)
    }
}

@Composable
private fun NumberChooserDialog(
    contact: DeviceContact,
    onDismiss: () -> Unit,
    onChoose: (String) -> Unit,
) {
    val resources = LocalContext.current.resources
    var chosen by remember { mutableStateOf(contact.defaultNumber) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.picker_choose_number, contact.displayName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                contact.phones.forEach { phone ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = chosen == phone.number,
                                role = Role.RadioButton,
                                onClick = { chosen = phone.number },
                            )
                            .padding(vertical = 8.dp),
                    ) {
                        RadioButton(selected = chosen == phone.number, onClick = null)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(phone.number.asLeftToRight())
                            Text(
                                text = phoneLabel(resources, phone),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onChoose(chosen) }) {
                Text(stringResource(R.string.picker_confirm_number))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

private fun phoneLabel(resources: android.content.res.Resources, phone: ContactPhone): String =
    Phone.getTypeLabel(resources, phone.type, phone.customLabel).toString()

private fun filterContacts(contacts: List<DeviceContact>, query: String): List<DeviceContact> {
    val search = ContactSearch(query)
    return contacts.filter { contact ->
        search.matches(contact.displayName, contact.phones.map { it.number })
    }
}
