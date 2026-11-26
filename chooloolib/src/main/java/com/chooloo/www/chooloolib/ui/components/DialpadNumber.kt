package com.chooloo.www.chooloolib.ui.components

import android.content.ClipboardManager
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
                    onClick = { menu = true },
                    onLongClick = { menu = true },
                )
            },
        ),
    ) {
        NumberText(number, style, verticalPadding, showCaret = onPaste != null)
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
private fun NumberText(number: String, style: TextStyle, verticalPadding: Dp, showCaret: Boolean) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = verticalPadding),
    ) {
        NumberLine(number, style, Modifier.weight(1f, fill = false))
        if (showCaret) Caret(style)
    }
}

/** A text cursor after the number, so the field reads as a place to type or paste into. */
@Composable
private fun Caret(style: TextStyle) {
    val blink = rememberInfiniteTransition(label = "Caret")
    val alpha by blink.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        // Hard on/off like a system cursor: visible for half the cycle, then hidden.
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 1000
                1f at 0
                1f at 499
                0f at 500
                0f at 999
            },
        ),
        label = "Caret blink",
    )
    Box(
        Modifier
            .padding(start = 2.dp)
            .width(2.dp)
            .height(with(LocalDensity.current) { style.fontSize.toDp() })
            .graphicsLayer { this.alpha = alpha }
            .background(MaterialTheme.colorScheme.primary),
    )
}

@Composable
private fun NumberLine(number: String, style: TextStyle, modifier: Modifier) {
    Text(
        text = rememberPhoneFormatter()(number),
        maxLines = 1,
        style = style,
        textAlign = TextAlign.Center,
        autoSize = TextAutoSize.StepBased(
            minFontSize = MaterialTheme.typography.titleMedium.fontSize,
            maxFontSize = style.fontSize,
        ),
        modifier = modifier,
    )
}
