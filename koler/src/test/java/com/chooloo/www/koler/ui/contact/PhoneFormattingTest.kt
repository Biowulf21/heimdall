package com.chooloo.www.koler.ui.contact

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PhoneFormattingTest {
    @Test
    fun `formatting variants share a key without losing dial modifiers`() {
        assertEquals(phoneFormattingKey("012 345 6789"), phoneFormattingKey("(012) 345-6789"))
        assertNotEquals(phoneFormattingKey("+123"), phoneFormattingKey("123"))
        assertNotEquals(phoneFormattingKey("123,45"), phoneFormattingKey("12345"))
        assertNotEquals(phoneFormattingKey("123;45"), phoneFormattingKey("123,45"))
    }
}
