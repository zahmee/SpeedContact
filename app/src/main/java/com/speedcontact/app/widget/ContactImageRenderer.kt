package com.speedcontact.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.speedcontact.app.R
import com.speedcontact.app.model.PhotoShape
import com.speedcontact.app.model.PlaceholderStyle

/**
 * Bakes the final tile image. Shape and the name scrim are drawn into the bitmap because widgets
 * cannot clip images to a circle on every supported Android version.
 */
class ContactImageRenderer(private val context: Context) {

    fun render(
        photo: Bitmap?,
        shape: PhotoShape,
        placeholder: PlaceholderStyle,
        sizePx: Int,
        withScrim: Boolean,
        withPersonIcon: Boolean = true,
    ): Bitmap {
        val square = createBitmap(sizePx, sizePx)
        val canvas = Canvas(square)
        if (photo != null) {
            drawCenterCropped(canvas, photo, sizePx)
        } else {
            drawPlaceholder(canvas, placeholder, sizePx, withPersonIcon)
        }
        if (withScrim) drawScrim(canvas, sizePx)
        return if (shape == PhotoShape.SQUARE) square else clip(square, shape, sizePx)
    }

    private fun drawCenterCropped(canvas: Canvas, photo: Bitmap, sizePx: Int) {
        val scale = sizePx.toFloat() / minOf(photo.width, photo.height)
        val matrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate(
                (sizePx - photo.width * scale) / 2f,
                (sizePx - photo.height * scale) / 2f,
            )
        }
        canvas.drawBitmap(photo, matrix, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
    }

    private fun drawPlaceholder(
        canvas: Canvas,
        style: PlaceholderStyle,
        sizePx: Int,
        withPersonIcon: Boolean,
    ) {
        val (background, foreground) = when (style) {
            PlaceholderStyle.LIGHT -> Color.parseColor("#EDF1F7") to Color.parseColor("#9AA8BC")
            PlaceholderStyle.DARK -> Color.parseColor("#2A2E36") to Color.parseColor("#8C96A6")
        }
        canvas.drawColor(background)
        // A full name written on the tile takes the icon's place, or the two would be unreadable.
        if (!withPersonIcon) return
        ContextCompat.getDrawable(context, R.drawable.ic_person)?.mutate()?.apply {
            setTint(foreground)
            // Sized so the shoulders run off the bottom edge, like a portrait photo.
            val bleed = sizePx / 40
            val top = sizePx / 8
            setBounds(-bleed, top, sizePx + bleed, top + sizePx + 2 * bleed)
            draw(canvas)
        }
    }

    private fun drawScrim(canvas: Canvas, sizePx: Int) {
        val size = sizePx.toFloat()
        val paint = Paint().apply {
            shader = LinearGradient(
                0f,
                size * SCRIM_START,
                0f,
                size,
                Color.TRANSPARENT,
                Color.argb(190, 0, 0, 0),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, size * SCRIM_START, size, size, paint)
    }

    private fun clip(source: Bitmap, shape: PhotoShape, sizePx: Int): Bitmap {
        val output = createBitmap(sizePx, sizePx)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        val size = sizePx.toFloat()
        val canvas = Canvas(output)
        when (shape) {
            PhotoShape.CIRCLE -> canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
            else -> {
                val radius = size * ROUNDED_RADIUS
                canvas.drawRoundRect(RectF(0f, 0f, size, size), radius, radius, paint)
            }
        }
        source.recycle()
        return output
    }

    private companion object {
        const val SCRIM_START = 0.5f
        const val ROUNDED_RADIUS = 0.12f
    }
}
