package com.speedcontact.app.ui.config

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.speedcontact.app.R

/**
 * Says what a permission is for before Android's own prompt appears. Google Play requires this
 * for contacts because widgets read them in the background, and only a press of "continue" counts.
 */
@Composable
fun PermissionDisclosureDialog(
    title: Int,
    body: Int,
    onContinue: () -> Unit,
    onNotNow: () -> Unit,
    onDismiss: () -> Unit = onNotNow,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = { Text(stringResource(body)) },
        confirmButton = {
            TextButton(onClick = onContinue) {
                Text(stringResource(R.string.permission_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = onNotNow) {
                Text(stringResource(R.string.permission_not_now))
            }
        },
    )
}
