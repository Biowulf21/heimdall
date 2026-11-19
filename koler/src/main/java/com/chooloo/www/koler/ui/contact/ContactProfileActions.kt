package com.chooloo.www.koler.ui.contact

import android.Manifest.permission.CALL_PHONE
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract.Contacts
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import android.widget.Toast
import androidx.annotation.StringRes
import com.chooloo.www.chooloolib.R as SharedR
import com.chooloo.www.chooloolib.domain.model.Contact
import com.chooloo.www.koler.R
import com.chooloo.www.koler.ui.main.MainActivity

internal class ContactProfileActions(private val context: Context) {
    private val shortcuts get() = context.getSystemService(ShortcutManager::class.java)
    private val telecom get() = context.getSystemService(TelecomManager::class.java)

    val canPinShortcut: Boolean get() = shortcuts?.isRequestPinShortcutSupported == true
    val canVideoCall: Boolean
        get() = context.checkSelfPermission(CALL_PHONE) == PERMISSION_GRANTED &&
            videoAccounts().isNotEmpty()

    fun ringtonePicker(current: String?) = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
        .putExtra(
            RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
            when (current) {
                null -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                "" -> null
                else -> Uri.parse(current)
            },
        )

    fun ringtoneLabel(current: String?): String = when (current) {
        null -> context.getString(R.string.contact_ringtone_default)

        "" -> context.getString(R.string.contact_ringtone_silent)

        else -> runCatching {
            RingtoneManager.getRingtone(context, Uri.parse(current))?.getTitle(context)
        }.getOrNull() ?: context.getString(R.string.contact_ringtone_custom)
    }

    fun share(contact: Contact) {
        val key = contact.lookupKey ?: return
        val vcard = Uri.withAppendedPath(Contacts.CONTENT_VCARD_URI, key)
        val send = Intent(Intent.ACTION_SEND)
            .setType(Contacts.CONTENT_VCARD_TYPE)
            .putExtra(Intent.EXTRA_STREAM, vcard)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .apply {
                clipData = ClipData.newRawUri(context.getString(R.string.contact_share), vcard)
            }
        perform { context.startActivity(Intent.createChooser(send, null)) }
    }

    fun pinShortcut(contact: Contact, label: String) {
        val key = contact.lookupKey ?: return
        val manager = shortcuts ?: return
        val openContact = Intent(context, MainActivity::class.java)
            .setAction(ContactProfileActivity.OPEN_SHORTCUT)
            .putExtra(ContactProfileActivity.CONTACT_ID, contact.id)
            .putExtra(ContactProfileActivity.LOOKUP_KEY, key)
        val shortcut = ShortcutInfo.Builder(context, "contact:$key")
            .setShortLabel(label)
            .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(openContact)
            .build()
        perform(R.string.contact_shortcut_failed) {
            if (!manager.requestPinShortcut(
                    shortcut,
                    null,
                )
            ) {
                notify(R.string.contact_shortcut_failed)
            }
        }
    }

    fun videoCall(number: String) = perform(SharedR.string.error_call_failed) {
        val manager = telecom
        val accounts = videoAccounts()
        if (manager == null || accounts.isEmpty()) {
            notify(SharedR.string.error_call_failed)
            return@perform
        }
        val preferred = manager.getDefaultOutgoingPhoneAccount(PhoneAccount.SCHEME_TEL)
            ?.takeIf(accounts::contains) ?: accounts.singleOrNull()
        val options = Bundle().apply {
            putInt(
                TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE,
                VideoProfile.STATE_BIDIRECTIONAL,
            )
            preferred?.let { putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, it) }
        }
        manager.placeCall(Uri.fromParts(PhoneAccount.SCHEME_TEL, number, null), options)
    }

    fun perform(@StringRes error: Int = SharedR.string.error_loading_failed, action: () -> Unit) {
        try {
            action()
        } catch (_: ActivityNotFoundException) {
            notify(SharedR.string.error_no_app_found)
        } catch (_: SecurityException) {
            notify(error)
        } catch (_: IllegalStateException) {
            notify(error)
        }
    }

    private fun videoAccounts(): List<PhoneAccountHandle> {
        val manager = telecom ?: return emptyList()
        return try {
            manager.callCapablePhoneAccounts.filter {
                manager.getPhoneAccount(
                    it,
                )?.hasCapabilities(PhoneAccount.CAPABILITY_VIDEO_CALLING) ==
                    true
            }
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    private fun notify(@StringRes message: Int) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
