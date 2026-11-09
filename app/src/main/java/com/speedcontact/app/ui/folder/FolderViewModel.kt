package com.speedcontact.app.ui.folder

import android.app.Application
import android.appwidget.AppWidgetManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.speedcontact.app.SpeedContactApp
import com.speedcontact.app.widget.WidgetKind
import com.speedcontact.app.widget.WidgetUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class FolderViewModel(
    application: Application,
    savedState: SavedStateHandle,
) : AndroidViewModel(application) {
    private val app = application as SpeedContactApp
    private val loader = app.widgetStateLoader

    val appWidgetId: Int =
        savedState[AppWidgetManager.EXTRA_APPWIDGET_ID] ?: AppWidgetManager.INVALID_APPWIDGET_ID

    val kind: WidgetKind = WidgetKind.of(app, appWidgetId)

    /** Null only for the moment the people are being read. */
    val state: StateFlow<WidgetUiState?> = loader.loadKeys(appWidgetId)
        .mapLatest { key -> if (kind.isList) loader.loadNames(key) else loader.loadFolderScreen(key) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * The app's scope, since the folder may close before the size is saved. A names list opened
     * from its "show all" button is also on the home screen, so that copy has to follow.
     */
    fun stepFontSize(steps: Int) {
        app.appScope.launch {
            app.store.updateWidget(appWidgetId) { it.copy(list = it.list.withFontStep(steps)) }
            if (!kind.isFolder) app.widgetUpdater.update(appWidgetId)
        }
    }
}
