@file:OptIn(ExperimentalMaterial3Api::class)

package com.speedcontact.app.ui.home

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.speedcontact.app.R
import com.speedcontact.app.ui.AppIcon
import com.speedcontact.app.ui.asLeftToRight

private const val DEVELOPER_PHONE = "+966502010911"
private const val DEVELOPER_WHATSAPP_URL = "https://wa.me/966502010911"
private const val DEVELOPER_EMAIL = "info@cdit.co"

/** The links open in other apps; this app itself has no internet access and sends nothing. */
@Composable
fun AboutScreen(
    versionName: String,
    onBack: () -> Unit,
    onOpen: (Intent) -> Unit,
) {
    var showPrivacy by rememberSaveable { mutableStateOf(false) }
    if (showPrivacy) PrivacyPolicyDialog(onDismiss = { showPrivacy = false })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painterResource(R.drawable.ic_back),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            ) {
                AppIcon(Modifier.size(88.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    text = stringResource(R.string.about_version, versionName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider()
            Text(
                text = stringResource(R.string.about_developer),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
            )
            Text(
                text = stringResource(R.string.developer_name),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Text(
                text = stringResource(R.string.developer_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            )

            val websiteUrl = stringResource(R.string.developer_website_url)
            AboutRow(
                label = R.string.about_website,
                value = stringResource(R.string.developer_website_label),
                onClick = { onOpen(Intent(Intent.ACTION_VIEW, websiteUrl.toUri())) },
            )
            AboutRow(
                label = R.string.about_phone,
                value = DEVELOPER_PHONE.asLeftToRight(),
                onClick = { onOpen(Intent(Intent.ACTION_DIAL, "tel:$DEVELOPER_PHONE".toUri())) },
            )
            AboutRow(
                label = R.string.about_whatsapp,
                value = DEVELOPER_PHONE.asLeftToRight(),
                onClick = { onOpen(Intent(Intent.ACTION_VIEW, DEVELOPER_WHATSAPP_URL.toUri())) },
            )
            AboutRow(
                label = R.string.about_email,
                value = DEVELOPER_EMAIL.asLeftToRight(),
                onClick = { onOpen(Intent(Intent.ACTION_SENDTO, "mailto:$DEVELOPER_EMAIL".toUri())) },
            )
            AboutRow(
                label = R.string.about_location,
                value = stringResource(R.string.developer_location),
            )

            HorizontalDivider(Modifier.padding(top = 8.dp))
            ListItem(
                headlineContent = { Text(stringResource(R.string.about_privacy)) },
                modifier = Modifier.clickable(role = Role.Button) { showPrivacy = true },
            )
        }
    }
}

/** Google Play asks for the policy inside the app as well as at a public address. */
@Composable
private fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.about_privacy)) },
        text = {
            Text(
                text = stringResource(R.string.privacy_policy_text),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )
}

@Composable
private fun AboutRow(label: Int, value: String, onClick: (() -> Unit)? = null) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable(role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }
    ListItem(
        headlineContent = { Text(value) },
        overlineContent = { Text(stringResource(label)) },
        modifier = clickModifier,
    )
}
