package com.speedcontact.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import androidx.annotation.StringRes
import com.speedcontact.app.R
import com.speedcontact.app.model.WidgetConfig

/**
 * The kinds of home screen widget. Every placed widget of any kind has its own [WidgetConfig].
 * A folder is one icon that opens a full screen; a list shows names in place of photos.
 */
enum class WidgetKind(
    val receiver: Class<out AppWidgetProvider>,
    @StringRes val untitledLabel: Int,
    val isFolder: Boolean = false,
    val isList: Boolean = false,
) {
    GRID(ContactGridWidgetReceiver::class.java, R.string.home_widget_untitled),
    FOLDER(ContactFolderWidgetReceiver::class.java, R.string.folder_untitled, isFolder = true),
    NAME_LIST(ContactListWidgetReceiver::class.java, R.string.list_widget_label, isList = true),
    LIST_FOLDER(
        ContactListFolderWidgetReceiver::class.java,
        R.string.list_folder_widget_label,
        isFolder = true,
        isList = true,
    );

    fun provider(context: Context): ComponentName = ComponentName(context, receiver)

    /** The widgets of this kind now on the home screen. */
    fun placedIds(context: Context): List<Int> =
        AppWidgetManager.getInstance(context).getAppWidgetIds(provider(context)).toList()

    /** A folder needs a name to tell it apart on the home screen; the others show their people. */
    fun defaultConfig(context: Context): WidgetConfig = when (this) {
        GRID, NAME_LIST -> WidgetConfig()
        FOLDER -> WidgetConfig(
            title = context.getString(R.string.folder_default_title),
            showTitle = true,
        )
        LIST_FOLDER -> WidgetConfig(
            title = context.getString(R.string.list_folder_default_title),
            showTitle = true,
        )
    }

    companion object {
        fun of(context: Context, appWidgetId: Int): WidgetKind {
            val provider = AppWidgetManager.getInstance(context).getAppWidgetInfo(appWidgetId)?.provider
            return entries.firstOrNull { it.receiver.name == provider?.className } ?: GRID
        }

        /** Whether any widget of any kind is still on the home screen. */
        fun anyPlaced(context: Context): Boolean = entries.any { it.placedIds(context).isNotEmpty() }
    }
}
