package com.chooloo.www.koler.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.chooloo.www.chooloolib.R
import com.chooloo.www.chooloolib.ui.components.FloatingBubble
import com.chooloo.www.chooloolib.ui.components.FloatingPill
import com.chooloo.www.chooloolib.ui.components.Reveal
import com.chooloo.www.chooloolib.ui.theme.Motion
import com.chooloo.www.chooloolib.ui.theme.Sizes
import com.chooloo.www.koler.ui.main.MainViewModel.Tab

/**
 * Tabs on one side, one primary button on the other: the dialpad on Recents, add contact on
 * Contacts. It is the screen's only accent, so it always names the main thing to do there.
 * Left-handed mode swaps the sides.
 */
@Composable
fun MainNavigationBar(
    selectedTab: Tab,
    onTabSelected: (Tab) -> Unit,
    isPrimaryVisible: Boolean,
    leftHanded: Boolean,
    onDialpadClick: () -> Unit,
    onAddContactClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isContacts = selectedTab == Tab.CONTACTS
    // One lobe of the 9-lobe cookie, so the shape lands back on itself as the action swaps.
    val turn by animateFloatAsState(
        targetValue = if (isContacts) -40f else 0f,
        animationSpec = tween(Motion.SURFACE_MILLIS, easing = Motion.EaseOut),
        label = "primaryTurn",
    )
    val tabs = @Composable {
        FloatingPill {
            TabBubble(Icons.Rounded.History, stringResource(R.string.recents), !isContacts) {
                onTabSelected(Tab.RECENTS)
            }
            TabBubble(Icons.Rounded.Person, stringResource(R.string.contacts), isContacts) {
                onTabSelected(Tab.CONTACTS)
            }
        }
    }
    val primary = @Composable {
        Reveal(visible = isPrimaryVisible) {
            FloatingBubble(
                icon = if (isContacts) Icons.Rounded.PersonAdd else Icons.Rounded.Dialpad,
                contentDescription = stringResource(
                    if (isContacts) R.string.cd_add_contact else R.string.cd_dialpad,
                ),
                onClick = if (isContacts) onAddContactClick else onDialpadClick,
                size = Sizes.ButtonLarge,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shapeRotation = turn,
            )
        }
    }
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        if (leftHanded) {
            primary()
            tabs()
        } else {
            tabs()
            primary()
        }
    }
}

@Composable
private fun TabBubble(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    FloatingBubble(
        icon = icon,
        contentDescription = label,
        onClick = onClick,
        size = Sizes.Button,
        containerColor = animateColorAsState(
            if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
            label = "tabContainer",
        ).value,
        contentColor = animateColorAsState(
            if (isSelected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            label = "tabContent",
        ).value,
        hasShadow = false,
    )
}
