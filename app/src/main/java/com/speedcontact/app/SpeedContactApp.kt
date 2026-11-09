package com.speedcontact.app

import android.app.Application
import android.content.Context
import com.speedcontact.app.data.AppStateRepository
import com.speedcontact.app.data.ContactsRepository
import com.speedcontact.app.data.FolderIconStore
import com.speedcontact.app.widget.WidgetStateLoader
import com.speedcontact.app.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first

class SpeedContactApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val store by lazy { AppStateRepository(this) }
    val contacts by lazy { ContactsRepository(this) }
    val folderIcons by lazy { FolderIconStore(this) }
    val widgetStateLoader by lazy { WidgetStateLoader(this, store, contacts) }
    val widgetUpdater by lazy { WidgetUpdater(this, store, contacts, folderIcons) }

    override fun onCreate() {
        super.onCreate()
        // Left behind by the 1.2.x trial builds, which recorded what a launcher asked of the widgets.
        deleteFile("widget_diagnostics.txt")
    }

    /** A picture stops being needed when its widget is removed or given another picture. */
    suspend fun deleteUnusedFolderIcons() {
        val used = store.state.first().widgets.values.mapNotNull { it.customIconFile }.toSet()
        folderIcons.deleteUnused(used)
    }
}

val Context.app: SpeedContactApp
    get() = applicationContext as SpeedContactApp
