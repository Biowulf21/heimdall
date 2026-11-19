package com.chooloo.www.chooloolib.ui.dialpad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.zIndex
import com.chooloo.www.chooloolib.R
import com.chooloo.www.chooloolib.ui.components.DialpadNumber
import com.chooloo.www.chooloolib.ui.components.FloatingBubble
import com.chooloo.www.chooloolib.ui.components.Keypad
import com.chooloo.www.chooloolib.ui.components.Reveal
import com.chooloo.www.chooloolib.ui.theme.Sizes
import com.chooloo.www.chooloolib.ui.theme.Spacing

@Composable
fun Dialpad(
    modifier: Modifier = Modifier,
    value: String,
    onCallClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDeleteLongClick: () -> Unit,
    onAddContactClick: () -> Unit,
    onKeyClick: (key: Char) -> Unit,
    onKeyLongClick: ((key: Char) -> Unit)?,
    leftHanded: Boolean = false,
    suggestions: (@Composable () -> Unit)? = null,
) {
    val addContactOnStart = leftHanded == (LocalLayoutDirection.current == LayoutDirection.Ltr)
    BoxWithConstraints(modifier.fillMaxSize()) {
        val viewportHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState(), reverseScrolling = true)
                .heightIn(min = viewportHeight),
            verticalArrangement = Arrangement.Bottom,
        ) {
            suggestions?.invoke()
            DialpadNumber(number = value)
            Keypad(onKeyClick = onKeyClick, onKeyLongClick = onKeyLongClick)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.Medium),
            ) {
                val sideOffset = Sizes.Button / 2 + Spacing.Medium + Sizes.ButtonLarge / 2
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
                ) {
                    SideButton(
                        addContact = addContactOnStart,
                        visible = value.isNotEmpty(),
                        offset = sideOffset,
                        onAddContactClick,
                        onDeleteClick,
                        onDeleteLongClick,
                    )
                    FloatingBubble(
                        icon = Icons.Rounded.Call,
                        contentDescription = stringResource(R.string.cd_call),
                        onClick = onCallClick,
                        size = Sizes.ButtonLarge,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.zIndex(1f),
                    )
                    SideButton(
                        addContact = !addContactOnStart,
                        visible = value.isNotEmpty(),
                        offset = -sideOffset,
                        onAddContactClick,
                        onDeleteClick,
                        onDeleteLongClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun SideButton(
    addContact: Boolean,
    visible: Boolean,
    offset: androidx.compose.ui.unit.Dp,
    onAdd: () -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
) {
    Reveal(visible = visible, hiddenOffsetX = offset, hiddenScale = 1f) {
        FloatingBubble(
            icon = if (addContact) {
                Icons.Rounded.PersonAdd
            } else {
                Icons.AutoMirrored.Rounded.Backspace
            },
            contentDescription = stringResource(
                if (addContact) R.string.cd_add_contact else R.string.cd_backspace,
            ),
            onClick = if (addContact) onAdd else onDelete,
            onLongClick = if (addContact) null else onClear,
            size = Sizes.Button,
        )
    }
}
