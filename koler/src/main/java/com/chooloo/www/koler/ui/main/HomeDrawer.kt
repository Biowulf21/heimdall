package com.chooloo.www.koler.ui.main

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.chooloo.www.chooloolib.R
import com.chooloo.www.chooloolib.ui.theme.Spacing
import com.chooloo.www.koler.ui.main.MainViewModel.Tab

@Composable
internal fun ColumnScope.HomeDrawer(
    selectedTab: Tab,
    onTabSelected: (Tab) -> Unit,
    onAddContact: () -> Unit,
    onSettings: () -> Unit,
) {
    Text(
        text = stringResource(com.chooloo.www.koler.R.string.app_name),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(
            start = Spacing.Medium,
            top = Spacing.Small,
            bottom = Spacing.Medium,
        ),
    )
    DrawerItem(
        Icons.Rounded.History,
        stringResource(R.string.recents),
        selectedTab == Tab.RECENTS,
    ) {
        onTabSelected(Tab.RECENTS)
    }
    DrawerItem(
        Icons.Rounded.Person,
        stringResource(R.string.contacts),
        selectedTab == Tab.CONTACTS,
    ) {
        onTabSelected(Tab.CONTACTS)
    }
    HorizontalDivider(Modifier.padding(horizontal = Spacing.Medium, vertical = Spacing.Small))
    DrawerItem(
        Icons.Rounded.PersonAdd,
        stringResource(R.string.cd_add_contact),
        false,
        onAddContact,
    )
    DrawerItem(Icons.Rounded.Settings, stringResource(R.string.settings), false, onSettings)
}

@Composable
private fun DrawerItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label) },
        selected = selected,
        onClick = onClick,
    )
}
