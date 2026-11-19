package com.chooloo.www.chooloolib.domain.repository

import com.chooloo.www.chooloolib.domain.model.Contact
import com.chooloo.www.chooloolib.domain.model.ContactDetails
import com.chooloo.www.chooloolib.domain.model.DialpadContact
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    fun contacts(filter: String): Flow<List<Contact>>
    fun dialpadContacts(): Flow<List<DialpadContact>>
    fun contact(contactId: Long): Flow<Contact?>
    fun details(contactId: Long): Flow<ContactDetails>
    suspend fun contactIdOf(link: String): Long?
    suspend fun deleteContact(contactId: Long)
    suspend fun setFavorite(contactId: Long, isFavorite: Boolean)
    suspend fun setRingtone(contactId: Long, ringtone: String?)
    suspend fun setSendsToVoicemail(contactId: Long, enabled: Boolean)
}
