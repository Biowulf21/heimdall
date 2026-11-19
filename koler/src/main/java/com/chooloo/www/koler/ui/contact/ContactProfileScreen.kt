package com.chooloo.www.koler.ui.contact

import android.app.Activity
import android.media.RingtoneManager
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AddToHomeScreen
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Voicemail
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chooloo.www.chooloolib.R as SharedR
import com.chooloo.www.chooloolib.ui.caller.CallerTarget
import com.chooloo.www.chooloolib.ui.caller.CallerViewModel
import com.chooloo.www.chooloolib.ui.common.LoadingState
import com.chooloo.www.chooloolib.ui.common.rememberExternalActions
import com.chooloo.www.chooloolib.ui.components.Avatar
import com.chooloo.www.chooloolib.ui.components.ConfirmDialog
import com.chooloo.www.chooloolib.ui.components.Empty
import com.chooloo.www.chooloolib.ui.components.Loading
import com.chooloo.www.chooloolib.ui.components.MessageToast
import com.chooloo.www.chooloolib.ui.components.list.ListItem
import com.chooloo.www.chooloolib.ui.recents.RecentTypeIcon
import com.chooloo.www.koler.R

private enum class ContactAction(@StringRes val title: Int, val icon: ImageVector) {
    CALL(SharedR.string.action_call, Icons.Rounded.Call),
    MESSAGE(SharedR.string.action_message, Icons.AutoMirrored.Rounded.Message),
    VIDEO(R.string.contact_video, Icons.Rounded.Videocam),
    EMAIL(R.string.contact_email, Icons.Rounded.Email),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContactProfileScreen(
    target: CallerTarget,
    onBack: () -> Unit,
    viewModel: CallerViewModel = hiltViewModel<CallerViewModel, CallerViewModel.Factory>(
        key = target.toString(),
        creationCallback = { it.create(target) },
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val external = rememberExternalActions()
    val actions = remember(context) { ContactProfileActions(context) }
    val list = rememberLazyListState()
    val collapsed by remember { derivedStateOf { list.firstVisibleItemIndex > 0 } }
    val title = state.name ?: state.number ?: stringResource(SharedR.string.caller_unknown)
    val contact = state.contact
    val phones = state.details.phones.filter { it.number.isNotBlank() }
    val phoneNumbers = phones.map { it.number }.ifEmpty { listOfNotNull(state.number) }
    val emails = state.details.emails
    var selection by rememberSaveable { mutableStateOf<ContactAction?>(null) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val ringtonePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK &&
            data?.hasExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) == true
        ) {
            val picked = IntentCompat.getParcelableExtra(
                data,
                RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                Uri::class.java,
            )
            val ringtone = when (picked) {
                null -> ""
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE) -> null
                else -> picked.toString()
            }
            viewModel.onSetRingtone(ringtone)
        }
    }

    fun valuesFor(action: ContactAction) = if (action ==
        ContactAction.EMAIL
    ) {
        emails
    } else {
        phoneNumbers
    }

    fun perform(action: ContactAction, value: String) {
        when (action) {
            ContactAction.CALL -> {
                viewModel.onCall(value)
            }

            ContactAction.MESSAGE -> external.openSms(value)

            ContactAction.VIDEO -> actions.videoCall(value)

            ContactAction.EMAIL -> external.sendEmail(value)
        }
    }

    fun select(action: ContactAction) {
        val values = valuesFor(action)
        when (values.size) {
            0 -> Unit
            1 -> perform(action, values.single())
            else -> selection = action
        }
    }

    MessageToast(message = state.message, onShown = viewModel::onMessageShown)
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                if (collapsed) Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        stringResource(R.string.contact_back),
                    )
                }
            },
            actions = {
                if (contact != null) {
                    IconButton(onClick = { viewModel.onToggleFavorite() }) {
                        Icon(
                            if (contact.isFavorite) {
                                Icons.Rounded.Star
                            } else {
                                Icons.Rounded.StarBorder
                            },
                            stringResource(
                                if (contact.isFavorite) {
                                    R.string.contact_remove_favorite
                                } else {
                                    R.string.contact_add_favorite
                                },
                            ),
                        )
                    }
                    IconButton(onClick = { external.editContact(contact.id) }) {
                        Icon(Icons.Rounded.Edit, stringResource(SharedR.string.action_edit))
                    }
                }
            },
        )

        when (state.loadingState) {
            LoadingState.LOADING -> Loading()

            LoadingState.FAILED -> Empty(titleRes = SharedR.string.error_caller_not_found)

            LoadingState.SUCCESS -> if (contact != null) {
                LazyColumn(
                    state = list,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 20.dp),
                ) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(
                                horizontal = 16.dp,
                                vertical = 16.dp,
                            ),
                        ) {
                            Avatar(name = contact.name, photoUri = contact.photoUri, size = 104.dp)
                            Text(
                                title,
                                style = MaterialTheme.typography.headlineMedium,
                                textAlign = TextAlign.Center,
                            )
                            state.details.organization?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                    stickyHeader {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(
                                6.dp,
                                Alignment.CenterHorizontally,
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ContactAction.entries.forEach { action ->
                                if (action != ContactAction.VIDEO || actions.canVideoCall) {
                                    QuickAction(action, valuesFor(action).isNotEmpty()) {
                                        select(action)
                                    }
                                }
                            }
                        }
                    }
                    if (phoneNumbers.isNotEmpty()) {
                        item { SectionHeading(SharedR.string.phones) }
                        items(phoneNumbers, key = { it }) { number ->
                            val phone = phones.firstOrNull { it.number == number }
                            ProfileRow(
                                title = number,
                                subtitle = phone?.let {
                                    Phone.getTypeLabel(
                                        context.resources,
                                        it.type,
                                        it.label,
                                    ).toString()
                                },
                                icon = Icons.Rounded.Call,
                                onClick = { viewModel.onCall(number) },
                                end = {
                                    IconButton(onClick = { external.openSms(number) }) {
                                        Icon(
                                            Icons.AutoMirrored.Rounded.Message,
                                            stringResource(SharedR.string.action_message),
                                        )
                                    }
                                },
                            )
                        }
                    }
                    if (state.history.isNotEmpty()) {
                        item { SectionHeading(SharedR.string.recent_calls) }
                        items(state.history, key = { it.id }) { recent ->
                            val date = DateUtils.formatDateTime(
                                context,
                                recent.dateMillis,
                                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or
                                    DateUtils.FORMAT_ABBREV_MONTH,
                            )
                            val duration = recent.durationSeconds.takeIf {
                                it > 0
                            }?.let(DateUtils::formatElapsedTime)
                            ProfileRow(
                                title = recent.number,
                                subtitle = listOfNotNull(date, duration).joinToString(" · "),
                                icon = Icons.Rounded.Call,
                                end = { RecentTypeIcon(recent.type, contentDescription = null) },
                            )
                        }
                    }
                    item { SectionHeading(R.string.contact_settings) }
                    item {
                        ProfileRow(
                            title = stringResource(R.string.contact_ringtone),
                            subtitle = remember(contact.customRingtone) {
                                actions.ringtoneLabel(contact.customRingtone)
                            },
                            icon = Icons.Rounded.MusicNote,
                            onClick = {
                                actions.perform {
                                    ringtonePicker.launch(
                                        actions.ringtonePicker(contact.customRingtone),
                                    )
                                }
                            },
                        )
                    }
                    if (contact.lookupKey != null) {
                        item {
                            ProfileRow(
                                stringResource(
                                    R.string.contact_share,
                                ),
                                Icons.Rounded.Share,
                                onClick = {
                                    actions.share(contact)
                                },
                            )
                        }
                        if (actions.canPinShortcut) {
                            item {
                                ProfileRow(
                                    stringResource(
                                        R.string.contact_pin,
                                    ),
                                    Icons.AutoMirrored.Rounded.AddToHomeScreen,
                                    onClick = {
                                        actions.pinShortcut(contact, title)
                                    },
                                )
                            }
                        }
                    }
                    if (phoneNumbers.isNotEmpty()) {
                        item {
                            ProfileRow(
                                title = stringResource(R.string.contact_send_to_voicemail),
                                icon = Icons.Rounded.Voicemail,
                                onClick = {
                                    viewModel.onSetSendsToVoicemail(!contact.sendsToVoicemail)
                                },
                                end = {
                                    Switch(
                                        checked = contact.sendsToVoicemail,
                                        onCheckedChange = { viewModel.onSetSendsToVoicemail(it) },
                                    )
                                },
                            )
                        }
                    }
                    item {
                        ProfileRow(
                            title = stringResource(SharedR.string.action_delete),
                            icon = Icons.Rounded.DeleteOutline,
                            destructive = true,
                            onClick = { confirmDelete = true },
                        )
                    }
                }
            }
        }
    }

    selection?.let { action ->
        AlertDialog(
            onDismissRequest = { selection = null },
            title = { Text(stringResource(action.title)) },
            text = {
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    valuesFor(action).forEach { value ->
                        val phone = phones.firstOrNull { it.number == value }
                        ListItem(
                            title = value,
                            subtitle = phone?.let {
                                Phone.getTypeLabel(context.resources, it.type, it.label).toString()
                            },
                            onClick = {
                                selection = null
                                perform(action, value)
                            },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selection = null
                }) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }

    if (confirmDelete && contact != null) {
        ConfirmDialog(
            title = stringResource(SharedR.string.confirm_delete_contact_title),
            text = stringResource(SharedR.string.confirm_delete_contact),
            confirmLabel = stringResource(SharedR.string.action_delete),
            onConfirm = { viewModel.onDeleteConfirmed(onBack) },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun QuickAction(action: ContactAction, enabled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large
    val labelColor = if (enabled) colors.onSurface else colors.onSurfaceVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .widthIn(min = 68.dp)
            .clip(shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        Surface(
            shape = shape,
            color = if (enabled) colors.secondaryContainer else colors.surfaceContainer,
            contentColor = if (enabled) colors.onSecondaryContainer else colors.onSurfaceVariant,
            modifier = Modifier.size(width = 56.dp, height = 48.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(action.icon, contentDescription = null)
            }
        }
        Text(
            text = stringResource(action.title),
            style = MaterialTheme.typography.labelMedium,
            color = labelColor,
        )
    }
}

@Composable
private fun SectionHeading(@StringRes title: Int) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 8.dp, top = 14.dp, bottom = 2.dp),
    )
}

@Composable
private fun ProfileRow(
    title: String,
    icon: ImageVector,
    subtitle: String? = null,
    destructive: Boolean = false,
    onClick: (() -> Unit)? = null,
    end: @Composable (() -> Unit)? = null,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        ListItem(
            title = title,
            subtitle = subtitle,
            onClick = onClick,
            startContainer = {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (destructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            },
            endContainer = end,
        )
    }
}
