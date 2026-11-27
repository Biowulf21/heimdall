package com.chooloo.www.chooloolib.ui.components.list

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.chooloo.www.chooloolib.R
import com.chooloo.www.chooloolib.ui.common.LoadingState
import com.chooloo.www.chooloolib.ui.components.Empty
import com.chooloo.www.chooloolib.ui.components.Loading

@Composable
fun <ItemType> ItemList(
    items: List<ItemType>,
    item: @Composable (ItemType) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    loadingState: LoadingState = LoadingState.SUCCESS,
    @StringRes loadingTitle: Int? = null,
    @StringRes emptyTitle: Int? = null,
    emptyIcon: ImageVector? = null,
    key: ((ItemType) -> String)? = null,
    header: ((ItemType) -> String)? = null,
    listState: LazyListState = rememberLazyListState(),
) {
    val headersByIndex = remember(items, header) { headersByIndex(items, header) }

    when {
        loadingState == LoadingState.LOADING -> Loading(
            modifier = modifier,
            titleRes = loadingTitle,
        )

        loadingState == LoadingState.FAILED -> Empty(
            modifier = modifier,
            titleRes = R.string.error_loading_failed,
        )

        items.isEmpty() -> Empty(
            modifier = modifier,
            titleRes = emptyTitle,
            image = emptyIcon?.let { icon -> { Icon(icon, contentDescription = null) } },
        )

        else -> LazyColumn(
            modifier = modifier,
            contentPadding = contentPadding,
            state = listState,
        ) {
            itemsIndexed(
                items = items,
                key = key?.let {
                    { _, item -> it(item) }
                },
            ) { index, listItem ->
                Column(Modifier.animateItem()) {
                    headersByIndex[index]?.let { HeaderListItem(header = it) }
                    val isFirst = index == 0 || headersByIndex.containsKey(index)
                    val isLast = index == items.lastIndex || headersByIndex.containsKey(index + 1)
                    CompositionLocalProvider(
                        LocalListItemPosition provides ListItemPosition.of(isFirst, isLast),
                    ) {
                        item(listItem)
                    }
                }
            }
        }
    }
}

private fun <ItemType> headersByIndex(
    items: List<ItemType>,
    header: ((ItemType) -> String)?,
): Map<Int, String> {
    if (header == null) return emptyMap()
    var lastHeader: String? = null
    return buildMap {
        items.forEachIndexed { index, item ->
            val current = header(item)
            if (current != lastHeader) {
                put(index, current)
                lastHeader = current
            }
        }
    }
}
