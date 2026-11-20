package com.chooloo.www.chooloolib.ui.common

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.RawContacts

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
) {
    val key: String get() = "${id ?: 0}:${kind.name}"
}

internal data class ContactEditorData(
    val fields: List<ContactEditorField>,
    val rawContactId: Long? = null,
    val contactId: Long? = null,
) {
    fun save(context: Context, values: Map<String, String>) {
        val operations = arrayListOf<ContentProviderOperation>()
        if (contactId == null) {
            operations += ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                .withValue(RawContacts.ACCOUNT_NAME, null)
                .withValue(RawContacts.ACCOUNT_TYPE, null)
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
        val rows = fields.groupBy { it.id to it.kind.mimeType }
        for ((row, rowFields) in rows) {
            val (id, mimeType) = row
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
        context.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
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
    return ContactEditorData(
        identity + fields.filter { it.kind !in primaryKinds },
        targetRawId,
        contactId,
    )
}
