package com.speedcontact.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.net.toUri
import com.speedcontact.app.R
import com.speedcontact.app.action.ContactActionActivity
import com.speedcontact.app.app
import com.speedcontact.app.domain.GridMetrics
import com.speedcontact.app.model.PhotoShape
import com.speedcontact.app.model.WidgetConfig
import com.speedcontact.app.ui.descriptionRes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** Hands the launcher one row of photos at a time, so any number stays within Android's limits. */
class ContactGridService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        return ContactGridFactory(applicationContext, appWidgetId)
    }

    companion object {
        /** The address makes Android keep a separate list for each widget. */
        fun intent(context: Context, appWidgetId: Int): Intent =
            Intent(context, ContactGridService::class.java)
                .setData("speedcontact://grid/$appWidgetId".toUri())
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }
}

/** The views of one column in a grid row. */
private class TileSlot(val root: Int, val photo: Int, val name: Int, val fullName: Int)

private val TILE_SLOTS = listOf(
    TileSlot(R.id.tile_0, R.id.tile_photo_0, R.id.tile_name_0, R.id.tile_full_name_0),
    TileSlot(R.id.tile_1, R.id.tile_photo_1, R.id.tile_name_1, R.id.tile_full_name_1),
    TileSlot(R.id.tile_2, R.id.tile_photo_2, R.id.tile_name_2, R.id.tile_full_name_2),
    TileSlot(R.id.tile_3, R.id.tile_photo_3, R.id.tile_name_3, R.id.tile_full_name_3),
    TileSlot(R.id.tile_4, R.id.tile_photo_4, R.id.tile_name_4, R.id.tile_full_name_4),
)

private class ContactGridFactory(
    private val context: Context,
    private val appWidgetId: Int,
) : RemoteViewsService.RemoteViewsFactory {
    private var config = WidgetConfig()
    private var columns = 1
    private var rows = emptyList<List<WidgetTile>>()

    override fun onCreate() = Unit

    // Android calls this on a background thread and waits for it, so blocking here is expected.
    override fun onDataSetChanged() {
        val loader = context.app.widgetStateLoader
        val state = runBlocking { loader.load(loader.loadKeys(appWidgetId).first()) }
        config = state.config
        columns = GridMetrics.columns(WidgetDimensions.availableWidthDp(context, appWidgetId), config.size)
        val tiles = (state.content as? WidgetContent.Tiles)?.items.orEmpty()
        rows = tiles.chunked(columns)
    }

    override fun onDestroy() {
        rows = emptyList()
    }

    override fun getCount(): Int = rows.size

    /**
     * Recycled rows keep their previous state, so every slot is set in both directions. A slot
     * past the last person keeps its column's width but shows nothing; one past the column count
     * takes no width at all.
     */
    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_grid_row)
        val row = rows.getOrNull(position).orEmpty()
        val (side, bottom) = nameInsets()

        TILE_SLOTS.forEachIndexed { column, slot ->
            val tile = row.getOrNull(column)
            views.setViewVisibility(
                slot.root,
                when {
                    column >= columns -> View.GONE
                    tile == null -> View.INVISIBLE
                    else -> View.VISIBLE
                },
            )
            if (tile != null) bindTile(views, slot, tile, side, bottom)
        }
        return views
    }

    private fun bindTile(views: RemoteViews, slot: TileSlot, tile: WidgetTile, side: Int, bottom: Int) {
        val person = tile.person
        views.setImageViewBitmap(slot.photo, tile.image)
        views.setContentDescription(
            slot.root,
            context.getString(config.tapAction.descriptionRes, person.name),
        )
        views.setOnClickFillInIntent(slot.root, ContactActionActivity.fillInIntent(config, person))

        val fontSize = GridMetrics.nameFontSizeSp(config.size).toFloat()

        views.setViewVisibility(slot.name, if (config.name.showName) View.VISIBLE else View.GONE)
        if (config.name.showName) {
            views.setTextViewText(slot.name, person.name)
            views.setTextViewTextSize(slot.name, TypedValue.COMPLEX_UNIT_SP, fontSize)
            views.setViewPadding(slot.name, side, 0, side, bottom)
        }

        views.setViewVisibility(slot.fullName, if (tile.showsFullName) View.VISIBLE else View.GONE)
        if (tile.showsFullName) {
            views.setTextViewText(slot.fullName, person.name)
            views.setTextViewTextSize(slot.fullName, TypedValue.COMPLEX_UNIT_SP, fontSize)
            views.setTextColor(slot.fullName, config.placeholder.nameArgb.toInt())
            views.setInt(slot.fullName, "setMaxLines", GridMetrics.fullNameMaxLines(config.size))
            views.setViewPadding(slot.fullName, side, 0, side, 0)
        }
    }

    /** A circle is narrower near its bottom, so a name sits further in and higher up. */
    private fun nameInsets(): Pair<Int, Int> {
        val density = context.resources.displayMetrics.density
        return if (config.shape == PhotoShape.CIRCLE) {
            val width = WidgetDimensions.availableWidthDp(context, appWidgetId)
            val cellPx = GridMetrics.cellDp(width, columns) * density
            (cellPx * CIRCLE_SIDE_INSET).toInt() to (cellPx * CIRCLE_BOTTOM_INSET).toInt()
        } else {
            (NAME_INSET_DP * density).toInt() to (NAME_INSET_DP * density).toInt()
        }
    }

    // A blank row, so Android's own "Loading" text never flashes in the grid.
    override fun getLoadingView(): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_grid_row)

    override fun getViewTypeCount(): Int = 1

    /** Identifies a row by who is in it, so it only redraws when they change. */
    override fun getItemId(position: Int): Long =
        rows.getOrNull(position)?.fold(ROW_ID_SEED) { id, tile -> id * ROW_ID_FACTOR + tile.person.contactId }
            ?: position.toLong()

    override fun hasStableIds(): Boolean = true

    private companion object {
        const val CIRCLE_SIDE_INSET = 0.14f
        const val CIRCLE_BOTTOM_INSET = 0.08f
        const val NAME_INSET_DP = 4
        const val ROW_ID_SEED = 17L
        const val ROW_ID_FACTOR = 1_000_003L
    }
}
