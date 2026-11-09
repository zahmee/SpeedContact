package com.speedcontact.app.domain

import com.speedcontact.app.model.ListOptions
import com.speedcontact.app.model.PhotoSize
import kotlin.math.roundToInt

object GridMetrics {
    private const val LIST_NAME_LINE_SCALE = 1.7f
    private const val LIST_NUMBER_LINE_SCALE = 1.35f

    /** The grid's row layout has this many slots. */
    const val MAX_COLUMNS = 5
    private const val MIN_BITMAP_PX = 96
    private const val MAX_BITMAP_PX = 320
    private const val BITMAP_OVERSCAN = 1.3f

    /** A folder has the whole screen, so its photos are larger than a widget's. */
    private const val FOLDER_SCALE = 1.3f
    private const val MAX_FOLDER_COLUMNS = 8
    private const val MAX_FOLDER_BITMAP_PX = 720

    fun columns(availableWidthDp: Float, size: PhotoSize): Int =
        (availableWidthDp / size.targetCellDp).roundToInt().coerceIn(1, MAX_COLUMNS)

    fun folderColumns(availableWidthDp: Float, size: PhotoSize): Int =
        (availableWidthDp / (size.targetCellDp * FOLDER_SCALE)).roundToInt()
            .coerceIn(1, MAX_FOLDER_COLUMNS)

    fun folderBitmapPx(size: PhotoSize, density: Float): Int =
        (size.targetCellDp * FOLDER_SCALE * density).roundToInt()
            .coerceIn(MIN_BITMAP_PX, MAX_FOLDER_BITMAP_PX)

    fun cellDp(availableWidthDp: Float, columns: Int): Float = availableWidthDp / columns

    fun nameFontSizeSp(size: PhotoSize): Int = when (size) {
        PhotoSize.SMALL -> 10
        PhotoSize.MEDIUM -> 11
        PhotoSize.LARGE -> 13
    }

    /** How many lines of a full name fit on a tile without making it taller than its photo. */
    fun fullNameMaxLines(size: PhotoSize): Int = if (size == PhotoSize.SMALL) 3 else 4

    /** How tall a names list row is, measured on Arabic names, whose lines are taller than Latin ones. */
    fun listRowHeightDp(options: ListOptions, fontScale: Float): Float {
        val name = options.fontSizeSp * fontScale * LIST_NAME_LINE_SCALE
        val number = if (options.showsNumberBelow) {
            options.numberFontSizeSp * fontScale * LIST_NUMBER_LINE_SCALE
        } else {
            0f
        }
        val divider = if (options.showDividers) 1f else 0f
        return name + number + options.spacing.rowPaddingDp * 2 + divider
    }

    /** Roughly how many rows a widget shows at once; one screenful is what a page button moves. */
    fun visibleRows(widthDp: Float, heightDp: Float, size: PhotoSize): Int =
        (heightDp / cellDp(widthDp, columns(widthDp, size))).toInt().coerceAtLeast(1)

    fun visibleListRows(heightDp: Float, options: ListOptions, fontScale: Float): Int =
        (heightDp / listRowHeightDp(options, fontScale)).toInt().coerceAtLeast(1)

    fun overflows(count: Int, widthDp: Float, heightDp: Float, size: PhotoSize): Boolean {
        val columns = columns(widthDp, size)
        val rows = (count + columns - 1) / columns
        return rows * cellDp(widthDp, columns) > heightDp
    }

    fun listOverflows(count: Int, heightDp: Float, options: ListOptions, fontScale: Float): Boolean =
        count * listRowHeightDp(options, fontScale) > heightDp

    /** Widget bitmaps travel over Binder, so they stay small enough to be safe at any count. */
    fun bitmapPx(size: PhotoSize, density: Float): Int =
        (size.targetCellDp * density * BITMAP_OVERSCAN).roundToInt()
            .coerceIn(MIN_BITMAP_PX, MAX_BITMAP_PX)
}
