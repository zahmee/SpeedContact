package com.speedcontact.app.action

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Data
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import androidx.core.net.toUri
import com.speedcontact.app.domain.PhoneNumbers
import java.util.Locale

class ContactIntents(private val context: Context) {

    fun call(number: String): Intent = Intent(Intent.ACTION_CALL, telUri(number))

    fun dial(number: String): Intent = Intent(Intent.ACTION_DIAL, telUri(number))

    fun sms(number: String): Intent =
        Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", PhoneNumbers.toAsciiDigits(number), null))

    fun contactCard(contactId: Long, lookupKey: String): Intent =
        Intent(Intent.ACTION_VIEW, Contacts.getLookupUri(contactId, lookupKey))

    /** Null when WhatsApp is not installed. */
    fun whatsAppChat(number: String): Intent? {
        val packageName = installedWhatsAppPackage() ?: return null
        val digits = whatsAppDigits(number)
        return Intent(Intent.ACTION_VIEW, "https://wa.me/$digits".toUri()).setPackage(packageName)
    }

    /** Null when WhatsApp is not installed or has not synced a calling entry for this contact. */
    fun whatsAppCall(contactId: Long, number: String): Intent? {
        val packageName = installedWhatsAppPackage() ?: return null
        val mimeType = "vnd.android.cursor.item/vnd.$packageName.voip.call"
        val digits = whatsAppDigits(number)

        var firstEntry: Long? = null
        var matchingEntry: Long? = null
        runCatching {
            context.contentResolver.query(
                Data.CONTENT_URI,
                arrayOf(Data._ID, Data.DATA1),
                "${Data.CONTACT_ID} = ? AND ${Data.MIMETYPE} = ?",
                arrayOf(contactId.toString(), mimeType),
                null,
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    if (firstEntry == null) firstEntry = id
                    if (cursor.getString(1)?.startsWith(digits) == true) matchingEntry = id
                }
            }
        }

        val entry = matchingEntry ?: firstEntry ?: return null
        return Intent(Intent.ACTION_VIEW)
            .setDataAndType(ContentUris.withAppendedId(Data.CONTENT_URI, entry), mimeType)
            .setPackage(packageName)
    }

    private fun telUri(number: String): Uri =
        Uri.fromParts("tel", PhoneNumbers.toAsciiDigits(number), null)

    private fun installedWhatsAppPackage(): String? = WHATSAPP_PACKAGES.firstOrNull { packageName ->
        try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun whatsAppDigits(number: String): String {
        val ascii = PhoneNumbers.toAsciiDigits(number)
        val international = PhoneNumberUtils.formatNumberToE164(ascii, countryIso())
        return PhoneNumbers.whatsAppDigits(international ?: ascii)
    }

    private fun countryIso(): String {
        val telephony = context.getSystemService(TelephonyManager::class.java)
        // The phone's own language: one chosen for this app says nothing about the country.
        val phoneCountry = Resources.getSystem().configuration.locales[0].country
        return listOf(telephony?.simCountryIso, telephony?.networkCountryIso, phoneCountry)
            .firstOrNull { !it.isNullOrBlank() }
            .orEmpty()
            .uppercase(Locale.ROOT)
    }

    private companion object {
        val WHATSAPP_PACKAGES = listOf("com.whatsapp", "com.whatsapp.w4b")
    }
}
