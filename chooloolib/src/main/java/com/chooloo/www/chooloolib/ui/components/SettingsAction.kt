package com.chooloo.www.chooloolib.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chooloo.www.chooloolib.R
import com.chooloo.www.chooloolib.domain.enum.DesignStyle
import com.chooloo.www.chooloolib.ui.theme.LocalDesignStyle

/** Top-bar settings button: a morphing cookie in Expressive, the plain icon button in Classic. */
@Composable
fun SettingsAction(onClick: () -> Unit) {
    val label = stringResource(R.string.settings)
    if (LocalDesignStyle.current == DesignStyle.EXPRESSIVE) {
        FloatingBubble(
            icon = Icons.Rounded.Settings,
            contentDescription = label,
            onClick = onClick,
            size = 48.dp,
            hasShadow = false,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    } else {
        IconButton(onClick = onClick) { Icon(Icons.Rounded.Settings, label) }
    }
}
