package com.chooloo.www.chooloolib.ui.contacts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chooloo.www.chooloolib.R
import com.chooloo.www.chooloolib.domain.model.Contact
import com.chooloo.www.chooloolib.ui.common.LoadingState
import com.chooloo.www.chooloolib.ui.components.list.ItemList
import java.text.Normalizer

private data class ContactRow(val contact: Contact, val isFavorite: Boolean) {
    val section: String get() = if (isFavorite) "★" else contactSection(contact.name)
}

internal fun contactSection(name: String?): String {
    val first = name?.trimStart()?.firstOrNull()?.takeIf(Char::isLetter) ?: return "#"
    return Normalizer.normalize(first.toString(), Normalizer.Form.NFD).first().uppercase()
}

@Composable
fun ContactsList(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    items: List<Contact>,
    withFavorites: Boolean = false,
    onItemClick: (Contact) -> Unit,
    onItemLongClick: ((Contact) -> Unit)?,
    loadingState: LoadingState,
    alphabetIndexOnLeft: Boolean? = null,
    circularLetterPreview: Boolean = false,
    selectedIds: Set<Long> = emptySet(),
) {
    val state = rememberLazyListState()
    val rows = remember(items, withFavorites, alphabetIndexOnLeft != null) {
        val favorites = if (withFavorites) {
            items.filter { it.isFavorite }.map { ContactRow(it, isFavorite = true) }
        } else {
            emptyList()
        }
        val contacts = items.map { ContactRow(it, isFavorite = false) }
        favorites + if (alphabetIndexOnLeft == null) {
            contacts
        } else {
            contacts.groupBy { it.section }.values.flatten()
        }
    }
    val sections = remember(rows, alphabetIndexOnLeft, loadingState) {
        if (alphabetIndexOnLeft == null || loadingState != LoadingState.SUCCESS) {
            emptyMap()
        } else {
            buildMap {
                rows.forEachIndexed { index, row ->
                    if (row.section !in
                        this
                    ) {
                        put(row.section, index)
                    }
                }
            }
        }
    }
    val listInset = when {
        sections.isEmpty() -> Modifier
        alphabetIndexOnLeft == true -> Modifier.padding(PaddingValues.Absolute(left = 32.dp))
        else -> Modifier.padding(PaddingValues.Absolute(right = 32.dp))
    }
    Box(modifier.fillMaxSize()) {
        ItemList(
            items = rows,
            modifier = Modifier.fillMaxSize().then(listInset),
            contentPadding = contentPadding,
            loadingState = loadingState,
            key = { row -> "${row.isFavorite}_${row.contact.id}" },
            loadingTitle = R.string.loading_contacts,
            emptyTitle = R.string.no_results_contacts,
            emptyIcon = Icons.Rounded.Person,
            header = { it.section },
            listState = state,
            item = { row ->
                ContactListItem(
                    contact = row.contact,
                    isSelected = row.contact.id in selectedIds,
                    onClick = { onItemClick(row.contact) },
                    onLongClick = onItemLongClick?.let { { it(row.contact) } },
                )
            },
        )
        if (sections.isNotEmpty()) {
            AlphabetIndex(
                sections = sections.entries.toList(),
                listState = state,
                onLeft = alphabetIndexOnLeft == true,
                circularPreview = circularLetterPreview,
                modifier = Modifier
                    .align(
                        if (alphabetIndexOnLeft ==
                            true
                        ) {
                            AbsoluteAlignment.TopLeft
                        } else {
                            AbsoluteAlignment.TopRight
                        },
                    )
                    .padding(bottom = contentPadding.calculateBottomPadding()),
            )
        }
    }
}
