package com.chooloo.www.chooloolib.ui.common

import android.accounts.Account
import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.Photo
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.RawContacts
import android.provider.ContactsContract.RawContacts.DefaultAccount.DefaultAccountAndState
import android.util.Log
import androidx.annotation.StringRes
import com.chooloo.www.chooloolib.R
import kotlin.math.max

internal enum class ContactFieldKind(val mimeType: String, val column: String = Data.DATA1) {
    FIRST_NAME(StructuredName.CONTENT_ITEM_TYPE, StructuredName.GIVEN_NAME),
    LAST_NAME(StructuredName.CONTENT_ITEM_TYPE, StructuredName.FAMILY_NAME),
    COMPANY(Organization.CONTENT_ITEM_TYPE),
    PHONE(Phone.CONTENT_ITEM_TYPE),
    EMAIL(Email.CONTENT_ITEM_TYPE),
}

internal data class ContactEditorField(
    val kind: ContactFieldKind,
    val value: String = "",
    val id: Long? = null,
    val version: Long = 0,
    val readOnly: Boolean = false,
    val typeLabel: String? = null,
    // Tells apart several new phones or emails that have no row id yet.
    val slot: Int = 0,
) {
    val key: String get() = "${id ?: "new$slot"}:${kind.name}"
}

internal sealed interface PhotoEdit {
    data class Set(val source: Uri) : PhotoEdit
    data object Remove : PhotoEdit
}

private val PHONE_CHARS = Regex("[0-9+()\\-.*#,;/pPwW ]+")
private val EMAIL = Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")

/** Returns an error string for each invalid field key. Values must already be trimmed. */
internal fun validateContact(
    fields: List<ContactEditorField>,
    values: Map<String, String>,
    creating: Boolean,
): Map<String, Int> = buildMap {
    for (field in fields) {
        val value = values[field.key].orEmpty()
        if (field.readOnly || value.isEmpty()) continue
        when (field.kind) {
            ContactFieldKind.PHONE -> if (!value.matches(PHONE_CHARS) ||
                value.count(Char::isDigit) < 3
            ) {
                put(field.key, R.string.contact_editor_invalid_phone)
            }

            ContactFieldKind.EMAIL -> if (!value.matches(EMAIL)) {
                put(field.key, R.string.contact_editor_invalid_email)
            }

            else -> Unit
        }
    }
    if (creating && values.values.all(String::isEmpty)) {
        fields.firstOrNull { it.kind == ContactFieldKind.FIRST_NAME }
            ?.let { put(it.key, R.string.contact_editor_empty) }
    }
}

/** Splits a full name from another app into first and last name at the first space. */
internal fun splitName(name: String): Pair<String, String> {
    val trimmed = name.trim()
    val space = trimmed.indexOf(' ')
    return if (space < 0) {
        trimmed to ""
    } else {
        trimmed.substring(0, space) to trimmed.substring(space + 1).trim()
    }
}

@get:StringRes
internal val ContactFieldKind.label: Int
    get() = when (this) {
        ContactFieldKind.FIRST_NAME -> R.string.contact_editor_first_name
        ContactFieldKind.LAST_NAME -> R.string.contact_editor_last_name
        ContactFieldKind.COMPANY -> R.string.contact_editor_company
        ContactFieldKind.PHONE -> R.string.new_contact_phone
        ContactFieldKind.EMAIL -> R.string.new_contact_email
    }

