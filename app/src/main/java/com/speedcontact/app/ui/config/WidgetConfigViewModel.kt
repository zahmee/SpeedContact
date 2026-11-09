package com.speedcontact.app.ui.config

import android.app.Application
import android.appwidget.AppWidgetManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.speedcontact.app.SpeedContactApp
import com.speedcontact.app.data.AppJson
import com.speedcontact.app.data.DeviceContact
import com.speedcontact.app.data.ResolvedPerson
import com.speedcontact.app.model.ContactSource
import com.speedcontact.app.model.FolderIcon
import com.speedcontact.app.model.SelectedContact
import com.speedcontact.app.model.WidgetConfig
import com.speedcontact.app.widget.ContactsChangedWorker
import com.speedcontact.app.widget.WidgetContent
import com.speedcontact.app.widget.WidgetKind
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConfigUiState(
    val config: WidgetConfig = WidgetConfig(),
    val isLoaded: Boolean = false,
    val hasContactsPermission: Boolean = false,
    val people: List<ResolvedPerson> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class WidgetConfigViewModel(
    application: Application,
    private val savedState: SavedStateHandle,
) : AndroidViewModel(application) {
    private val app = application as SpeedContactApp

    val appWidgetId: Int =
        savedState[AppWidgetManager.EXTRA_APPWIDGET_ID] ?: AppWidgetManager.INVALID_APPWIDGET_ID

    val kind: WidgetKind = WidgetKind.of(app, appWidgetId)

    private val draft = MutableStateFlow(
        savedState.get<String>(KEY_DRAFT)?.let { AppJson.decodeFromString<WidgetConfig>(it) },
    )
    private val hasPermission = MutableStateFlow(app.contacts.hasPermission())
    private val _contacts = MutableStateFlow<List<DeviceContact>>(emptyList())

    /** Everyone on the phone who can be chosen; loaded once the permission is granted. */
    val contacts: StateFlow<List<DeviceContact>> = _contacts.asStateFlow()

    private val people = combine(draft.filterNotNull(), _contacts) { config, all -> config to all }
        .mapLatest { (config, all) -> app.contacts.resolvePeople(config, all) }

    val uiState: StateFlow<ConfigUiState> =
        combine(draft, hasPermission, people) { config, permission, resolved ->
            ConfigUiState(
                config = config ?: WidgetConfig(),
                isLoaded = config != null,
                hasContactsPermission = permission,
                people = resolved,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConfigUiState())

    /** The same tiles the real widget will show, re-rendered as settings change. */
    val preview: StateFlow<WidgetContent?> =
        combine(draft.filterNotNull(), hasPermission, _contacts) { config, _, _ -> config }
            .mapLatest { config ->
                delay(PREVIEW_DEBOUNCE_MILLIS)
                val lastCalled = app.store.state.first().lastCalled
                if (kind.isList) {
                    app.widgetStateLoader.loadNameContent(config, lastCalled)
                } else {
                    app.widgetStateLoader.loadContent(config, lastCalled)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            if (draft.value == null) {
                draft.value = app.store.state.first().widgets[appWidgetId] ?: kind.defaultConfig(app)
            }
        }
    }

    fun onPermissionResult() {
        hasPermission.value = app.contacts.hasPermission()
        refreshContacts()
    }

    private fun refreshContacts() {
        viewModelScope.launch { _contacts.value = app.contacts.contactsWithPhones() }
    }

    fun update(transform: (WidgetConfig) -> WidgetConfig) {
        draft.update { current ->
            current?.let(transform)?.also {
                savedState[KEY_DRAFT] = AppJson.encodeToString(WidgetConfig.serializer(), it)
            }
        }
    }

    fun setManualContacts(selected: List<SelectedContact>) = update { it.copy(contacts = selected) }

    fun remove(lookupKey: String) = update { config ->
        config.copy(contacts = config.contacts.filterNot { it.lookupKey == lookupKey })
    }

    /** Applies a drag in the people list to whichever order the current source uses. */
    fun move(fromLookupKey: String, toLookupKey: String) = update { config ->
        when (config.source) {
            ContactSource.MANUAL -> {
                val from = config.contacts.indexOfFirst { it.lookupKey == fromLookupKey }
                val to = config.contacts.indexOfFirst { it.lookupKey == toLookupKey }
                if (from < 0 || to < 0) {
                    config
                } else {
                    config.copy(contacts = config.contacts.toMutableList().apply { add(to, removeAt(from)) })
                }
            }

            ContactSource.FAVORITES -> {
                val keys = uiState.value.people.map { it.contact.lookupKey }.toMutableList()
                val from = keys.indexOf(fromLookupKey)
                val to = keys.indexOf(toLookupKey)
                if (from < 0 || to < 0) {
                    config
                } else {
                    config.copy(favoriteOrder = keys.apply { add(to, removeAt(from)) })
                }
            }
        }
    }

    /** Returns false when the picture could not be read, leaving the current icon in place. */
    suspend fun setCustomIcon(uri: Uri): Boolean {
        val name = app.folderIcons.import(uri) ?: return false
        update { it.copy(folderIcon = FolderIcon.CUSTOM, customIconFile = name) }
        return true
    }

    fun loadCustomIcon(name: String): Bitmap? = app.folderIcons.load(name)

    suspend fun save() {
        val config = draft.value ?: return
        app.store.saveWidget(appWidgetId, config)
        ContactsChangedWorker.ensureScheduled(app)
        app.widgetStateLoader.requestRefresh()
        app.widgetUpdater.update(appWidgetId)
        app.deleteUnusedFolderIcons()
    }

    private companion object {
        const val KEY_DRAFT = "draft"
        const val PREVIEW_DEBOUNCE_MILLIS = 120L
    }
}
