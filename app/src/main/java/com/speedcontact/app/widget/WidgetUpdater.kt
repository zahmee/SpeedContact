package com.speedcontact.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import android.view.View
import android.widget.RemoteViews
import androidx.core.net.toUri
import com.speedcontact.app.R
import com.speedcontact.app.action.ContactActionActivity
import com.speedcontact.app.data.AppStateRepository
import com.speedcontact.app.data.ContactsRepository
import com.speedcontact.app.data.FolderIconStore
import com.speedcontact.app.domain.GridMetrics
import com.speedcontact.app.model.ContactSource
import com.speedcontact.app.model.FolderIcon
import com.speedcontact.app.model.SortOrder
import com.speedcontact.app.model.WidgetBackground
import com.speedcontact.app.model.WidgetConfig
import com.speedcontact.app.ui.config.WidgetConfigActivity
import com.speedcontact.app.ui.drawableRes
import com.speedcontact.app.ui.folder.FolderActivity
import kotlinx.coroutines.flow.first

/**
 * Draws the home screen widgets. Their people come from [ContactGridService] for photos and
 * [ContactListService] for names.
 */
class WidgetUpdater(
    context: Context,
    private val store: AppStateRepository,
    private val contacts: ContactsRepository,
    private val folderIcons: FolderIconStore,
) {
    private val appContext = context.applicationContext
    private val packageName = appContext.packageName
    private val manager get() = AppWidgetManager.getInstance(appContext)

    suspend fun update(appWidgetId: Int) {
        val kind = WidgetKind.of(appContext, appWidgetId)
        val config = store.state.first().widgets[appWidgetId] ?: kind.defaultConfig(appContext)
        if (kind.isFolder) {
            updateFolder(appWidgetId, kind, config)
        } else {
            updatePeople(appWidgetId, kind, config)
        }
    }

    suspend fun updateAll() {
        WidgetKind.entries.forEach { kind ->
            kind.placedIds(appContext).forEach { update(it) }
        }
    }

    /** After a call, only widgets sorted by last call change order; folders load when opened. */
    suspend fun updateRecentlySorted() {
        val widgets = store.state.first().widgets
        WidgetKind.entries.filterNot { it.isFolder }
            .flatMap { it.placedIds(appContext) }
            .filter { widgets[it]?.sort == SortOrder.RECENT }
            .forEach { update(it) }
    }

    private fun updateFolder(appWidgetId: Int, kind: WidgetKind, config: WidgetConfig) {
        val hasName = config.showTitle && config.title.isNotBlank()
        val name = if (hasName) config.title else appContext.getString(kind.untitledLabel)

        val views = RemoteViews(packageName, R.layout.widget_folder).apply {
            setTextViewText(R.id.folder_name, config.title)
            setViewVisibility(R.id.folder_name, if (hasName) View.VISIBLE else View.GONE)
            setContentDescription(R.id.folder_root, appContext.getString(R.string.cd_open_folder, name))
            setOnClickPendingIntent(R.id.folder_root, openActivity(FolderActivity::class.java, appWidgetId))
        }
        applyFolderIcon(views, config)
        manager.updateAppWidget(appWidgetId, views)
    }

    /** A picture that can no longer be read falls back to the app's own icon. */
    private fun applyFolderIcon(views: RemoteViews, config: WidgetConfig) {
        val picture = config.customIconFile
            ?.takeIf { config.folderIcon == FolderIcon.CUSTOM }
            ?.let(folderIcons::load)
        if (picture != null) {
            views.setImageViewBitmap(R.id.folder_icon, picture)
        } else {
            views.setImageViewResource(R.id.folder_icon, config.folderIcon.drawableRes)
        }
    }

    /**
     * The launcher reapplies an update onto the views it already has, so anything not set here
     * keeps its previous value. Every state must be set on both paths, never only when it is on.
     */
    private suspend fun updatePeople(appWidgetId: Int, kind: WidgetKind, config: WidgetConfig) {
        val views = RemoteViews(packageName, R.layout.widget_people)
        applyLanguage(views)
        applyColors(views, config.background)

        val hasTitle = config.showTitle && config.title.isNotBlank()
        views.setViewVisibility(R.id.widget_title, if (hasTitle) View.VISIBLE else View.GONE)
        if (hasTitle) {
            views.setTextViewText(R.id.widget_title, config.title)
            views.setOnClickPendingIntent(
                R.id.widget_title,
                openActivity(WidgetConfigActivity::class.java, appWidgetId),
            )
        }

        val message = messageFor(config)
        val showBar = config.showPageBar && message == null && overflows(appWidgetId, kind, config, hasTitle)
        views.setViewVisibility(R.id.widget_people, if (message == null) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.widget_message, if (message == null) View.GONE else View.VISIBLE)
        views.setViewVisibility(R.id.widget_more_bar, if (showBar) View.VISIBLE else View.GONE)
        if (showBar) {
            views.setOnClickPendingIntent(R.id.widget_show_all, openActivity(FolderActivity::class.java, appWidgetId))
            views.setOnClickPendingIntent(R.id.widget_page_up, pageIntent(appWidgetId, kind, -1))
            views.setOnClickPendingIntent(R.id.widget_page_down, pageIntent(appWidgetId, kind, 1))
        }
        if (message != null) {
            views.setTextViewText(R.id.widget_message_text, appContext.getString(message))
            views.setOnClickPendingIntent(
                R.id.widget_message,
                openActivity(WidgetConfigActivity::class.java, appWidgetId),
            )
        } else {
            val adapter = if (kind.isList) {
                ContactListService.intent(appContext, appWidgetId)
            } else {
                ContactGridService.intent(appContext, appWidgetId)
            }
            @Suppress("DEPRECATION") // Its replacement sends every photo at once; this sends a row at a time.
            views.setRemoteAdapter(R.id.widget_people, adapter)
            views.setPendingIntentTemplate(R.id.widget_people, tapTemplate(appWidgetId))
        }

        manager.updateAppWidget(appWidgetId, views)
        // Also how a resized grid gets its new column count: the rows are built by its factory.
        @Suppress("DEPRECATION")
        manager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_people)
    }

    /** Moves a widget by one screenful, for launchers that keep the drag to themselves. */
    suspend fun page(appWidgetId: Int, direction: Int) {
        val kind = WidgetKind.of(appContext, appWidgetId)
        val config = store.state.first().widgets[appWidgetId] ?: return
        val hasTitle = config.showTitle && config.title.isNotBlank()
        val height = WidgetDimensions.availableHeightDp(appContext, appWidgetId, hasTitle)
        val step = if (kind.isList) {
            GridMetrics.visibleListRows(height, config.list, fontScale)
        } else {
            val width = WidgetDimensions.availableWidthDp(appContext, appWidgetId)
            GridMetrics.visibleRows(width, height, config.size)
        }
        val views = RemoteViews(packageName, R.layout.widget_people)
        views.setRelativeScrollPosition(R.id.widget_people, direction * step)
        manager.partiallyUpdateAppWidget(appWidgetId, views)
    }

    private val fontScale get() = appContext.resources.configuration.fontScale

    private suspend fun overflows(
        appWidgetId: Int,
        kind: WidgetKind,
        config: WidgetConfig,
        hasTitle: Boolean,
    ): Boolean {
        val count = contacts.resolvePeople(config).size
        val height = WidgetDimensions.availableHeightDp(appContext, appWidgetId, hasTitle)
        return if (kind.isList) {
            GridMetrics.listOverflows(count, height, config.list, fontScale)
        } else {
            GridMetrics.overflows(count, WidgetDimensions.availableWidthDp(appContext, appWidgetId), height, config.size)
        }
    }

    private fun pageIntent(appWidgetId: Int, kind: WidgetKind, direction: Int): PendingIntent {
        val intent = Intent(appContext, kind.receiver)
            .setAction(BaseWidgetProvider.ACTION_PAGE)
            .setData("speedcontact://page/$appWidgetId/$direction".toUri())
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            .putExtra(BaseWidgetProvider.EXTRA_DIRECTION, direction)
        return PendingIntent.getBroadcast(
            appContext,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private suspend fun messageFor(config: WidgetConfig): Int? = when {
        !contacts.hasPermission() -> R.string.widget_message_permission
        contacts.resolvePeople(config).isNotEmpty() -> null
        config.source == ContactSource.FAVORITES -> R.string.widget_message_no_favorites
        else -> R.string.widget_message_choose_people
    }

    /**
     * The launcher lays a widget out in the phone's language, so a string in its XML and its
     * direction would ignore a language chosen for this app. Both are set from here instead.
     */
    private fun applyLanguage(views: RemoteViews) {
        // Read from the language itself: on Android 15, going back to the phone's language leaves
        // the configuration's own direction left to right even when that language is Arabic.
        val language = appContext.resources.configuration.locales[0]
        val isRightToLeft = TextUtils.getLayoutDirectionFromLocale(language) == View.LAYOUT_DIRECTION_RTL
        views.setInt(
            android.R.id.background,
            "setLayoutDirection",
            // Never left to the locale: that would be the launcher's, which is the phone's.
            if (isRightToLeft) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR,
        )
        views.setTextViewText(R.id.widget_show_all, appContext.getString(R.string.widget_show_all))
        views.setContentDescription(R.id.widget_page_up, appContext.getString(R.string.cd_page_up))
        views.setContentDescription(R.id.widget_page_down, appContext.getString(R.string.cd_page_down))
    }

    private fun applyColors(views: RemoteViews, background: WidgetBackground) {
        if (background == WidgetBackground.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            views.setInt(R.id.widget_background, "setImageAlpha", FULLY_OPAQUE)
            // Resolved by the launcher, so this follows the wallpaper and night mode by itself.
            views.setColorStateList(R.id.widget_background, "setImageTintList", R.color.widget_dynamic_background)
        } else {
            val argb = WidgetColors.solidBackground(background).argb.toInt()
            WidgetColors.applyImageColor(views, R.id.widget_background, argb or OPAQUE)
            views.setInt(R.id.widget_background, "setImageAlpha", Color.alpha(argb))
        }
        WidgetColors.applyText(views, R.id.widget_title, background)
        WidgetColors.applyText(views, R.id.widget_message_text, background)
        WidgetColors.applyIcon(views, R.id.widget_message_icon, background)
        WidgetColors.applyText(views, R.id.widget_show_all, background)
        WidgetColors.applyIcon(views, R.id.widget_page_up, background)
        WidgetColors.applyIcon(views, R.id.widget_page_down, background)
    }

    private fun openActivity(activity: Class<*>, appWidgetId: Int): PendingIntent {
        val intent = Intent(appContext, activity)
            .setData(widgetUri(appWidgetId))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        return PendingIntent.getActivity(
            appContext,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Mutable so each tile can add its person; the target is fixed to this app's own screen. */
    private fun tapTemplate(appWidgetId: Int): PendingIntent {
        val intent = Intent(appContext, ContactActionActivity::class.java)
            .setData(widgetUri(appWidgetId))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            appContext,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    private fun widgetUri(appWidgetId: Int): Uri = "speedcontact://widget/$appWidgetId".toUri()

    private companion object {
        const val OPAQUE = 0xFF000000.toInt()
        const val FULLY_OPAQUE = 255
    }
}

object WidgetDimensions {
    private const val FALLBACK_WIDTH_DP = 320
    private const val FALLBACK_HEIGHT_DP = 240
    private const val WIDGET_PADDING_DP = 6
    private const val TITLE_HEIGHT_DP = 28

    /** The height people can use, for the orientation the widget is showing in now. */
    fun availableHeightDp(context: Context, appWidgetId: Int, hasTitle: Boolean): Float {
        val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
        val isLandscape =
            context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val height = options.getInt(
            if (isLandscape) {
                AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT
            } else {
                AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT
            },
        )
        val title = if (hasTitle) TITLE_HEIGHT_DP else 0
        return ((if (height > 0) height else FALLBACK_HEIGHT_DP) - WIDGET_PADDING_DP * 2 - title).toFloat()
    }

    /** The width tiles can use, for the screen orientation the widget is showing in now. */
    fun availableWidthDp(context: Context, appWidgetId: Int): Float {
        val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
        val isLandscape =
            context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val width = options.getInt(
            if (isLandscape) {
                AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH
            } else {
                AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
            },
        )
        return ((if (width > 0) width else FALLBACK_WIDTH_DP) - WIDGET_PADDING_DP * 2).toFloat()
    }
}