internal data class ContactEditorData(
    val fields: List<ContactEditorField>,
    val rawContactId: Long? = null,
    val contactId: Long? = null,
    val photoUri: String? = null,
) {
    /** Saves the changes and returns the contact's lookup URI, when the provider gives one. */
    fun save(context: Context, values: Map<String, String>, photo: PhotoEdit?): Uri? {
        val operations = arrayListOf<ContentProviderOperation>()
        if (contactId == null) {
            operations += ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                .apply {
                    val account = newContactAccount(context)
                    withValue(RawContacts.ACCOUNT_NAME, account?.name)
                    withValue(RawContacts.ACCOUNT_TYPE, account?.type)
                }
                .build()
        } else {
            val rawUri = ContentUris.withAppendedId(
                RawContacts.CONTENT_URI,
                checkNotNull(rawContactId),
            )
            operations += ContentProviderOperation.newAssertQuery(rawUri)
                .withValue(RawContacts.CONTACT_ID, contactId)
                .withValue(RawContacts.DELETED, 0)
                .withExpectedCount(1)
                .build()
        }
        val rows = fields.groupBy { Triple(it.id, it.slot, it.kind.mimeType) }
        for ((row, rowFields) in rows) {
            val (id, _, mimeType) = row
            val changes = ContentValues()
            for (field in rowFields) {
                val value = values[field.key] ?: continue
                if (field.readOnly || (contactId != null && value == field.value)) continue
                if (id != null || value.isNotEmpty()) changes.put(field.kind.column, value)
            }
            if (changes.size() == 0) continue

            val operation = if (id == null) {
                ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .apply {
                        if (rawContactId == null) {
                            withValueBackReference(Data.RAW_CONTACT_ID, 0)
                        } else {
                            withValue(Data.RAW_CONTACT_ID, rawContactId)
                        }
                        if (mimeType == Phone.CONTENT_ITEM_TYPE) {
                            withValue(Phone.TYPE, Phone.TYPE_MOBILE)
                        }
                    }
                    .withValue(Data.MIMETYPE, mimeType)
                    .withValues(changes)
            } else {
                val uri = ContentUris.withAppendedId(Data.CONTENT_URI, id)
                val remove = (
                    mimeType == Phone.CONTENT_ITEM_TYPE ||
                        mimeType == Email.CONTENT_ITEM_TYPE
                    ) &&
                    changes.getAsString(Data.DATA1).isEmpty()
                val change = if (remove) {
                    ContentProviderOperation.newDelete(uri)
                } else {
                    ContentProviderOperation.newUpdate(uri).withValues(changes)
                }
                val version = rowFields.first().version
                change.withSelection("${Data.DATA_VERSION} = ?", arrayOf(version.toString()))
                    .withExpectedCount(1)
            }
            operations += operation.build()
        }
        val resolver = context.contentResolver
        val results = resolver.applyBatch(ContactsContract.AUTHORITY, operations)
        val rawId = rawContactId ?: results.first().uri?.let(ContentUris::parseId) ?: return null
        when (photo) {
            is PhotoEdit.Set -> writePhoto(context, rawId, photo.source)

            PhotoEdit.Remove -> resolver.delete(
                Data.CONTENT_URI,
                "${Data.RAW_CONTACT_ID} = ? AND ${Data.MIMETYPE} = ?",
                arrayOf(rawId.toString(), Photo.CONTENT_ITEM_TYPE),
            )

            null -> Unit
        }
        return RawContacts.getContactLookupUri(
            resolver,
            ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawId),
        )
    }
}

/**
 * Where a new contact goes. Android 16 can refuse contacts with no account when the user's
 * default is a cloud or SIM account, and some phone makers use their own on-device account.
 * Null means the plain local account.
 */
private fun newContactAccount(context: Context): Account? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
        val default = runCatching {
            RawContacts.DefaultAccount.getDefaultAccountForNewContacts(context.contentResolver)
        }.onFailure { Log.w(TAG, "Default contacts account unavailable", it) }.getOrNull()
        when (default?.state) {
            DefaultAccountAndState.DEFAULT_ACCOUNT_STATE_CLOUD,
            DefaultAccountAndState.DEFAULT_ACCOUNT_STATE_SIM,
            -> default.account?.let { return it }

            else -> Unit
        }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val name = RawContacts.getLocalAccountName(context)
        val type = RawContacts.getLocalAccountType(context)
        if (name != null && type != null) return Account(name, type)
    }
    return null
}

private const val TAG = "ContactEditor"
private const val PHOTO_MAX_SIDE = 720

private fun writePhoto(context: Context, rawContactId: Long, source: Uri) {
    val bitmap = ImageDecoder.decodeBitmap(
        ImageDecoder.createSource(context.contentResolver, source),
    ) { decoder, info, _ ->
        val scale = PHOTO_MAX_SIDE.toFloat() / max(info.size.width, info.size.height)
        if (scale < 1f) {
            decoder.setTargetSize(
                (info.size.width * scale).toInt(),
                (info.size.height * scale).toInt(),
            )
        }
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    }
    val target = Uri.withAppendedPath(
        ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId),
        RawContacts.DisplayPhoto.CONTENT_DIRECTORY,
    )
    checkNotNull(context.contentResolver.openAssetFileDescriptor(target, "rw")).use { file ->
        file.createOutputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
    }
}

