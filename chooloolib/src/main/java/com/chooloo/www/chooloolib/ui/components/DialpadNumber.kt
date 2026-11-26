package com.chooloo.www.chooloolib.ui.components

import android.content.ClipboardManager
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.chooloo.www.chooloolib.ui.common.rememberPhoneFormatter
import com.chooloo.www.chooloolib.ui.theme.Spacing

@Composable
fun DialpadNumber(
    number: String,
    modifier: Modifier = Modifier,
    verticalPadding: Dp = Spacing.Medium,
    onPaste: ((String) -> Unit)? = null,
) {
    val style = MaterialTheme.typography.headlineLarge
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    Box(
        modifier = modifier.then(
            if (onPaste == null) {
                Modifier
            } else {
                Modifier.combinedClickable(
                    interactionSource = null,
                    indication = null,
                    onClickLabel = stringResource(android.R.string.paste),
                    onClick = {},
                    onLongClick = { menu = true },
                )
            },
        ),
    ) {
        NumberText(number, style, verticalPadding)
        if (onPaste != null) {
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(android.R.string.paste)) },
                    onClick = {
                        menu = false
                        context.getSystemService(ClipboardManager::class.java)
                            ?.primaryClip?.takeIf { it.itemCount > 0 }
                            ?.getItemAt(0)?.coerceToText(context)?.toString()
                            ?.let(onPaste)
                    },
                )
            }
        }
    }
}

@Composable
private fun NumberText(number: String, style: TextStyle, verticalPadding: Dp) {
    Text(
        text = rememberPhoneFormatter()(number),
        maxLines = 1,
        style = style,
        textAlign = TextAlign.Center,
        autoSize = TextAutoSize.StepBased(
            minFontSize = MaterialTheme.typography.titleMedium.fontSize,
            maxFontSize = style.fontSize,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = verticalPadding),
    )
}
