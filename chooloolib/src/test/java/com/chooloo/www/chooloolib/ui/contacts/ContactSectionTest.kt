package com.chooloo.www.chooloolib.ui.contacts

import org.junit.Assert.assertEquals
import org.junit.Test

class ContactSectionTest {
    @Test
    fun groupsAccentsWithTheirBaseLetterAndUnknownNamesUnderNumberSign() {
        assertEquals("E", contactSection("  Émile"))
        assertEquals("C", contactSection("cruz"))
        assertEquals("#", contactSection("1250"))
        assertEquals("#", contactSection(null))
    }
}
