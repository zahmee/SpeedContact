package com.speedcontact.app.domain

import com.speedcontact.app.model.ListOptions
import com.speedcontact.app.model.NumberPlacement
import com.speedcontact.app.model.PhotoSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GridMetricsTest {
    private val fourCellWidth = 320f

    @Test
    fun `a four cell wide widget matches the video's column counts`() {
        assertEquals(5, GridMetrics.columns(fourCellWidth, PhotoSize.SMALL))
        assertEquals(4, GridMetrics.columns(fourCellWidth, PhotoSize.MEDIUM))
        assertEquals(3, GridMetrics.columns(fourCellWidth, PhotoSize.LARGE))
    }

    @Test
    fun `columns stay within what the widget grid supports`() {
        assertEquals(1, GridMetrics.columns(20f, PhotoSize.LARGE))
        assertEquals(5, GridMetrics.columns(2000f, PhotoSize.SMALL))
    }

    @Test
    fun `a folder on a phone shows larger photos than the widget`() {
        val phoneWidth = 411f
        assertEquals(5, GridMetrics.folderColumns(phoneWidth, PhotoSize.SMALL))
        assertEquals(4, GridMetrics.folderColumns(phoneWidth, PhotoSize.MEDIUM))
        assertEquals(3, GridMetrics.folderColumns(phoneWidth, PhotoSize.LARGE))
    }

    @Test
    fun `folder photos stay sharp without using too much memory`() {
        assertEquals(720, GridMetrics.folderBitmapPx(PhotoSize.LARGE, density = 8f))
        assertTrue(
            GridMetrics.folderBitmapPx(PhotoSize.LARGE, density = 3f) >
                GridMetrics.bitmapPx(PhotoSize.LARGE, density = 3f),
        )
    }

    @Test
    fun `a grid overflows only when its rows are taller than the widget`() {
        // Four columns of 80 dp, so three rows need 240 dp.
        assertFalse(GridMetrics.overflows(12, fourCellWidth, 240f, PhotoSize.MEDIUM))
        assertTrue(GridMetrics.overflows(13, fourCellWidth, 240f, PhotoSize.MEDIUM))
    }

    @Test
    fun `a page of a grid is the rows that fit`() {
        assertEquals(3, GridMetrics.visibleRows(fourCellWidth, 240f, PhotoSize.MEDIUM))
        assertEquals(1, GridMetrics.visibleRows(fourCellWidth, 10f, PhotoSize.MEDIUM))
    }

    @Test
    fun `a names list overflows when its rows are taller than the widget`() {
        val options = ListOptions()
        val row = GridMetrics.listRowHeightDp(options, fontScale = 1f)
        assertFalse(GridMetrics.listOverflows(5, row * 5, options, fontScale = 1f))
        assertTrue(GridMetrics.listOverflows(6, row * 5, options, fontScale = 1f))
    }

    @Test
    fun `larger text and numbers make rows taller, so fewer fit`() {
        val height = 300f
        val plain = GridMetrics.visibleListRows(height, ListOptions(), fontScale = 1f)
        val larger = GridMetrics.visibleListRows(height, ListOptions(fontSizeSp = 30), fontScale = 1f)
        val withNumbers = GridMetrics.visibleListRows(height, ListOptions(showNumber = true), fontScale = 1f)
        assertTrue(larger < plain)
        assertTrue(withNumbers < plain)
        assertEquals(1, GridMetrics.visibleListRows(1f, ListOptions(), fontScale = 1f))
    }

    @Test
    fun `a number beside the name adds no height, unlike one below it`() {
        val plain = GridMetrics.listRowHeightDp(ListOptions(), fontScale = 1f)
        val beside = ListOptions(showNumber = true, numberPlacement = NumberPlacement.BESIDE)
        val below = ListOptions(showNumber = true, numberPlacement = NumberPlacement.BELOW)
        assertEquals(plain, GridMetrics.listRowHeightDp(beside, fontScale = 1f))
        assertTrue(GridMetrics.listRowHeightDp(below, fontScale = 1f) > plain)
    }

    @Test
    fun `bitmaps never grow beyond the safe limit`() {
        assertEquals(320, GridMetrics.bitmapPx(PhotoSize.LARGE, density = 4f))
        assertTrue(GridMetrics.bitmapPx(PhotoSize.SMALL, density = 1f) >= 96)
    }
}
