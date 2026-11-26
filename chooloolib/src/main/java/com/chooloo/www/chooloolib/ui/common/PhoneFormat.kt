package com.chooloo.www.chooloolib.ui.common

import android.content.Context
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

private val BRACKETS = Regex("[()]")
private val SEPARATORS = Regex("[\\s\\-.]+")

/** Joins the groups of an already formatted number with dashes: "+1 (555) 123-4567" → "+1-555-123-4567". */
internal fun dashGroups(formatted: String): String =
    formatted.replace(BRACKETS, "").trim().replace(SEPARATORS, "-")

/**
 * Formats a number for display only, grouped for [countryIso] and joined by dashes.
 * Service codes, partial input, and anything the platform can't parse stay as typed.
 * Never dial or store the result: keep the raw number for that.
 */
fun formatPhoneNumber(number: String, countryIso: String): String {
    if (number.any { it.isLetter() || it in "*#,;" }) return number
    val formatted = PhoneNumberUtils.formatNumber(number, countryIso) ?: return number
    return dashGroups(formatted)
}

/** The country to format local numbers for: the network's, then the SIM's, then the locale's. */
fun phoneCountryIso(context: Context): String {
    val telephony = context.getSystemService(TelephonyManager::class.java)
    return listOfNotNull(telephony?.networkCountryIso, telephony?.simCountryIso)
        .firstOrNull(String::isNotEmpty)
        ?.uppercase(Locale.ROOT)
        ?: Locale.getDefault().country
}

fun formatPhoneNumber(context: Context, number: String): String =
    formatPhoneNumber(number, phoneCountryIso(context))

@Composable
fun rememberPhoneFormatter(): (String) -> String {
    val context = LocalContext.current
    return remember(context) {
        val iso = phoneCountryIso(context)
        val format: (String) -> String = { formatPhoneNumber(it, iso) }
        format
    }
}
