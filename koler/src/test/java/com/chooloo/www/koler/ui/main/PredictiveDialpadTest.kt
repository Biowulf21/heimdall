package com.chooloo.www.koler.ui.main

import com.chooloo.www.chooloolib.domain.model.Contact
import com.chooloo.www.chooloolib.domain.model.DialpadContact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PredictiveDialpadTest {
    @Test
    fun matchesAnyNameWordOrConsecutiveInitials() {
        val lasanas = matchContact(contact("Benny Lásanas"), "52")
        assertEquals(listOf(6..7), lasanas?.nameMatch)
        val julius = matchContact(contact("Julius Acain Rela"), "52")
        assertEquals(listOf(0..0, 7..7), julius?.nameMatch)
        assertNull(matchContact(contact("Benny Lásanas"), "53"))
    }

    @Test
    fun matchesFormattedSecondaryNumberAndRetainsAllMatchingNumbers() {
        val secondary = "+63 917-123-4578"
        val other = "555 4578"
        val entry = contact("Ana Cruz", "0917 1111", secondary, other)
        val result = matchContact(entry, "4578")
        assertEquals(listOf(secondary, other), result?.numbers)
        assertEquals(
            "4578",
            result?.numberMatch.orEmpty().joinToString("") {
                secondary.substring(it)
            },
        )
    }

    @Test
    fun keepsAllNumbersForNameOnlyMatch() {
        val entry = contact("Ana Cruz", "6505550101", "6505550102")
        assertEquals(entry.numbers, matchContact(entry, "2789")?.numbers)
    }

    @Test
    fun acceptsDigitsAndDoesNotMatchServiceCodes() {
        assertEquals("222", dialpadQuery("222"))
        assertEquals("63917", dialpadQuery("+63917"))
        assertEquals("52", dialpadQuery("٥٢"))
        assertNull(dialpadQuery("*#06#"))
        assertNull(dialpadQuery(""))
    }

    private fun contact(name: String, vararg numbers: String) =
        DialpadContact(Contact(id = 1, name = name), numbers.toList().ifEmpty { listOf("550000") })
}
