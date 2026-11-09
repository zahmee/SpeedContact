package com.speedcontact.app

import android.Manifest
import android.content.ContentProviderOperation
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Nickname
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.Photo
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.RawContacts
import androidx.core.graphics.createBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayOutputStream
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Fills an emulator with fictional contacts for manual checks of the widget. It refuses to run on
 * a real phone, and only runs when asked for with the instrumentation argument seed=true.
 */
@RunWith(AndroidJUnit4::class)
class SeedTestContacts {
    private data class Person(
        val given: String,
        val family: String,
        val numbers: List<Pair<String, Int>>,
        val starred: Boolean = false,
        val nickname: String? = null,
        val photoColors: Pair<String, String>? = null,
        val latin: Pair<String, String>,
    )

    private val people = listOf(
        Person("أحمد", "سالم", listOf("+15550100001" to Phone.TYPE_MOBILE), starred = true, photoColors = "#1E88E5" to "#0D47A1", latin = "Adam" to "Reed"),
        Person("سارة", "خالد", listOf("+15550100002" to Phone.TYPE_MOBILE, "+15550100012" to Phone.TYPE_WORK), starred = true, photoColors = "#EC407A" to "#880E4F", latin = "Sofia" to "Lopez"),
        Person("محمد", "علي", listOf("+15550100003" to Phone.TYPE_MOBILE), nickname = "أبو علي", photoColors = "#26A69A" to "#004D40", latin = "Mateo" to "Costa"),
        Person("نورة", "فهد", listOf("+15550100004" to Phone.TYPE_MOBILE), starred = true, latin = "Nora" to "Hill"),
        Person("Layla", "Omar", listOf("+15550100005" to Phone.TYPE_MOBILE), photoColors = "#FFA726" to "#E65100", latin = "Layla" to "Omar"),
        Person("خالد", "ناصر", listOf("+15550100006" to Phone.TYPE_MOBILE), photoColors = "#7E57C2" to "#311B92", latin = "Kemal" to "Demir"),
        Person("ريم", "يوسف", listOf("٠٥٥٠١٠٠٠٧" to Phone.TYPE_MOBILE), photoColors = "#66BB6A" to "#1B5E20", latin = "Rina" to "Kusuma"),
        Person("Omar", "Zaid", listOf("+15550100008" to Phone.TYPE_MOBILE), latin = "Omar" to "Zaid"),
        Person("فاطمة", "أحمد", listOf("+15550100009" to Phone.TYPE_MOBILE), starred = true, photoColors = "#EF5350" to "#B71C1C", latin = "Fatima" to "Khan"),
        Person("يوسف", "عمر", listOf("+15550100010" to Phone.TYPE_MOBILE), photoColors = "#42A5F5" to "#01579B", latin = "Yusuf" to "Aziz"),
        Person("هند", "سعيد", listOf("+15550100011" to Phone.TYPE_MOBILE), photoColors = "#AB47BC" to "#4A148C", latin = "Priya" to "Sharma"),
        Person("علي", "حسن", listOf("+15550100013" to Phone.TYPE_MOBILE), photoColors = "#8D6E63" to "#3E2723", latin = "Lucas" to "Silva"),
        Person("عبدالرحمن محمد بن عبدالعزيز", "الشمري", listOf("+15550100014" to Phone.TYPE_MOBILE), latin = "Alexander Montgomery" to "Whitfield"),
    )

