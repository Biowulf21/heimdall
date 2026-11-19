package com.chooloo.www.koler.ui.contact

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract.Contacts
import com.chooloo.www.chooloolib.ui.caller.CallerTarget
import com.chooloo.www.chooloolib.ui.common.BaseActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ContactProfileActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val target = intent.data?.let { CallerTarget.LinkTarget(it.toString()) }
            ?: intent.getLongExtra(CONTACT_ID, 0).takeIf {
                it > 0
            }?.let(CallerTarget::ContactTarget)
        if (target == null) {
            finish()
            return
        }

        setChoolooContent(heimdall = true) {
            ContactProfileScreen(target = target, onBack = ::finish)
        }
    }

    companion object {
        internal const val OPEN_SHORTCUT = "io.github.biowulf21.heimdall.action.OPEN_CONTACT"
        internal const val CONTACT_ID = "contact_id"
        internal const val LOOKUP_KEY = "lookup_key"

        fun intent(context: Context, contactId: Long): Intent =
            Intent(context, ContactProfileActivity::class.java).putExtra(CONTACT_ID, contactId)

        fun fromShortcut(context: Context, shortcut: Intent): Intent? {
            val id = shortcut.getLongExtra(CONTACT_ID, 0)
            val key = shortcut.getStringExtra(LOOKUP_KEY)?.takeIf(String::isNotBlank)
            if (id <= 0 || key == null || '/' in key || '?' in key || '#' in key) return null
            val uri = Contacts.getLookupUri(id, key) ?: return null
            return Intent(context, ContactProfileActivity::class.java).setData(uri)
        }
    }
}