internal fun loadContactEditor(context: Context, contactId: Long): ContactEditorData {
    val resolver = context.contentResolver
    val rawIds = checkNotNull(
        resolver.query(
            RawContacts.CONTENT_URI,
            arrayOf(RawContacts._ID),
            "${RawContacts.CONTACT_ID} = ? AND ${RawContacts.DELETED} = 0",
            arrayOf(contactId.toString()),
            RawContacts._ID,
        ),
    ).use { cursor ->
        buildSet {
            while (cursor.moveToNext()) {
                add(cursor.getLong(0))
            }
        }
    }
    val fields = checkNotNull(
        resolver.query(
            Data.CONTENT_URI,
            arrayOf(
                Data._ID,
                Data.MIMETYPE,
                Data.DATA1,
                Data.DATA2,
                Data.DATA3,
                Data.DATA_VERSION,
                Data.RAW_CONTACT_ID,
            ),
            "${Data.CONTACT_ID} = ? AND ${Data.MIMETYPE} IN (?, ?, ?, ?)",
            arrayOf(
                contactId.toString(),
                StructuredName.CONTENT_ITEM_TYPE,
                Organization.CONTENT_ITEM_TYPE,
                ContactFieldKind.PHONE.mimeType,
                ContactFieldKind.EMAIL.mimeType,
            ),
            "${Data.IS_SUPER_PRIMARY} DESC, ${Data.IS_PRIMARY} DESC, ${Data._ID}",
        ),
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                val mimeType = cursor.getString(1)
                val value = cursor.getString(2).orEmpty()
                val label = cursor.getString(4)
                val id = cursor.getLong(0)
                val version = cursor.getLong(5)
                val readOnly = cursor.getLong(6) !in rawIds
                if (mimeType == StructuredName.CONTENT_ITEM_TYPE) {
                    val given = cursor.getString(3).orEmpty()
                    val family = label.orEmpty()
                    val first = if (given.isEmpty() && family.isEmpty()) value else given
                    add(
                        ContactEditorField(
                            ContactFieldKind.FIRST_NAME,
                            first,
                            id,
                            version,
                            readOnly,
                        ),
                    )
                    add(
                        ContactEditorField(
                            ContactFieldKind.LAST_NAME,
                            family,
                            id,
                            version,
                            readOnly,
                        ),
                    )
                } else {
                    val kind = ContactFieldKind.entries.first { it.mimeType == mimeType }
                    val type = cursor.getInt(3)
                    val typeLabel = when (kind) {
                        ContactFieldKind.PHONE -> Phone.getTypeLabel(context.resources, type, label)
                        ContactFieldKind.EMAIL -> Email.getTypeLabel(context.resources, type, label)
                        else -> null
                    }
                    add(
                        ContactEditorField(
                            kind,
                            value,
                            id,
                            version,
                            readOnly,
                            typeLabel?.toString(),
                        ),
                    )
                }
            }
        }
    }
    val targetRawId = rawIds.firstOrNull()
    val primaryKinds = listOf(
        ContactFieldKind.FIRST_NAME,
        ContactFieldKind.LAST_NAME,
        ContactFieldKind.COMPANY,
    )
    val identity = primaryKinds.map { kind ->
        val candidates = fields.filter { it.kind == kind }
        candidates.firstOrNull { !it.readOnly } ?: candidates.firstOrNull()
            ?: ContactEditorField(kind, readOnly = targetRawId == null)
    }
    val photoUri = resolver.query(
        ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId),
        arrayOf(Contacts.PHOTO_URI),
        null,
        null,
        null,
    )?.use { if (it.moveToFirst()) it.getString(0) else null }
    return ContactEditorData(
        identity + fields.filter { it.kind !in primaryKinds },
        targetRawId,
        contactId,
        photoUri,
    )
}
