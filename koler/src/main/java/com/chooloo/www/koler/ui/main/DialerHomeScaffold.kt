package com.chooloo.www.koler.ui.main

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chooloo.www.chooloolib.R
import com.chooloo.www.chooloolib.ui.theme.Sizes
import com.chooloo.www.chooloolib.ui.theme.Spacing
import com.chooloo.www.chooloolib.ui.theme.screenPadding
import kotlinx.coroutines.launch

/**
 * Home layout: a menu button and a full-width search pill on top, the list below, the floating
 * bar at the bottom. Settings and navigation live in the drawer the menu button opens, so the top
 * row carries no competing buttons. While contacts are selected the pill shows the count and the
 * selection actions instead of the field.
 */
@Composable
internal fun DialerHomeScaffold(
    isSearching: Boolean,
    searchText: String,
    @StringRes searchHintRes: Int,
    onSearchOpen: () -> Unit,
    onSearchTextChange: (String) -> Unit,
    onSearchClose: () -> Unit,
    drawerContent: @Composable ColumnScope.(closeDrawer: () -> Unit) -> Unit,
    selectionTitle: String? = null,
    selectionActions: (@Composable RowScope.() -> Unit)? = null,
    bottomBarContent: @Composable BoxScope.() -> Unit,
    content: @Composable ColumnScope.(PaddingValues) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val closeSearch = {
        focusManager.clearFocus()
        keyboard?.hide()
        onSearchClose()
    }
    BackHandler(enabled = isSearching, onBack = closeSearch)

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val closeDrawer: () -> Unit = { scope.launch { drawerState.close() } }
    BackHandler(enabled = drawerState.isOpen, onBack = closeDrawer)

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(Modifier.padding(horizontal = Spacing.Page, vertical = Spacing.Medium)) {
                    drawerContent(closeDrawer)
                }
            }
        },
    ) {
        Box(Modifier.fillMaxSize().screenPadding().imePadding()) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Sizes.Bar)
                        .padding(bottom = Spacing.Small),
                ) {
                    if (selectionTitle != null && selectionActions != null) {
                        SelectionPill(selectionTitle, selectionActions)
                    } else {
                        IconButton(
                            onClick = {
                                closeSearch()
                                scope.launch { drawerState.open() }
                            },
                        ) {
                            Icon(Icons.Rounded.Menu, stringResource(R.string.cd_open_menu))
                        }
                        Spacer(Modifier.width(Spacing.ExtraSmall))
                        SearchPill(
                            isSearching = isSearching,
                            searchText = searchText,
                            searchHintRes = searchHintRes,
                            onSearchOpen = onSearchOpen,
                            onSearchTextChange = onSearchTextChange,
                            onSearchClose = closeSearch,
                        )
                    }
                }
                content(PaddingValues(bottom = 88.dp))
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                content = bottomBarContent,
            )
        }
    }
}

@Composable
private fun RowScope.SearchPill(
    isSearching: Boolean,
    searchText: String,
    @StringRes searchHintRes: Int,
    onSearchOpen: () -> Unit,
    onSearchTextChange: (String) -> Unit,
    onSearchClose: () -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    TextField(
        value = searchText,
        onValueChange = onSearchTextChange,
        modifier = Modifier
            .weight(1f)
            .onFocusChanged { if (it.isFocused) onSearchOpen() },
        shape = CircleShape,
        placeholder = { Text(stringResource(searchHintRes), maxLines = 1) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = if (isSearching) {
            {
                IconButton(onClick = onSearchClose) {
                    Icon(Icons.Rounded.Close, stringResource(R.string.cd_close_search))
                }
            }
        } else {
            null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {
            keyboard?.hide()
            focusManager.clearFocus()
        }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
private fun SelectionPill(title: String, actions: @Composable RowScope.() -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
            .padding(start = Spacing.Large, end = Spacing.ExtraSmall),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        actions()
    }
}
