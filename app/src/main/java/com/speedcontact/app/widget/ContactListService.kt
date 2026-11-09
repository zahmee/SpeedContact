package com.speedcontact.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.net.toUri
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import com.speedcontact.app.R
import com.speedcontact.app.action.ContactActionActivity
import com.speedcontact.app.app
import com.speedcontact.app.model.WidgetBackground
import com.speedcontact.app.model.WidgetConfig
import com.speedcontact.app.ui.asLeftToRight
import com.speedcontact.app.ui.descriptionRes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** Hands the launcher one line of a names list at a time. */
class ContactListService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        return ContactListFactory(applicationContext, appWidgetId)
    }

    companion object {
        /** The address makes Android keep a separate list for each widget. */
        fun intent(context: Context, appWidgetId: Int): Intent =
            Intent(context, ContactListService::class.java)
                .setData("speedcontact://list/$appWidgetId".toUri())
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }
}

private class ContactListFactory(
    private val context: Context,
    private val appWidgetId: Int,
) : RemoteViewsService.RemoteViewsFactory {
    private var config = WidgetConfig()
    private var people = emptyList<WidgetPerson>()

    override fun onCreate() = Unit

    // Android calls this on a background thread and waits for it, so blocking here is expected.
    override fun onDataSetChanged() {
        val loader = context.app.widgetStateLoader
        val state = runBlocking { loader.loadNames(loader.loadKeys(appWidgetId).first()) }
        config = state.config
        people = (state.content as? WidgetContent.Names)?.items.orEmpty()
    }

    override fun onDestroy() {
        people = emptyList()
    }

    override fun getCount(): Int = people.size

    override fun getViewAt(position: Int): RemoteViews {
        val layout = if (config.background == WidgetBackground.TRANSPARENT) {
            R.layout.widget_list_item_shadow
        } else {
            R.layout.widget_list_item
        }
        val views = RemoteViews(context.packageName, layout)
        val person = people.getOrNull(position) ?: return views
        val options = config.list

        views.setContentDescription(
            R.id.row,
            context.getString(config.tapAction.descriptionRes, person.name),
        )
        views.setOnClickFillInIntent(R.id.row, ContactActionActivity.fillInIntent(config, person))

        val verticalPadding = (options.spacing.rowPaddingDp * context.resources.displayMetrics.density).toInt()
        views.setViewPadding(R.id.row_text, 0, verticalPadding, 0, verticalPadding)

        // A widget cannot change a text's weight directly, but it can carry it inside the text.
        val name = if (options.boldName) buildSpannedString { bold { append(person.name) } } else person.name
        views.setTextViewText(R.id.row_name, name)
        views.setTextViewTextSize(R.id.row_name, TypedValue.COMPLEX_UNIT_SP, options.fontSizeSp.toFloat())
        WidgetColors.applyText(views, R.id.row_name, config.background)

        // Recycled rows keep their previous state, so visibility is set in both directions.
        bindNumber(views, R.id.row_number, options.showsNumberBelow, person)
        bindNumber(views, R.id.row_number_beside, options.showsNumberBeside, person)

        val showDivider = options.showDividers && position < people.lastIndex
        views.setViewVisibility(R.id.row_divider, if (showDivider) View.VISIBLE else View.GONE)
        if (showDivider) WidgetColors.applyIcon(views, R.id.row_divider, config.background)
        return views
    }

    private fun bindNumber(views: RemoteViews, viewId: Int, show: Boolean, person: WidgetPerson) {
        views.setViewVisibility(viewId, if (show) View.VISIBLE else View.GONE)
        if (!show) return
        views.setTextViewText(viewId, person.number.asLeftToRight())
        views.setTextViewTextSize(viewId, TypedValue.COMPLEX_UNIT_SP, config.list.numberFontSizeSp.toFloat())
        WidgetColors.applyText(views, viewId, config.background)
    }

    // A blank row, so Android's own "Loading" text never flashes in the list.
    override fun getLoadingView(): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_list_item)

    // Plain and shadowed rows; changing the background can swap one for the other on a live list.
    override fun getViewTypeCount(): Int = 2

    override fun getItemId(position: Int): Long =
        people.getOrNull(position)?.contactId ?: position.toLong()

    override fun hasStableIds(): Boolean = true
}
