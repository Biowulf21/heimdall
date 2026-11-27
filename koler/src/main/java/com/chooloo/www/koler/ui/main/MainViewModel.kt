package com.chooloo.www.koler.ui.main

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chooloo.www.chooloolib.domain.enum.Page
import com.chooloo.www.chooloolib.domain.enum.RecentFilter
import com.chooloo.www.chooloolib.domain.model.PhoneLookup
import com.chooloo.www.chooloolib.domain.repository.PhoneRepository
import com.chooloo.www.chooloolib.domain.repository.PreferenceRepository
import com.chooloo.www.chooloolib.ui.caller.CallerTarget
import com.chooloo.www.chooloolib.ui.home.HomeSearch
import com.chooloo.www.koler.domain.enum.CallState
import com.chooloo.www.koler.domain.model.Call
import com.chooloo.www.koler.domain.repository.CallRepository
import com.chooloo.www.koler.domain.usecase.ClearMissedCalls
import com.chooloo.www.koler.domain.usecase.GetPrimaryCall
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val preferenceRepository: PreferenceRepository,
    private val callRepository: CallRepository,
    private val clearMissedCalls: ClearMissedCalls,
    phoneRepository: PhoneRepository,
    getPrimaryCall: GetPrimaryCall,
) : ViewModel() {
    enum class Tab { RECENTS, CONTACTS }

    data class OngoingCall(val call: Call, val caller: PhoneLookup?)

    data class UiState(
        val tab: Tab? = null,
        val isDialpadOpen: Boolean = false,
        val dialNumber: String? = null,
        val isSearching: Boolean = false,
        val searchText: String = "",
        val selectedCaller: CallerTarget? = null,
        val recentFilter: RecentFilter = RecentFilter.ALL,
        val favoritesOnly: Boolean = false,
    )

    // In memory only: list filters reset when the process or the user leaves.
    private data class TransientState(
        val dialNumber: String? = null,
        val selectedCaller: CallerTarget? = null,
        val recentFilter: RecentFilter = RecentFilter.ALL,
        val favoritesOnly: Boolean = false,
    )

    private val tab = savedStateHandle.getStateFlow<Tab?>(KEY_TAB, null)
    private val isDialpadOpen = savedStateHandle.getStateFlow(KEY_DIALPAD_OPEN, false)
    private val transientState = MutableStateFlow(TransientState())

    val search = HomeSearch(savedStateHandle)
    val phoneSettings = preferenceRepository.settings

    fun completePhoneSetup() {
        viewModelScope.launch { preferenceRepository.completePhoneSetup() }
    }

    val uiState: StateFlow<UiState> = combine(
        tab,
        isDialpadOpen,
        search.isOpen,
        search.text,
        transientState,
    ) {
            tab,
            isDialpadOpen,
            isSearching,
            searchText,
            transient,
        ->
        UiState(
            tab,
            isDialpadOpen,
            transient.dialNumber,
            isSearching,
            searchText,
            transient.selectedCaller,
            transient.recentFilter,
            transient.favoritesOnly,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    val ongoingCall: StateFlow<OngoingCall?> = callRepository.calls
        .map { getPrimaryCall(it) }
        .distinctUntilChanged()
        .map { call ->
            call?.let {
                OngoingCall(it, it.number?.let { number -> phoneRepository.lookup(number) })
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        closeDialSheetsWhenCallStarts()
        viewModelScope.launch {
            if (tab.value != null) return@launch
            val defaultPage = preferenceRepository.settings.first().defaultPage
            if (tab.value != null) return@launch
            savedStateHandle[KEY_TAB] =
                if (defaultPage == Page.CONTACTS) Tab.CONTACTS else Tab.RECENTS
            if (defaultPage == Page.DIALER && callRepository.calls.value.isEmpty()) {
                savedStateHandle[KEY_DIALPAD_OPEN] = true
            }
        }
    }

    fun onDialNumber(number: String) {
        val trimmed = number.trim()
        if (trimmed.isEmpty()) return
        transientState.update { it.copy(dialNumber = trimmed) }
        if (tab.value == null) savedStateHandle[KEY_TAB] = Tab.RECENTS
        savedStateHandle[KEY_DIALPAD_OPEN] = true
    }

    fun onMissedCallsOpened() {
        onTabSelected(Tab.RECENTS)
        onDialpadClose()
        search.close()
        transientState.value = TransientState(recentFilter = RecentFilter.MISSED)
        viewModelScope.launch(NonCancellable) {
            runCatching {
                clearMissedCalls()
            }.onFailure { Log.e(TAG, "Failed clearing missed calls", it) }
        }
    }

    fun onTabSelected(tab: Tab) {
        savedStateHandle[KEY_TAB] = tab
    }

    fun onDialpadOpen() {
        savedStateHandle[KEY_DIALPAD_OPEN] = true
    }

    fun onDialpadClose() {
        savedStateHandle[KEY_DIALPAD_OPEN] = false
        transientState.update { it.copy(dialNumber = null) }
    }

    fun onCallerSelected(target: CallerTarget) {
        transientState.update { it.copy(selectedCaller = target) }
    }

    fun onCallerDismiss() {
        transientState.update { it.copy(selectedCaller = null) }
    }

    fun onRecentFilter(filter: RecentFilter) {
        onTabSelected(Tab.RECENTS)
        transientState.update { it.copy(recentFilter = filter) }
    }

    fun onFavoritesOnly(favoritesOnly: Boolean) {
        if (favoritesOnly) onTabSelected(Tab.CONTACTS)
        transientState.update { it.copy(favoritesOnly = favoritesOnly) }
    }

    fun onUserLeft() {
        transientState.update { it.copy(recentFilter = RecentFilter.ALL, favoritesOnly = false) }
    }

    private fun closeDialSheetsWhenCallStarts() {
        viewModelScope.launch {
            callRepository.calls
                .map { calls -> calls.any { it.state != CallState.INCOMING } }
                .distinctUntilChanged()
                .filter { it }
                .collect {
                    onDialpadClose()
                    onCallerDismiss()
                }
        }
    }

    private companion object {
        const val TAG = "MainViewModel"
        const val KEY_TAB = "tab"
        const val KEY_DIALPAD_OPEN = "dialpad_open"
    }
}
