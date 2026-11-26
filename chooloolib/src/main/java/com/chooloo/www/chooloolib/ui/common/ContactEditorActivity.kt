package com.chooloo.www.chooloolib.ui.common

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.Intents.Insert
import androidx.core.content.IntentCompat
import dagger.hilt.android.AndroidEntryPoint

/** Lets other apps (Telegram, WhatsApp, messaging, browsers) add a contact through Heimdall. */
@AndroidEntryPoint
class ContactEditorActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefill = prefillFrom(intent)
        setChoolooContent(heimdall = true) {
            ContactEditorDialog(contactId = null, prefill = prefill, onClose = ::finishWith)
        }
    }

    private fun finishWith(saved: Uri?) {
        if (saved == null) {
            setResult(Activity.RESULT_CANCELED)
        } else {
            setResult(Activity.RESULT_OK, Intent().setData(saved.takeUnless { it == Uri.EMPTY }))
        }
        finish()
    }
}

internal fun prefillFrom(intent: Intent): Map<ContactFieldKind, List<String>> {
    fun text(key: String) = intent.getCharSequenceExtra(key)?.toString()?.trim()
        ?.takeIf(String::isNotEmpty)

    // Telegram and other vCard senders put details in DATA rows instead of the simple extras.
    val rows = IntentCompat.getParcelableArrayListExtra(
        intent,
        Insert.DATA,
        ContentValues::class.java,
    ).orEmpty()
    fun values(mimeType: String) = rows
        .filter { it.getAsString(Data.MIMETYPE) == mimeType }
        .mapNotNull { it.getAsString(Data.DATA1)?.trim()?.takeIf(String::isNotEmpty) }
    val structured = rows.firstOrNull {
        it.getAsString(Data.MIMETYPE) == StructuredName.CONTENT_ITEM_TYPE
    }
    val (first, last) = text(Insert.NAME)?.let(::splitName)
        ?: (
            structured?.getAsString(StructuredName.GIVEN_NAME).orEmpty().trim() to
                structured?.getAsString(StructuredName.FAMILY_NAME).orEmpty().trim()
            )
    val phones = listOfNotNull(
        text(Insert.PHONE),
        text(Insert.SECONDARY_PHONE),
        text(Insert.TERTIARY_PHONE),
    ) + values(Phone.CONTENT_ITEM_TYPE)
    val emails = listOfNotNull(
        text(Insert.EMAIL),
        text(Insert.SECONDARY_EMAIL),
        text(Insert.TERTIARY_EMAIL),
    ) + values(Email.CONTENT_ITEM_TYPE)
    val companies = listOfNotNull(text(Insert.COMPANY)) + values(Organization.CONTENT_ITEM_TYPE)
    return mapOf(
        ContactFieldKind.FIRST_NAME to listOf(first),
        ContactFieldKind.LAST_NAME to listOf(last),
        ContactFieldKind.COMPANY to companies.distinct(),
        ContactFieldKind.PHONE to phones.distinct(),
        ContactFieldKind.EMAIL to emails.distinct(),
    )
}
