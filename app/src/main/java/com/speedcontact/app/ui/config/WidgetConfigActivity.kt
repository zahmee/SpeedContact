package com.speedcontact.app.ui.config

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.speedcontact.app.R
import com.speedcontact.app.model.TapAction
import com.speedcontact.app.ui.SpeedContactTheme
import kotlinx.coroutines.launch

class WidgetConfigActivity : ComponentActivity() {
    private val viewModel: WidgetConfigViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Leaving without saving must cancel the launcher's placement of a new widget.
        setResult(RESULT_CANCELED, resultIntent())
        if (viewModel.appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            SpeedContactTheme {
                ConfigRoute()
            }
        }
    }

    @Composable
    private fun ConfigRoute() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val preview by viewModel.preview.collectAsStateWithLifecycle()
        val contacts by viewModel.contacts.collectAsStateWithLifecycle()
        var showPicker by rememberSaveable { mutableStateOf(false) }
        var permissionAsked by rememberSaveable { mutableStateOf(false) }
        var showContactsDisclosure by rememberSaveable {
            mutableStateOf(!isGranted(Manifest.permission.READ_CONTACTS))
        }
        var showCallDisclosure by rememberSaveable { mutableStateOf(false) }
        val listState = rememberLazyListState()

        val contactsPermission = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) {
            permissionAsked = true
            viewModel.onPermissionResult()
        }
        val callPermission = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) {
            saveAndFinish()
        }
        // Needs no storage permission: Android hands over only the one picture that was chosen.
        val iconPicker = rememberLauncherForActivityResult(
            ActivityResultContracts.PickVisualMedia(),
        ) { uri ->
            if (uri != null) {
                lifecycleScope.launch {
                    if (!viewModel.setCustomIcon(uri)) showIconFailure()
                }
            }
        }

        if (showContactsDisclosure) {
            PermissionDisclosureDialog(
                title = R.string.permission_contacts_title,
                body = R.string.permission_contacts_body,
                onContinue = {
                    showContactsDisclosure = false
                    contactsPermission.launch(Manifest.permission.READ_CONTACTS)
                },
                onNotNow = { showContactsDisclosure = false },
            )
        }
        if (showCallDisclosure) {
            PermissionDisclosureDialog(
                title = R.string.permission_call_title,
                body = R.string.permission_call_body,
                onContinue = {
                    showCallDisclosure = false
                    callPermission.launch(Manifest.permission.CALL_PHONE)
                },
                onNotNow = {
                    showCallDisclosure = false
                    saveAndFinish()
                },
                // Tapping beside the dialog only closes it; saving takes a clear choice.
                onDismiss = { showCallDisclosure = false },
            )
        }

        if (showPicker) {
            BackHandler { showPicker = false }
            ContactPickerScreen(
                contacts = contacts,
                initialSelection = state.config.contacts,
                onCancel = { showPicker = false },
                onDone = { selected ->
                    viewModel.setManualContacts(selected)
                    showPicker = false
                },
            )
        } else {
            WidgetConfigScreen(
                kind = viewModel.kind,
                state = state,
                preview = preview,
                listState = listState,
                // Once Android stops showing the prompt, only the settings page can grant access.
                canAskForPermission = !permissionAsked ||
                    shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS),
                onRequestPermission = { contactsPermission.launch(Manifest.permission.READ_CONTACTS) },
                onOpenAppSettings = ::openAppSettings,
                onUpdate = viewModel::update,
                onAddPeople = { showPicker = true },
                onRemovePerson = viewModel::remove,
                onMovePerson = viewModel::move,
                onPickFolderIcon = {
                    try {
                        iconPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    } catch (_: ActivityNotFoundException) {
                        showIconFailure()
                    }
                },
                loadCustomIcon = viewModel::loadCustomIcon,
                onClose = ::finish,
                onSave = {
                    val needsCallPermission = state.config.tapAction == TapAction.CALL &&
                        !isGranted(Manifest.permission.CALL_PHONE)
                    if (needsCallPermission) {
                        showCallDisclosure = true
                    } else {
                        saveAndFinish()
                    }
                },
            )
        }
    }

    private fun showIconFailure() {
        Toast.makeText(this, R.string.folder_icon_failed, Toast.LENGTH_LONG).show()
    }

    private fun saveAndFinish() {
        lifecycleScope.launch {
            viewModel.save()
            setResult(RESULT_OK, resultIntent())
            finish()
        }
    }

    private fun resultIntent(): Intent =
        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, viewModel.appWidgetId)

    private fun isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun openAppSettings() {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null),
            ),
        )
    }

    override fun onResume() {
        super.onResume()
        // The user may have just granted the permission from the settings page.
        viewModel.onPermissionResult()
    }
}
