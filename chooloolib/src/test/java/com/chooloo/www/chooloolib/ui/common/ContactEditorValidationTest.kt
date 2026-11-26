package com.chooloo.www.chooloolib.ui.common

import com.chooloo.www.chooloolib.R
import org.junit.Assert.assertEquals
import org.junit.Test

class ContactEditorValidationTest {
    private val first = ContactEditorField(ContactFieldKind.FIRST_NAME)
    private val phone = ContactEditorField(ContactFieldKind.PHONE)
    private val email = ContactEditorField(ContactFieldKind.EMAIL)
    private val fields = listOf(first, phone, email)

    private fun errors(
        phone: String = "",
        email: String = "",
        name: String = "",
        creating: Boolean = true,
    ) = validateContact(
        fields,
        mapOf(first.key to name, this.phone.key to phone, this.email.key to email),
        creating,
    )

    @Test
    fun `real phone numbers and emails pass`() {
        listOf("+1 (555) 123-4567", "*123#", "555,1234", "09171234567").forEach {
            assertEquals(it, emptyMap<String, Int>(), errors(phone = it))
        }
        assertEquals(emptyMap<String, Int>(), errors(email = "ada@example.org"))
    }

    @Test
    fun `junk phone numbers and emails fail`() {
        listOf("abc", "12", "555-12a4").forEach {
            assertEquals(
                it,
                mapOf(phone.key to R.string.contact_editor_invalid_phone),
                errors(phone = it, name = "A"),
            )
        }
        listOf("x@", "ada@example", "a b@c.d").forEach {
            assertEquals(
                it,
                mapOf(email.key to R.string.contact_editor_invalid_email),
                errors(email = it, name = "A"),
            )
        }
    }

    @Test
    fun `a new contact needs something, an edit does not`() {
        assertEquals(mapOf(first.key to R.string.contact_editor_empty), errors())
        assertEquals(emptyMap<String, Int>(), errors(creating = false))
    }

    @Test
    fun `new fields in different slots get different keys`() {
        val keys = (0..2).map { ContactEditorField(ContactFieldKind.PHONE, slot = it).key }
        assertEquals(3, keys.toSet().size)
    }

    @Test
    fun `names from other apps split at the first space`() {
        assertEquals("Ada" to "Lovelace", splitName(" Ada Lovelace "))
        assertEquals("Ada" to "King Lovelace", splitName("Ada King Lovelace"))
        assertEquals("Ada" to "", splitName("Ada"))
        assertEquals("" to "", splitName(""))
    }
}
