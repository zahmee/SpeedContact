package com.speedcontact.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.speedcontact.app.app
import kotlinx.coroutines.launch

/** What every kind of widget does when Android tells it something changed. */
abstract class BaseWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_PAGE) {
            super.onReceive(context, intent)
            return
        }
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        val direction = intent.getIntExtra(EXTRA_DIRECTION, 0)
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID && direction != 0) {
            runAsync(context) { context.app.widgetUpdater.page(appWidgetId, direction) }
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        runAsync(context) {
            appWidgetIds.forEach { context.app.widgetUpdater.update(it) }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        runAsync(context) { context.app.widgetUpdater.update(appWidgetId) }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        runAsync(context) {
            appWidgetIds.forEach { context.app.store.removeWidget(it) }
            context.app.deleteUnusedFolderIcons()
        }
    }

    override fun onEnabled(context: Context) {
        ContactsChangedWorker.ensureScheduled(context)
    }

    override fun onDisabled(context: Context) {
        ContactsChangedWorker.cancelIfUnused(context)
    }

    private fun runAsync(context: Context, block: suspend () -> Unit) {
        val pending = goAsync()
        context.app.appScope.launch {
            try {
                block()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_PAGE = "com.speedcontact.app.action.PAGE"
        const val EXTRA_DIRECTION = "direction"
    }
}

// These names are how Android remembers widgets already on a home screen; they must not change.
class ContactGridWidgetReceiver : BaseWidgetProvider()

class ContactFolderWidgetReceiver : BaseWidgetProvider()

class ContactListWidgetReceiver : BaseWidgetProvider()

class ContactListFolderWidgetReceiver : BaseWidgetProvider()
