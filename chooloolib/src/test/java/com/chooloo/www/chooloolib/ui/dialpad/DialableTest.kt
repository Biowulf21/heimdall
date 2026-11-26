package com.chooloo.www.chooloolib.ui.dialpad

import org.junit.Assert.assertEquals
import org.junit.Test

class DialableTest {
    @Test
    fun `pasted text keeps only dialable characters`() {
        assertEquals("+639361235989", dialable("+63 (936) 123-5989"))
        assertEquals("09171234567", dialable("Call me: 0917 123 4567 thanks!"))
        assertEquals("*143#", dialable("*143#"))
        assertEquals("+15551234", dialable("+1555+1234"))
        assertEquals("012", dialable("０１２"))
        assertEquals("", dialable("hello"))
    }
}
