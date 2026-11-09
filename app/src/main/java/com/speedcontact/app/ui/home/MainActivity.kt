package com.speedcontact.app.ui.home

import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.speedcontact.app.R
import com.speedcontact.app.ui.SpeedContactTheme
import com.speedcontact.app.ui.config.WidgetConfigActivity
import com.speedcontact.app.widget.WidgetKind
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: HomeViewModel by viewModels()

    /** Set while a "place on home screen" prompt is up, to open setup for the new widget after. */
    private var awaitingPinnedWidget = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        awaitingPinnedWidget = savedInstanceState?.getBoolean(KEY_AWAITING_PIN) == true

        setContent {
            SpeedContactTheme {
                val items by viewModel.items.collectAsStateWithLifecycle()
                var showPinHelp by rememberSaveable { mutableStateOf(false) }
                var showAbout by rememberSaveable { mutableStateOf(false) }
                var showLanguage by rememberSaveable { mutableStateOf(false) }

                if (showAbout) {
                    BackHandler { showAbout = false }
                    AboutScreen(
                        versionName = versionName(),
                        onBack = { showAbout = false },
                        onOpen = ::openExternally,
                    )
                } else {
                    HomeScreen(
                        items = items,
                        onAddWidget = { kind ->
                            if (!requestPinWidget(kind)) showPinHelp = true
                        },
                        onOpenWidget = ::openWidgetSettings,
                        onOpenAbout = { showAbout = true },
                        onOpenLanguage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            { showLanguage = true }
                        } else {
                            null
                        },
                    )
                }

                if (showLanguage && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    LanguageDialog(
                        selected = currentAppLanguage(),
                        onSelect = { language ->
                            showLanguage = false
                            setAppLanguage(language)
                        },
                        onDismiss = { showLanguage = false },
                    )
                }

                if (showPinHelp) {
                    AlertDialog(
                        onDismissRequest = { showPinHelp = false },
                        title = { Text(stringResource(R.string.pin_help_title)) },
                        text = { Text(stringResource(R.string.pin_help_body)) },
                        confirmButton = {
                            TextButton(onClick = { showPinHelp = false }) {
                                Text(stringResource(R.string.ok))
                            }
                        },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val items = viewModel.refresh()
            if (awaitingPinnedWidget) {
                items.firstOrNull { it.config == null }?.let { newWidget ->
                    awaitingPinnedWidget = false
                    openWidgetSettings(newWidget.appWidgetId)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_AWAITING_PIN, awaitingPinnedWidget)
    }

    private fun requestPinWidget(kind: WidgetKind): Boolean {
        val manager = AppWidgetManager.getInstance(this)
        if (!manager.isRequestPinAppWidgetSupported) return false
        awaitingPinnedWidget = manager.requestPinAppWidget(kind.provider(this), null, null)
        return awaitingPinnedWidget
    }

    private fun versionName(): String =
        packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()

    private fun openExternally(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.action_failed, Toast.LENGTH_LONG).show()
        }
    }

    private fun openWidgetSettings(appWidgetId: Int) {
        startActivity(
            Intent(this, WidgetConfigActivity::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
        )
    }

    private companion object {
        const val KEY_AWAITING_PIN = "awaiting_pin"
    }
}
