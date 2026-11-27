package com.chooloo.www.koler.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallMissed
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Voicemail
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.chooloo.www.chooloolib.R
import com.chooloo.www.chooloolib.domain.enum.RecentFilter
import com.chooloo.www.chooloolib.ui.theme.Spacing

private class DrawerEntry(
    val icon: ImageVector,
    val label: String,
    val isSelected: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * Gjall's drawer, in Heimdall's terms: a large title, then grouped rounded rows with a round icon
 * badge each. Only the active filter is filled. The bottom bar owns the tabs, so they are not here.
 */
@Composable
internal fun HomeDrawer(
    recentFilter: RecentFilter?,
    favoritesOnly: Boolean,
    onRecentFilter: (RecentFilter) -> Unit,
    onFavorites: () -> Unit,
    onBlockedNumbers: () -> Unit,
    onVoicemail: () -> Unit,
    onSettings: () -> Unit,
    onPhoneSetup: () -> Unit,
    onHelp: () -> Unit,
    onDonate: () -> Unit,
) {
    fun filter(icon: ImageVector, label: String, value: RecentFilter) =
        DrawerEntry(icon, label, recentFilter == value) { onRecentFilter(value) }

    val groups = listOf(
        listOf(
            filter(
                Icons.Rounded.History,
                stringResource(R.string.filter_all_calls),
                RecentFilter.ALL,
            ),
            filter(
                Icons.AutoMirrored.Rounded.CallMissed,
                stringResource(R.string.filter_missed),
                RecentFilter.MISSED,
            ),
            filter(
                Icons.AutoMirrored.Rounded.CallReceived,
                stringResource(R.string.filter_incoming),
                RecentFilter.INCOMING,
            ),
            filter(
                Icons.AutoMirrored.Rounded.CallMade,
                stringResource(R.string.filter_outgoing),
                RecentFilter.OUTGOING,
            ),
        ),
        listOf(
            DrawerEntry(
                Icons.Rounded.Star,
                stringResource(R.string.favourites),
                favoritesOnly,
                onFavorites,
            ),
            DrawerEntry(
                Icons.Rounded.Block,
                stringResource(R.string.blocked_numbers),
                onClick = onBlockedNumbers,
            ),
            DrawerEntry(
                Icons.Rounded.Voicemail,
                stringResource(R.string.voicemail),
                onClick = onVoicemail,
            ),
        ),
        listOf(
            DrawerEntry(
                Icons.Rounded.Settings,
                stringResource(R.string.settings),
                onClick = onSettings,
            ),
            DrawerEntry(
                Icons.Rounded.PhoneAndroid,
                stringResource(R.string.phone_setup),
                onClick = onPhoneSetup,
            ),
            DrawerEntry(
                Icons.Rounded.HelpOutline,
                stringResource(R.string.help_feedback),
                onClick = onHelp,
            ),
            DrawerEntry(
                Icons.Rounded.Favorite,
                stringResource(R.string.pref_title_donate),
                onClick = onDonate,
            ),
        ),
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.Page),
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.Page)
            .padding(bottom = Spacing.Large),
    ) {
        Text(
            text = stringResource(com.chooloo.www.koler.R.string.app_name),
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.padding(
                start = Spacing.Large,
                top = Spacing.ExtraLarge,
                bottom = Spacing.Small,
            ),
        )
        groups.forEach { DrawerGroup(it) }
    }
}

@Composable
private fun DrawerGroup(entries: List<DrawerEntry>) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        entries.forEachIndexed { index, entry ->
            val top = if (index == 0) OUTER else INNER
            val bottom = if (index == entries.lastIndex) OUTER else INNER
            DrawerRow(entry, RoundedCornerShape(top, top, bottom, bottom))
        }
    }
}

@Composable
private fun DrawerRow(entry: DrawerEntry, shape: RoundedCornerShape) {
    val colors = MaterialTheme.colorScheme
    // The selected row is filled, so its badge melts into it like Gjall's.
    val badgeColor =
        if (entry.isSelected) colors.secondaryContainer else colors.surfaceContainerHighest
    val iconColor = if (entry.isSelected) colors.onSecondaryContainer else colors.onSurfaceVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(shape)
            .background(
                if (entry.isSelected) colors.secondaryContainer else colors.surfaceContainer,
            )
            .clickable(role = Role.Button, onClick = entry.onClick)
            .padding(horizontal = Spacing.Medium),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .background(badgeColor, CircleShape),
        ) {
            Icon(
                entry.icon,
                contentDescription = null,
                tint = iconColor,
            )
        }
        Spacer(Modifier.width(Spacing.Medium))
        Text(
            text = entry.label,
            style = MaterialTheme.typography.titleMedium,
            color = if (entry.isSelected) colors.onSecondaryContainer else colors.onSurface,
        )
    }
}

private val OUTER = 28.dp
private val INNER = 4.dp
