package com.chooloo.www.chooloolib.ui.common

import android.content.Context
import android.telephony.PhoneNumberFormattingTextWatcher
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import android.text.SpannableStringBuilder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.util.Locale

/**
 * Groups a number the way the country writes it ("0956 921 2428", "+63 956 921 2428"),
 * including partly typed numbers. Service codes and anything with letters stay as typed.
 * Display only: dial and store the raw number.
 */
fun formatPhoneNumber(number: String, countryIso: String): String {
    if (number.isEmpty() || number.any { it.isLetter() || it in "*#,;" }) return number
    // The platform's as-you-type formatter, applied once to the whole number.
    val text = SpannableStringBuilder(number)
    return runCatching {
        PhoneNumberFormattingTextWatcher(countryIso).afterTextChanged(text)
        text.toString()
    }.getOrNull()?.takeIf { sameDialChars(it, number) } ?: number
}

/** No phone number anywhere has more than 15 digits (ITU E.164). */
const val MAX_PHONE_DIGITS = 15

/** Drops digits past [MAX_PHONE_DIGITS], keeping everything else as typed. */
fun capPhoneDigits(text: String): String {
    var digits = 0
    return text.filter { !it.isDigit() || ++digits <= MAX_PHONE_DIGITS }
}

/**
 * Whether [number] is a complete, valid number for [countryIso] or in international form.
 * Service codes (* and #) count as complete. Used only for a gentle hint, never to block.
 */
fun looksLikeCompleteNumber(number: String, countryIso: String): Boolean =
    number.any { it in "*#" } ||
        runCatching { PhoneNumberUtils.formatNumberToE164(number, countryIso) != null }
            .getOrDefault(true)

private fun dialChars(text: String) = text.filter { it.isDigit() || it in "+*#" }

private fun sameDialChars(a: String, b: String) = dialChars(a) == dialChars(b)

/**
 * Shows a phone field grouped as the user types while keeping the typed text unchanged.
 * Cursor positions map across the added or removed spaces by counting dialable characters.
 */
class PhoneVisualTransformation(private val format: (String) -> String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val shown = format(raw)
        if (shown == raw || !sameDialChars(shown, raw)) {
            return TransformedText(text, OffsetMapping.Identity)
        }
        return TransformedText(AnnotatedString(shown), DialCharMapping(raw, shown))
    }

    private class DialCharMapping(private val raw: String, private val shown: String) :
        OffsetMapping {
        override fun originalToTransformed(offset: Int) = map(raw, offset, shown)
        override fun transformedToOriginal(offset: Int) = map(shown, offset, raw)

        /** The position in [to] just after as many dialable characters as [from] has before [offset]. */
        private fun map(from: String, offset: Int, to: String): Int {
            val count = from.take(offset).count { it.isDigit() || it in "+*#" }
            if (count == 0) return 0
            var seen = 0
            to.forEachIndexed { i, c ->
                if (c.isDigit() || c in "+*#") seen++
                if (seen == count) return i + 1
            }
            return to.length
        }
    }
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
fun rememberPhoneCountryIso(): String {
    val context = LocalContext.current
    return remember(context) { phoneCountryIso(context) }
}

@Composable
fun rememberPhoneFormatter(): (String) -> String {
    val context = LocalContext.current
    return remember(context) {
        val iso = phoneCountryIso(context)
        val format: (String) -> String = { formatPhoneNumber(it, iso) }
        format
    }
}
