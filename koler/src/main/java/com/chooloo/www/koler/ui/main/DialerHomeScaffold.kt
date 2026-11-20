package com.chooloo.www.koler.ui.main

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.chooloo.www.chooloolib.R
import com.chooloo.www.chooloolib.ui.components.ScreenTopBar

@Composable
internal fun DialerHomeScaffold(
    title: String,
    isSearching: Boolean,
    searchText: String,
    @StringRes searchHintRes: Int,
    onSearchOpen: () -> Unit,
    onSearchTextChange: (String) -> Unit,
    onSearchClose: () -> Unit,
    onSettingsOpen: () -> Unit,
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

    Box(Modifier.fillMaxSize().imePadding()) {
        Column(Modifier.fillMaxSize()) {
            ScreenTopBar(
                title = title,
                actions = selectionActions ?: {
                    IconButton(onClick = onSettingsOpen) {
                        Icon(Icons.Rounded.Settings, stringResource(R.string.settings))
                    }
                },
            )
            TextField(
                value = searchText,
                onValueChange = onSearchTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .heightIn(min = 56.dp)
                    .onFocusChanged { if (it.isFocused) onSearchOpen() },
                shape = MaterialTheme.shapes.large,
                placeholder = { Text(stringResource(searchHintRes)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = if (isSearching) {
                    {
                        IconButton(onClick = closeSearch) {
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
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )
            content(PaddingValues(bottom = 88.dp))
        }
        Box(
            modifier = Modifier.align(
                Alignment.BottomCenter,
            ).fillMaxWidth().padding(vertical = 12.dp),
            content = bottomBarContent,
        )
    }
}
