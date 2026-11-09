package com.speedcontact.app.ui.folder

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.speedcontact.app.action.ContactActionActivity
import com.speedcontact.app.model.TapAction
import com.speedcontact.app.ui.SpeedContactTheme
import com.speedcontact.app.ui.config.WidgetConfigActivity
import com.speedcontact.app.ui.isDarkBackground
import com.speedcontact.app.widget.WidgetPerson

/** The full screen behind a folder icon. Closes with its close button or the back gesture. */
class FolderActivity : ComponentActivity() {
    private val viewModel: FolderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (viewModel.appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            SpeedContactTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                state?.let { current ->
                    val isDark = isDarkBackground(current.config.background)
                    LaunchedEffect(isDark) { matchSystemBars(isDark) }
                    FolderScreen(
                        kind = viewModel.kind,
                        state = current,
                        onClose = ::finish,
                        onOpenSettings = ::openSettings,
                        onPersonAction = ::performAction,
                        onFontStep = viewModel::stepFontSize,
                    )
                }
            }
        }
    }

    private fun matchSystemBars(isDark: Boolean) {
        val transparent = Color.TRANSPARENT
        val style = if (isDark) {
            SystemBarStyle.dark(transparent)
        } else {
            SystemBarStyle.light(transparent, transparent)
        }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    private fun performAction(action: TapAction, person: WidgetPerson) {
        startActivity(ContactActionActivity.intent(this, action, person))
        finish()
    }

    private fun openSettings() {
        startActivity(
            Intent(this, WidgetConfigActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, viewModel.appWidgetId),
        )
        finish()
    }
}
