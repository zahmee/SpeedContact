package com.speedcontact.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.speedcontact.app.model.PhotoShape
import com.speedcontact.app.model.PlaceholderStyle
import com.speedcontact.app.widget.ContactImageRenderer
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Folder icons chosen from the phone's gallery. Access to a picked picture does not last, so a
 * small round copy is kept in the app's own storage and the home screen never needs the original.
 */
class FolderIconStore(context: Context) {
    private val appContext = context.applicationContext
    private val directory = File(appContext.filesDir, "folder_icons")
    private val renderer = ContactImageRenderer(appContext)

    /** Returns the saved icon's name, or null when the picture could not be read. */
    suspend fun import(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val photo = decode(uri) ?: return@runCatching null
            val icon = renderer.render(
                photo = photo,
                shape = PhotoShape.CIRCLE,
                placeholder = PlaceholderStyle.LIGHT,
                sizePx = ICON_SIZE_PX,
                withScrim = false,
            )
            directory.mkdirs()
            val name = "${UUID.randomUUID()}.png"
            File(directory, name).outputStream().use { icon.compress(Bitmap.CompressFormat.PNG, 100, it) }
            name
        }.getOrNull()
    }

    fun load(name: String): Bitmap? = BitmapFactory.decodeFile(File(directory, name).path)

    fun deleteUnused(used: Set<String>) {
        directory.listFiles()?.filterNot { it.name in used }?.forEach { it.delete() }
    }

    /** The newer decoder also turns a photo the way the camera held it; older phones do not. */
    private fun decode(uri: Uri): Bitmap? {
        val resolver = appContext.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
                // The icon is drawn onto a plain canvas, which cannot read hardware bitmaps.
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.setTargetSampleSize(sampleSize(info.size.width, info.size.height))
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
        }
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    private fun sampleSize(width: Int, height: Int): Int =
        (minOf(width, height) / ICON_SIZE_PX).coerceAtLeast(1)

    private companion object {
        /** Larger than a home screen icon on any phone, and small enough for a widget update. */
        const val ICON_SIZE_PX = 240
    }
}
