package com.chooloo.www.chooloolib.ui.common

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.PhoneNumberUtils
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.chooloo.www.chooloolib.R

@Composable
fun rememberExternalActions(): ExternalActions {
    val context = LocalContext.current
    var newContactNumber by rememberSaveable { mutableStateOf<String?>(null) }
    var editingContactId by rememberSaveable { mutableStateOf<Long?>(null) }
    if (newContactNumber != null || editingContactId != null) {
        ContactEditorDialog(newContactNumber.orEmpty(), editingContactId) {
            newContactNumber = null
            editingContactId = null
        }
    }
    return remember(context) {
        ExternalActions(context, { newContactNumber = it }, { editingContactId = it })
    }
}

class ExternalActions(
    private val context: Context,
    private val onCreateContact: (String) -> Unit,
    private val onEditContact: (Long) -> Unit,
) {
    fun openUrl(url: String) = launch(Intent(Intent.ACTION_VIEW, url.toUri()))

    fun openStoreListing() = openUrl("market://details?id=${context.packageName}")

    fun sendSupportEmail() = launch(
        Intent(Intent.ACTION_SENDTO, "mailto:".toUri())
            .putExtra(Intent.EXTRA_EMAIL, arrayOf(context.getString(R.string.support_email))),
    )

    fun sendEmail(address: String) =
        launch(Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", address, null)))

    fun openSms(number: String) = launch(
        Intent(Intent.ACTION_SENDTO, "smsto:${PhoneNumberUtils.normalizeNumber(number)}".toUri()),
    )

    fun openWhatsapp(number: String) = openUrl("https://api.whatsapp.com/send?phone=$number")

    fun openManageBlockedNumbers() = launch(
        context.getSystemService(TelecomManager::class.java).createManageBlockedNumbersIntent(),
    )

    fun createContact(number: String? = null) = onCreateContact(number.orEmpty())

    fun editContact(contactId: Long) = onEditContact(contactId)

    fun copyToClipboard(
        text: String,
        @StringRes feedback: Int = R.string.number_copied_to_clipboard,
    ) {
        context.getSystemService(ClipboardManager::class.java)
            .setPrimaryClip(ClipData.newPlainText(text, text))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, feedback, Toast.LENGTH_SHORT).show()
        }
    }

    private fun launch(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, R.string.error_no_app_found, Toast.LENGTH_SHORT).show()
        } catch (_: SecurityException) {
            Toast.makeText(context, R.string.error_loading_failed, Toast.LENGTH_SHORT).show()
        }
    }
}
