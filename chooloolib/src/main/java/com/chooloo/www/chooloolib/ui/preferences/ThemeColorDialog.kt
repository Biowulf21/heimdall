package com.chooloo.www.chooloolib.ui.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.chooloo.www.chooloolib.R
import com.chooloo.www.chooloolib.ui.theme.accentInk
import java.util.Locale

private val MaterialSwatches = listOf(
    0xE57373, 0xF44336, 0xC62828,
    0xBA68C8, 0x8E24AA, 0x5E35B1,
    0x7986CB, 0x3F51B5, 0x283593,
    0x90CAF9, 0x42A5F5, 0x1565C0,
    0x4FC3F7, 0x00ACC1, 0x00796B,
    0x81C784, 0x43A047, 0x2E7D32,
    0xFFD54F, 0xFF9800, 0xE65100,
    0xB0BEC5, 0x607D8B, 0x455A64,
)

internal fun accentHex(rgb: Int): String = String.format(Locale.ROOT, "#%06X", rgb)

private val hexColor = Regex("#[0-9a-fA-F]{6}")

internal fun parseAccentHex(value: String): Int? =
    value.trim().takeIf(hexColor::matches)?.substring(1)?.toInt(16)

@Composable
internal fun ThemeColorDialog(accentRgb: Int, onAccentRgb: (Int) -> Unit, onDismiss: () -> Unit) {
    var hex by rememberSaveable { mutableStateOf(accentHex(accentRgb)) }
    val parsed = parseAccentHex(hex)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pref_title_theme_color)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                Text(stringResource(R.string.theme_material_colors))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth().selectableGroup(),
                ) {
                    MaterialSwatches.forEach { rgb ->
                        val label = accentHex(rgb)
                        val color = Color(0xFF000000.toInt() or rgb)
                        Surface(
                            shape = CircleShape,
                            color = color,
                            contentColor = accentInk(color),
                            modifier = Modifier
                                .size(48.dp)
                                .selectable(parsed == rgb, role = Role.RadioButton) { hex = label }
                                .semantics { contentDescription = label },
                        ) {
                            if (parsed == rgb) {
                                androidx.compose.foundation.layout.Box(
                                    contentAlignment = androidx.compose.ui.Alignment.Center,
                                ) {
                                    Icon(Icons.Rounded.Check, contentDescription = null)
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = hex,
                    onValueChange = { hex = it },
                    label = { Text(stringResource(R.string.theme_custom_color)) },
                    placeholder = { Text("#42A5F5") },
                    singleLine = true,
                    isError = parsed == null,
                    supportingText = {
                        if (parsed == null) Text(stringResource(R.string.theme_hex_error))
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = parsed != null,
                onClick = {
                    parsed?.let(onAccentRgb)
                    onDismiss()
                },
            ) { Text(stringResource(R.string.theme_apply)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}
