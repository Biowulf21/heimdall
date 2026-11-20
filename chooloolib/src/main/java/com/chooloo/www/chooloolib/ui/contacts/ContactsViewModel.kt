package com.chooloo.www.chooloolib.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chooloo.www.chooloolib.domain.model.Contact
import com.chooloo.www.chooloolib.domain.repository.ContactRepository
import com.chooloo.www.chooloolib.ui.common.ListUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ContactsViewModel @Inject constructor(private val contactRepository: ContactRepository) :
    ViewModel() {
    private val filter = MutableStateFlow("")

    val uiState: StateFlow<ListUiState<Contact>> = filter
        .flatMapLatest { ListUiState.from(contactRepository.contacts(it), TAG) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListUiState())

    fun onFilterChanged(filter: String) {
        this.filter.value = filter
    }

    data class Selection(
        val ids: Set<Long> = emptySet(),
        val deleting: Boolean = false,
        val deleteFailed: Boolean = false,
    )

    private val _selection = MutableStateFlow(Selection())
    val selection: StateFlow<Selection> = _selection

    fun toggleSelected(id: Long) {
        val current = _selection.value
        if (current.deleting) return
        val ids = if (id in current.ids) current.ids - id else current.ids + id
        _selection.value = Selection(ids)
    }

    fun clearSelection() {
        if (!_selection.value.deleting) _selection.value = Selection()
    }

    fun deleteSelection() {
        val current = _selection.value
        if (current.ids.isEmpty() || current.deleting) return
        _selection.value = current.copy(deleting = true, deleteFailed = false)
        viewModelScope.launch {
            val failed = current.ids.filter { id ->
                try {
                    contactRepository.deleteContact(id)
                    false
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    true
                }
            }.toSet()
            _selection.value = Selection(ids = failed, deleteFailed = failed.isNotEmpty())
        }
    }

    private companion object {
        const val TAG = "ContactsViewModel"
    }
}
