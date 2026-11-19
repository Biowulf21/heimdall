package com.chooloo.www.koler.ui.main

import android.Manifest.permission.READ_CONTACTS
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.chooloo.www.chooloolib.domain.repository.ContactRepository
import com.chooloo.www.chooloolib.ui.components.Avatar
import com.chooloo.www.koler.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
internal class DialpadMatchesViewModel @Inject constructor(contacts: ContactRepository) :
    ViewModel() {
    val query = MutableStateFlow<String?>(null)
    val results = combine(contacts.dialpadContacts(), query) { contacts, query ->
        query to query?.let { digits -> contacts.mapNotNull { matchContact(it, digits) } }.orEmpty()
    }.catch { emit(null to emptyList()) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null to emptyList())
}

@Composable
internal fun DialpadMatches(
    digits: String,
    onCall: (String) -> Unit,
    onOpenContact: (Long) -> Unit,
    viewModel: DialpadMatchesViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var allowed by remember {
        mutableStateOf(context.checkSelfPermission(READ_CONTACTS) == PERMISSION_GRANTED)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        allowed = context.checkSelfPermission(READ_CONTACTS) == PERMISSION_GRANTED
    }
    val query = dialpadQuery(digits)
    if (!allowed || query == null) return
    val results by viewModel.results.collectAsStateWithLifecycle()
    val matches = results.second.takeIf { results.first == query }.orEmpty()
    LaunchedEffect(viewModel, query) { viewModel.query.value = query }
    var choosing by rememberSaveable(query) { mutableStateOf<Long?>(null) }

    if (matches.isNotEmpty()) {
        LazyColumn(modifier = Modifier.heightIn(max = 152.dp)) {
            items(matches, key = { it.entry.contact.id }) { match ->
                val contact = match.entry.contact
                val callButton: @Composable () -> Unit = {
                    IconButton(onClick = {
                        match.numbers.singleOrNull()?.let(onCall) ?: run { choosing = contact.id }
                    }) {
                        Icon(
                            Icons.Rounded.Call,
                            stringResource(
                                R.string.dialpad_call_contact,
                                contact.name ?: match.numbers.first(),
                            ),
                        )
                    }
                }
                ListItem(
                    headlineContent = {
                        MatchText(contact.name ?: match.numbers.first(), match.nameMatch)
                    },
                    supportingContent = { MatchText(match.numbers.first(), match.numberMatch) },
                    leadingContent = { Avatar(contact.name, contact.photoUri, 40.dp) },
                    trailingContent = callButton,
                    modifier = Modifier.clickable { onOpenContact(contact.id) },
                )
            }
        }
    }

    matches.firstOrNull { it.entry.contact.id == choosing }?.let { result ->
        AlertDialog(
            onDismissRequest = { choosing = null },
            title = { Text(stringResource(R.string.dialpad_choose_number)) },
            text = {
                LazyColumn(Modifier.heightIn(max = 260.dp)) {
                    items(result.numbers) { number ->
                        ListItem(
                            headlineContent = { Text(number) },
                            modifier = Modifier.clickable {
                                choosing = null
                                onCall(number)
                            },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    choosing = null
                }) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun MatchText(value: String, ranges: List<IntRange>) {
    val accent = MaterialTheme.colorScheme.primary
    Text(
        text = buildAnnotatedString {
            append(value)
            ranges.forEach {
                addStyle(
                    SpanStyle(color = accent, fontWeight = FontWeight.Bold),
                    it.first,
                    it.last + 1,
                )
            }
        },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
