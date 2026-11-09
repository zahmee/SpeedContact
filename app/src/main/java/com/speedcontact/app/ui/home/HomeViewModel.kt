package com.speedcontact.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.speedcontact.app.SpeedContactApp
import com.speedcontact.app.model.WidgetConfig
import com.speedcontact.app.widget.WidgetKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/** A widget on the home screen. [config] is null until it has been set up for the first time. */
data class HomeItem(
    val appWidgetId: Int,
    val kind: WidgetKind,
    val config: WidgetConfig?,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SpeedContactApp
    private val _items = MutableStateFlow<List<HomeItem>>(emptyList())
    val items: StateFlow<List<HomeItem>> = _items.asStateFlow()

    suspend fun refresh(): List<HomeItem> {
        val widgets = app.store.state.first().widgets
        val items = WidgetKind.entries.flatMap { kind ->
            kind.placedIds(app).map { appWidgetId ->
                HomeItem(appWidgetId, kind, widgets[appWidgetId])
            }
        }
        _items.value = items
        return items
    }
}
