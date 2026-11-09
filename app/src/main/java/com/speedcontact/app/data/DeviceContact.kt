package com.speedcontact.app.data

import android.provider.ContactsContract.CommonDataKinds.Phone

data class ContactPhone(
    val number: String,
    val type: Int,
    val customLabel: String?,
    val isPrimary: Boolean,
    val isSuperPrimary: Boolean,
)

data class DeviceContact(
    val id: Long,
    val lookupKey: String,
    val displayName: String,
    val thumbnailUri: String?,
    val starred: Boolean,
    val phones: List<ContactPhone>,
) {
    /** The number to use when the user has not picked one: their default, then a mobile. */
    val defaultNumber: String
        get() = (
            phones.firstOrNull { it.isSuperPrimary }
                ?: phones.firstOrNull { it.isPrimary }
                ?: phones.firstOrNull { it.type == Phone.TYPE_MOBILE }
                ?: phones.first()
            ).number
}

/** A contact as it will appear in a widget, with the number a tap will use. */
data class ResolvedPerson(
    val contact: DeviceContact,
    val number: String,
)
