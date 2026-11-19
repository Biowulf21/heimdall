package com.chooloo.www.chooloolib.ui.contacts

import org.junit.Assert.assertEquals
import org.junit.Test

class ContactSectionTest {
    @Test
    fun scrollPreviewCurvePeaksAtTheActiveLetter() {
        assertEquals(1f, alphabetBend(0f), 0f)
        assertEquals(alphabetBend(60f), alphabetBend(-60f), 0f)
        org.junit.Assert.assertTrue(alphabetBend(30f) > alphabetBend(90f))
        assertEquals(0f, alphabetBend(144f), 0f)
    }

    @Test
    fun scrollbarTargetsStayWithinTheContactList() {
        assertEquals(0, scrollTarget(-0.2f, 20))
        assertEquals(10, scrollTarget(0.5f, 20))
        assertEquals(20, scrollTarget(1.2f, 20))
        assertEquals(0, scrollTarget(0.8f, 0))
    }

    @Test
    fun groupsAccentsWithTheirBaseLetterAndUnknownNamesUnderNumberSign() {
        assertEquals("E", contactSection("  Émile"))
        assertEquals("C", contactSection("cruz"))
        assertEquals("#", contactSection("1250"))
        assertEquals("#", contactSection(null))
    }
}