    /**
     * Store screenshots for the languages in Latin script need names and photo letters to match.
     * Only runs when asked for with the instrumentation argument names=latin, or names=arabic to
     * put the seeded names back. People are matched by family name.
     */
    @Test
    fun rename() {
        val target = InstrumentationRegistry.getArguments().getString("names")
        assumeTrue("Pass names=latin or names=arabic to run", target == "latin" || target == "arabic")
        assumeTrue("Emulators only", Build.HARDWARE == "ranchu" || Build.HARDWARE == "goldfish")

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.uiAutomation
        automation.adoptShellPermissionIdentity(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
        )
        try {
            val resolver = instrumentation.targetContext.contentResolver
            val toLatin = target == "latin"
            val found = mutableListOf<Pair<Long, Person>>()
            resolver.query(
                Data.CONTENT_URI,
                arrayOf(Data.RAW_CONTACT_ID, StructuredName.FAMILY_NAME),
                "${Data.MIMETYPE}=?",
                arrayOf(StructuredName.CONTENT_ITEM_TYPE),
                null,
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val family = cursor.getString(1)
                    val person = people.firstOrNull { (if (toLatin) it.family else it.latin.second) == family }
                    if (person != null) found += cursor.getLong(0) to person
                }
            }
            found.forEach { (rawContactId, person) ->
                val (given, family) = if (toLatin) person.latin else person.given to person.family
                val where = "${Data.RAW_CONTACT_ID}=? AND ${Data.MIMETYPE}=?"
                resolver.update(
                    Data.CONTENT_URI,
                    ContentValues().apply {
                        put(StructuredName.GIVEN_NAME, given)
                        putNull(StructuredName.MIDDLE_NAME)
                        put(StructuredName.FAMILY_NAME, family)
                    },
                    where,
                    arrayOf(rawContactId.toString(), StructuredName.CONTENT_ITEM_TYPE),
                )
                person.photoColors?.let { colors ->
                    resolver.update(
                        Data.CONTENT_URI,
                        ContentValues().apply { put(Photo.PHOTO, photoBytes(given, colors)) },
                        where,
                        arrayOf(rawContactId.toString(), Photo.CONTENT_ITEM_TYPE),
                    )
                }
            }
        } finally {
            automation.dropShellPermissionIdentity()
        }
    }

    @Test
    fun seed() {
        val arguments = InstrumentationRegistry.getArguments()
        assumeTrue("Pass seed=true to run", arguments.getString("seed") == "true")
        assumeTrue("Emulators only", Build.HARDWARE == "ranchu" || Build.HARDWARE == "goldfish")

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.uiAutomation
        automation.adoptShellPermissionIdentity(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
        )
        try {
            val resolver = instrumentation.targetContext.contentResolver
            people.forEach { person ->
                resolver.applyBatch(ContactsContract.AUTHORITY, operationsFor(person))
            }
        } finally {
            automation.dropShellPermissionIdentity()
        }
    }

    private fun operationsFor(person: Person): ArrayList<ContentProviderOperation> {
        val operations = ArrayList<ContentProviderOperation>()
        operations += ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
            .withValue(RawContacts.ACCOUNT_TYPE, null)
            .withValue(RawContacts.ACCOUNT_NAME, null)
            .withValue(RawContacts.STARRED, if (person.starred) 1 else 0)
            .build()
        operations += dataRow(StructuredName.CONTENT_ITEM_TYPE)
            .withValue(StructuredName.GIVEN_NAME, person.given)
            .withValue(StructuredName.FAMILY_NAME, person.family)
            .build()
        person.numbers.forEach { (number, type) ->
            operations += dataRow(Phone.CONTENT_ITEM_TYPE)
                .withValue(Phone.NUMBER, number)
                .withValue(Phone.TYPE, type)
                .build()
        }
        person.nickname?.let { nickname ->
            operations += dataRow(Nickname.CONTENT_ITEM_TYPE)
                .withValue(Nickname.NAME, nickname)
                .build()
        }
        person.photoColors?.let { colors ->
            operations += dataRow(Photo.CONTENT_ITEM_TYPE)
                .withValue(Photo.PHOTO, photoBytes(person.given, colors))
                .build()
        }
        return operations
    }

    private fun dataRow(mimeType: String): ContentProviderOperation.Builder =
        ContentProviderOperation.newInsert(Data.CONTENT_URI)
            .withValueBackReference(Data.RAW_CONTACT_ID, 0)
            .withValue(Data.MIMETYPE, mimeType)

    private fun photoBytes(name: String, colors: Pair<String, String>): ByteArray {
        val size = 480f
        val bitmap = createBitmap(size.toInt(), size.toInt())
        val canvas = Canvas(bitmap)
        canvas.drawPaint(
            Paint().apply {
                shader = LinearGradient(
                    0f,
                    0f,
                    size,
                    size,
                    Color.parseColor(colors.first),
                    Color.parseColor(colors.second),
                    Shader.TileMode.CLAMP,
                )
            },
        )
        val letter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = size * 0.5f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText(name.take(1), size / 2f, size * 0.62f, letter)

        return ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            stream.toByteArray()
        }
    }
}
