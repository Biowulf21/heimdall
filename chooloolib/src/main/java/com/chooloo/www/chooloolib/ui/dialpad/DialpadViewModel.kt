package com.chooloo.www.chooloolib.ui.dialpad

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chooloo.www.chooloolib.domain.repository.AudioRepository
import com.chooloo.www.chooloolib.domain.repository.PreferenceRepository
import com.chooloo.www.chooloolib.domain.repository.RecentRepository
import com.chooloo.www.chooloolib.domain.repository.TelecomRepository
import com.chooloo.www.chooloolib.ui.common.UserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

/** Keeps only what a dialer can dial: digits, * and #, and a + at the very start. */
internal fun dialable(text: String): String {
    // Other scripts' digits (e.g. full-width ０１２) become plain 0-9.
    val kept = text.mapNotNull {
        when {
            it.isDigit() -> it.digitToInt().digitToChar()
            it in "+*#" -> it
            else -> null
        }
    }.joinToString("")
    return kept.take(1) + kept.drop(1).filter { it != '+' }
}

@HiltViewModel
class DialpadViewModel @Inject constructor(
    private val audioRepository: AudioRepository,
    private val recentRepository: RecentRepository,
    private val telecomRepository: TelecomRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {
    data class UiState(val value: String = "", val message: UserMessage? = null)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun setNumber(number: String) {
        _uiState.update { it.copy(value = number.filter { it.isDigit() || it in "+#*" }) }
    }

    fun onPaste(text: String) {
        _uiState.update { it.copy(value = dialable(it.value + text)) }
    }

    fun onCallClick() {
        val value = _uiState.value.value
        launchCatching {
            if (value.isNotEmpty()) {
                telecomRepository.callNumber(value)
            } else {
                recentRepository.lastOutgoingNumber()?.let(::setNumber)
            }
        }
    }

    fun callNumber(number: String) = launchCatching { telecomRepository.callNumber(number) }

    fun onDeleteClick() {
        _uiState.update { it.copy(value = it.value.dropLast(1)) }
    }

    fun onDeleteLongClick() {
        _uiState.update { it.copy(value = "") }
    }

    fun onKeyClick(key: Char) {
        viewModelScope.launch {
            val settings = preferenceRepository.settings.first()
            if (settings.isDialpadTonesEnabled) audioRepository.playTone(key)
            if (settings.isDialpadVibrateEnabled) audioRepository.vibrate()
        }
        val value = _uiState.updateAndGet { it.copy(value = it.value + key) }.value
        viewModelScope.launch {
            runCatching { telecomRepository.handleSecretCode(value) }
                .onFailure { Log.e(TAG, "Secret code failed", it) }
        }
    }

    fun onKeyLongClick(key: Char) {
        when (key) {
            '0' -> _uiState.update { it.copy(value = it.value + '+') }
            '1' -> launchCatching { telecomRepository.callVoicemail() }
        }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }

    private fun launchCatching(action: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { action() }.onFailure { error ->
                Log.e(TAG, "Call failed", error)
                _uiState.update { it.copy(message = UserMessage.CALL_FAILED) }
            }
        }
    }

    private companion object {
        const val TAG = "DialpadViewModel"
    }
}
