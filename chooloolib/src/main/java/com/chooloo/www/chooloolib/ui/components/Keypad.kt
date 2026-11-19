package com.chooloo.www.chooloolib.ui.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.chooloo.www.chooloolib.domain.model.phoneKeyLetters
import com.chooloo.www.chooloolib.ui.theme.Layout
import com.chooloo.www.chooloolib.ui.theme.Sizes
import com.chooloo.www.chooloolib.ui.theme.Spacing

@Composable
fun Keypad(
    modifier: Modifier = Modifier,
    onKeyClick: (key: Char) -> Unit,
    onKeyLongClick: ((key: Char) -> Unit)? = null,
    wideKeyHeight: Dp? = null,
) {
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = if (wideKeyHeight == null) Spacing.Medium else 8.dp),
    ) {
        val keySize = min((maxWidth - Spacing.Large * 2) / 3, Sizes.KeypadKey)
        val gap = if (wideKeyHeight == null) Spacing.Medium else 8.dp
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            Layout.KEYPAD_ROWS.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        if (wideKeyHeight ==
                            null
                        ) {
                            Spacing.Large
                        } else {
                            gap
                        },
                    ),
                ) {
                    row.forEach { key ->
                        KeypadKey(
                            key = key,
                            modifier = if (wideKeyHeight == null) {
                                Modifier.size(keySize)
                            } else {
                                Modifier.weight(1f).height(wideKeyHeight)
                            },
                            showLetters = wideKeyHeight != null,
                            onClick = { onKeyClick(key) },
                            onLongClick = onKeyLongClick?.let { { it(key) } },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadKey(
    key: Char,
    modifier: Modifier,
    showLetters: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
) {
    val hint = if (showLetters) phoneKeyLetters[key] ?: if (key == '0') "+" else null else null
    Surface(
        shape = if (showLetters) MaterialTheme.shapes.medium else CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.combinedClickable(
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = key.toString(),
                    style = if (showLetters) {
                        MaterialTheme.typography.headlineSmall
                    } else {
                        MaterialTheme.typography.headlineLarge
                    },
                )
                hint?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
