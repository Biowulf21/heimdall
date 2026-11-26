package com.chooloo.www.chooloolib.ui.common

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneFormatTest {
    @Test
    fun `service codes and letters are left alone`() {
        assertEquals("*143#", formatPhoneNumber("*143#", "PH"))
        assertEquals("1-800-FLOWERS", formatPhoneNumber("1-800-FLOWERS", "US"))
        assertEquals("", formatPhoneNumber("", "PH"))
    }

    // Stand-in for the platform formatter: "09569212428" -> "0956 921 2428".
    private val groups = PhoneVisualTransformation { raw ->
        val digits = raw.filter(Char::isDigit)
        listOf(digits.take(4), digits.drop(4).take(3), digits.drop(7)).filter(String::isNotEmpty)
            .joinToString(" ")
    }

    @Test
    fun `phone field shows groups and maps the cursor across spaces`() {
        val result = groups.filter(AnnotatedString("09569212428"))
        assertEquals("0956 921 2428", result.text.text)
        val map = result.offsetMapping
        assertEquals(0, map.originalToTransformed(0))
        assertEquals(4, map.originalToTransformed(4))
        assertEquals(6, map.originalToTransformed(5))
        assertEquals(13, map.originalToTransformed(11))
        assertEquals(4, map.transformedToOriginal(5))
        assertEquals(11, map.transformedToOriginal(13))
    }

    @Test
    fun `phone field leaves text alone when formatting would change the digits`() {
        val broken = PhoneVisualTransformation { "123" }
        assertEquals("0956", broken.filter(AnnotatedString("0956")).text.text)
    }

    @Test
    fun `input stops at 15 digits, the longest any phone number can be`() {
        assertEquals("+639569212428", capPhoneDigits("+639569212428"))
        assertEquals("123456789012345", capPhoneDigits("1234567890123456789"))
        assertEquals("+12 345 678 901 234 5", capPhoneDigits("+12 345 678 901 234 567"))
        assertEquals("*#*#4636#*#*", capPhoneDigits("*#*#4636#*#*"))
    }
}
