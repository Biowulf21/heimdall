package com.chooloo.www.chooloolib.ui.dialpad

import android.Manifest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chooloo.www.chooloolib.R
import com.chooloo.www.chooloolib.ui.common.CallGate
import com.chooloo.www.chooloolib.ui.common.rememberExternalActions
import com.chooloo.www.chooloolib.ui.components.MessageToast
import com.chooloo.www.chooloolib.ui.components.Permissioned

@Composable
fun DialpadScreen(
    initialNumber: String? = null,
    dialpadViewModel: DialpadViewModel = hiltViewModel(),
    leftHanded: Boolean = false,
    callGate: CallGate? = null,
    suggestions: (@Composable (String, (String) -> Unit) -> Unit)? = null,
) {
    val externalActions = rememberExternalActions()
    val uiState by dialpadViewModel.uiState.collectAsStateWithLifecycle()
    val value = uiState.value

    LaunchedEffect(initialNumber) {
        if (!initialNumber.isNullOrEmpty()) dialpadViewModel.setNumber(initialNumber)
    }

    fun requestCall(number: String?, placeCall: () -> Unit) {
        if (callGate == null) placeCall() else callGate(number, placeCall)
    }

    MessageToast(message = uiState.message, onShown = dialpadViewModel::onMessageShown)
    val keypad: @Composable () -> Unit = {
        Dialpad(
            value = value,
            onCallClick = {
                if (value.isEmpty()) {
                    dialpadViewModel.onCallClick()
                } else {
                    requestCall(value) { dialpadViewModel.onCallClick() }
                }
            },
            onDeleteClick = dialpadViewModel::onDeleteClick,
            onAddContactClick = { externalActions.createContact(value) },
            onDeleteLongClick = dialpadViewModel::onDeleteLongClick,
            onKeyClick = dialpadViewModel::onKeyClick,
            onKeyLongClick = { key ->
                if (key == '1') {
                    requestCall(null) { dialpadViewModel.onKeyLongClick(key) }
                } else {
                    dialpadViewModel.onKeyLongClick(key)
                }
            },
            leftHanded = leftHanded,
            suggestions = suggestions?.let { content ->
                {
                    content(value) { number ->
                        requestCall(number) { dialpadViewModel.callNumber(number) }
                    }
                }
            },
        )
    }
    if (callGate == null) {
        Permissioned(
            permissions = listOf(Manifest.permission.CALL_PHONE),
            rationaleRes = R.string.permission_rationale_phone,
            content = keypad,
        )
    } else {
        keypad()
    }
}
