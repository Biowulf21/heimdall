package com.chooloo.www.chooloolib.ui.dialpad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.chooloo.www.chooloolib.R
import com.chooloo.www.chooloolib.ui.components.DialpadNumber
import com.chooloo.www.chooloolib.ui.components.FloatingBubble
import com.chooloo.www.chooloolib.ui.components.Keypad

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
        val scrollPage = viewportHeight < 460.dp
        val keyHeight = ((viewportHeight * 0.6f - 176.dp) / 4).coerceIn(48.dp, 94.dp)
        Column(
            modifier = if (scrollPage) {
                Modifier.fillMaxWidth()
                    .verticalScroll(rememberScrollState(), reverseScrolling = true)
                    .heightIn(min = viewportHeight)
            } else {
                Modifier.fillMaxSize()
            },
            verticalArrangement = Arrangement.Bottom,
        ) {
            if (scrollPage) {
                suggestions?.invoke()
            } else if (suggestions != null) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    suggestions()
                }
            } else {
                Spacer(Modifier.weight(1f))
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SideButton(
                            addContactOnStart,
                            value.isNotEmpty(),
                            onAddContactClick,
                            onDeleteClick,
                            onDeleteLongClick,
                        )
                        DialpadNumber(value, Modifier.weight(1f), verticalPadding = 0.dp)
                        SideButton(
                            !addContactOnStart,
                            value.isNotEmpty(),
                            onAddContactClick,
                            onDeleteClick,
                            onDeleteLongClick,
                        )
                    }
                    Keypad(
                        onKeyClick = onKeyClick,
                        onKeyLongClick = onKeyLongClick,
                        wideKeyHeight = keyHeight,
                    )
                    Button(
                        onClick = onCallClick,
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.widthIn(min = 148.dp).heightIn(min = 52.dp),
                    ) {
                        Icon(Icons.Rounded.Call, contentDescription = null)
                        Text(
                            stringResource(R.string.cd_call),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SideButton(
    addContact: Boolean,
    visible: Boolean,
    onAdd: () -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
) {
    if (!visible) {
        Spacer(Modifier.size(48.dp))
    } else if (addContact) {
        IconButton(onClick = onAdd, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Rounded.PersonAdd, stringResource(R.string.cd_add_contact))
        }
    } else {
        FloatingBubble(
            icon = Icons.AutoMirrored.Rounded.Backspace,
            contentDescription = stringResource(R.string.cd_backspace),
            onClick = onDelete,
            onLongClick = onClear,
            size = 48.dp,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            hasShadow = false,
        )
    }
}
