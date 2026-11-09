package com.speedcontact.app.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.ContactsContract.CommonDataKinds.Nickname
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Data
import androidx.core.content.ContextCompat
import com.speedcontact.app.domain.ContactSorter
import com.speedcontact.app.domain.NameParts
import com.speedcontact.app.domain.PhoneNumbers
import com.speedcontact.app.model.ContactSource
import com.speedcontact.app.model.WidgetConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val resolver get() = appContext.contentResolver

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    /** Every contact that has at least one phone number, sorted by name. */
    suspend fun contactsWithPhones(): List<DeviceContact> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyList()

        val builders = LinkedHashMap<Long, DeviceContact>()
        resolver.query(
            Phone.CONTENT_URI,
            PHONE_PROJECTION,
            null,
            null,
            "${Phone.DISPLAY_NAME_PRIMARY} COLLATE LOCALIZED ASC",
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(Phone.CONTACT_ID)
            val lookupColumn = cursor.getColumnIndexOrThrow(Phone.LOOKUP_KEY)
            val nameColumn = cursor.getColumnIndexOrThrow(Phone.DISPLAY_NAME_PRIMARY)
            val thumbnailColumn = cursor.getColumnIndexOrThrow(Phone.PHOTO_THUMBNAIL_URI)
            val starredColumn = cursor.getColumnIndexOrThrow(Phone.STARRED)
            val numberColumn = cursor.getColumnIndexOrThrow(Phone.NUMBER)
            val typeColumn = cursor.getColumnIndexOrThrow(Phone.TYPE)
            val labelColumn = cursor.getColumnIndexOrThrow(Phone.LABEL)
            val primaryColumn = cursor.getColumnIndexOrThrow(Phone.IS_PRIMARY)
            val superPrimaryColumn = cursor.getColumnIndexOrThrow(Phone.IS_SUPER_PRIMARY)

            while (cursor.moveToNext()) {
                val number = cursor.getString(numberColumn)?.takeIf { it.isNotBlank() } ?: continue
                val id = cursor.getLong(idColumn)
                val phone = ContactPhone(
                    number = number,
                    type = cursor.getInt(typeColumn),
                    customLabel = cursor.getString(labelColumn),
                    isPrimary = cursor.getInt(primaryColumn) != 0,
                    isSuperPrimary = cursor.getInt(superPrimaryColumn) != 0,
                )
                val existing = builders[id]
                builders[id] = if (existing == null) {
                    DeviceContact(
                        id = id,
                        lookupKey = cursor.getString(lookupColumn).orEmpty(),
                        displayName = cursor.getString(nameColumn).orEmpty().ifBlank { number },
                        thumbnailUri = cursor.getString(thumbnailColumn),
                        starred = cursor.getInt(starredColumn) != 0,
                        phones = listOf(phone),
                    )
                } else if (existing.phones.none { PhoneNumbers.isSameNumber(it.number, number) }) {
                    existing.copy(phones = existing.phones + phone)
                } else {
                    existing
                }
            }
        }
        builders.values.toList()
    }

    /** The people a widget shows, in their chosen order. Deleted contacts are skipped. */
    suspend fun resolvePeople(
        config: WidgetConfig,
        all: List<DeviceContact>? = null,
    ): List<ResolvedPerson> = withContext(Dispatchers.IO) {
        // A chosen person is looked up by key below, and the provider refuses that without access.
        if (!hasPermission()) return@withContext emptyList()
        val contacts = all ?: contactsWithPhones()
        when (config.source) {
            ContactSource.FAVORITES -> {
                val favorites = contacts
                    .filter { it.starred }
                    .map { ResolvedPerson(it, it.defaultNumber) }
                ContactSorter.applyCustomOrder(favorites, config.favoriteOrder) { it.contact.lookupKey }
            }

            ContactSource.MANUAL -> {
                val byLookup = contacts.associateBy { it.lookupKey }
                val byId = contacts.associateBy { it.id }
                config.contacts.mapNotNull { selected ->
                    val contact = byLookup[selected.lookupKey]
                        ?: currentContactId(selected.lookupKey, selected.contactId)?.let(byId::get)
                        ?: return@mapNotNull null
                    val number = contact.phones
                        .firstOrNull { PhoneNumbers.isSameNumber(it.number, selected.number) }
                        ?.number
                        ?: contact.defaultNumber
                    ResolvedPerson(contact, number)
                }
            }
        // Two chosen contacts become one when merged in the phone's contacts app.
        }.distinctBy { it.contact.id }
    }

    /** Lookup keys can change when contacts are merged; the provider can still resolve them. */
    private fun currentContactId(lookupKey: String, contactId: Long): Long? = try {
        Contacts.lookupContact(resolver, Contacts.getLookupUri(contactId, lookupKey))
            ?.let(ContentUris::parseId)
    } catch (_: IllegalArgumentException) {
        null
    }

    suspend fun nameParts(contactIds: Collection<Long>): Map<Long, NameParts> =
        withContext(Dispatchers.IO) {
            val given = HashMap<Long, String?>()
            val family = HashMap<Long, String?>()
            val display = HashMap<Long, String?>()
            val nickname = HashMap<Long, String?>()

            contactIds.chunked(QUERY_CHUNK_SIZE).forEach { chunk ->
                val ids = chunk.joinToString(",")
                resolver.query(
                    Data.CONTENT_URI,
                    arrayOf(Data.CONTACT_ID, Data.MIMETYPE, Data.DATA1, Data.DATA2, Data.DATA3),
                    "${Data.CONTACT_ID} IN ($ids) AND ${Data.MIMETYPE} IN (?, ?)",
                    arrayOf(StructuredName.CONTENT_ITEM_TYPE, Nickname.CONTENT_ITEM_TYPE),
                    null,
                )?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(0)
                        if (cursor.getString(1) == Nickname.CONTENT_ITEM_TYPE) {
                            nickname.putIfAbsent(id, cursor.getString(2))
                        } else {
                            display.putIfAbsent(id, cursor.getString(2))
                            given.putIfAbsent(id, cursor.getString(3))
                            family.putIfAbsent(id, cursor.getString(4))
                        }
                    }
                }
            }

            contactIds.associateWith { id ->
                NameParts(
                    display = display[id].orEmpty(),
                    given = given[id],
                    family = family[id],
                    nickname = nickname[id],
                )
            }
        }

    /** Loads the contact's largest photo, downsampled close to [targetPx]. */
    suspend fun loadPhoto(contactId: Long, targetPx: Int): Bitmap? = withContext(Dispatchers.IO) {
        val uri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId)
        val bytes = runCatching {
            Contacts.openContactPhotoInputStream(resolver, uri, true)?.use { it.readBytes() }
        }.getOrNull() ?: return@withContext null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sampleSize = 1
        while (minOf(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= targetPx) {
            sampleSize *= 2
        }
        BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sampleSize },
        )
    }

    private companion object {
        const val QUERY_CHUNK_SIZE = 400

        val PHONE_PROJECTION = arrayOf(
            Phone.CONTACT_ID,
            Phone.LOOKUP_KEY,
            Phone.DISPLAY_NAME_PRIMARY,
            Phone.PHOTO_THUMBNAIL_URI,
            Phone.STARRED,
            Phone.NUMBER,
            Phone.TYPE,
            Phone.LABEL,
            Phone.IS_PRIMARY,
            Phone.IS_SUPER_PRIMARY,
        )
    }
}
