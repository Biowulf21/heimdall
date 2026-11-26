package com.chooloo.www.chooloolib.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneFormatTest {
    @Test
    fun `platform groups are joined with dashes`() {
        assertEquals("0936-123-5989", dashGroups("0936 123 5989"))
        assertEquals("555-123-4567", dashGroups("(555) 123-4567"))
        assertEquals("+63-936-123-5989", dashGroups("+63 936 123 5989"))
        assertEquals("+1-555-123-4567", dashGroups(" +1 (555) 123-4567 "))
    }

    @Test
    fun `service codes and letters are left alone`() {
        assertEquals("*143#", formatPhoneNumber("*143#", "PH"))
        assertEquals("1-800-FLOWERS", formatPhoneNumber("1-800-FLOWERS", "US"))
    }
}
