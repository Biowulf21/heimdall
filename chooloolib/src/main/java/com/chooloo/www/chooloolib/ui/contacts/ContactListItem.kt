package com.chooloo.www.chooloolib.ui.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.chooloo.www.chooloolib.R
import com.chooloo.www.chooloolib.domain.model.Contact
import com.chooloo.www.chooloolib.ui.components.Avatar
import com.chooloo.www.chooloolib.ui.components.list.ListItem
import com.chooloo.www.chooloolib.ui.theme.Sizes

@Composable
fun ContactListItem(
    contact: Contact,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
) {
    ListItem(
        modifier = modifier.height(Sizes.CompactListItem).then(
            if (isSelected) {
                Modifier.background(MaterialTheme.colorScheme.secondaryContainer)
                    .semantics { selected = true }
            } else {
                Modifier
            },
        ),
        subtitle = contact.number,
        title = contact.name ?: stringResource(id = R.string.contact_unknown_name),
        onClick = onClick,
        onLongClick = onLongClick,
        endContainer = if (isSelected) {
            {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            null
        },
        startContainer = {
            Avatar(name = contact.name, photoUri = contact.photoUri, size = Sizes.AvatarSmall)
        },
    )
}
