package com.speedcontact.app.widget

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.widget.RemoteViews
import com.speedcontact.app.R
import com.speedcontact.app.model.WidgetBackground

/** Colors for what sits on a widget's background, so it stays readable on whichever was chosen. */
object WidgetColors {
    private val DARK_FOREGROUND = Color.parseColor("#15181D")

    /** Phone colors only exist from Android 12, so older versions fall back to the blue. */
    fun solidBackground(background: WidgetBackground): WidgetBackground =
        if (background == WidgetBackground.DYNAMIC) WidgetBackground.BLUE else background

    fun applyText(views: RemoteViews, viewId: Int, background: WidgetBackground) {
        if (background == WidgetBackground.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Resolved by the launcher, so this follows the wallpaper and night mode by itself.
            views.setColorStateList(viewId, "setTextColor", R.color.widget_dynamic_foreground)
        } else {
            views.setTextColor(viewId, solidForeground(background))
        }
    }

    fun applyIcon(views: RemoteViews, viewId: Int, background: WidgetBackground) {
        if (background == WidgetBackground.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            views.setColorStateList(viewId, "setImageTintList", R.color.widget_dynamic_foreground)
        } else {
            applyImageColor(views, viewId, solidForeground(background))
        }
    }

    /**
     * Android 12 and later use a tint, because a color filter would override the phone colors tint
     * and cannot be cleared again from a widget. Older versions have only the filter.
     */
    fun applyImageColor(views: RemoteViews, viewId: Int, color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            views.setColorStateList(viewId, "setImageTintList", ColorStateList.valueOf(color))
        } else {
            views.setInt(viewId, "setColorFilter", color)
        }
    }

    private fun solidForeground(background: WidgetBackground): Int =
        if (solidBackground(background).isDark) Color.WHITE else DARK_FOREGROUND
}
