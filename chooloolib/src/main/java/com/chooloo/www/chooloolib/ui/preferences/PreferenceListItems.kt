package com.chooloo.www.chooloolib.ui.preferences

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.chooloo.www.chooloolib.domain.enum.DesignStyle
import com.chooloo.www.chooloolib.ui.components.list.ListItem
import com.chooloo.www.chooloolib.ui.theme.LocalDesignStyle

@Composable
fun ActionPreferenceListItem(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
) {
    ListItem(
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        startContainer = { PreferenceIcon(icon) },
    )
}

@Composable
fun SwitchPreferenceListItem(
    icon: ImageVector,
    title: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    subtitle: String? = null,
) {
    ListItem(
        title = title,
        subtitle = subtitle,
        modifier = Modifier.toggleable(
            value = value,
            role = Role.Switch,
            onValueChange = onValueChange,
        ),
        startContainer = { PreferenceIcon(icon) },
        endContainer = { Switch(checked = value, onCheckedChange = null) },
    )
}

@Composable
fun PreferenceIcon(icon: ImageVector) {
    if (LocalDesignStyle.current == DesignStyle.EXPRESSIVE) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    } else {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
