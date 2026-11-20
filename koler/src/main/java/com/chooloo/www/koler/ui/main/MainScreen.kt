package com.chooloo.www.koler.ui.main

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chooloo.www.chooloolib.R
import com.chooloo.www.chooloolib.ui.caller.CallerScreen
import com.chooloo.www.chooloolib.ui.caller.CallerTarget
import com.chooloo.www.chooloolib.ui.common.rememberExternalActions
import com.chooloo.www.chooloolib.ui.components.BottomSheet
import com.chooloo.www.chooloolib.ui.components.ConfirmDialog
import com.chooloo.www.chooloolib.ui.components.FloatingBubble
import com.chooloo.www.chooloolib.ui.components.Reveal
import com.chooloo.www.chooloolib.ui.contacts.ContactsScreen
import com.chooloo.www.chooloolib.ui.contacts.ContactsViewModel
import com.chooloo.www.chooloolib.ui.dialpad.DialpadScreen
import com.chooloo.www.chooloolib.ui.preferences.PreferencesScreen
import com.chooloo.www.chooloolib.ui.recents.RecentsScreen
import com.chooloo.www.koler.ui.call.CallActivity
import com.chooloo.www.koler.ui.contact.ContactProfileActivity
import com.chooloo.www.koler.ui.main.MainViewModel.Tab

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val externalActions = rememberExternalActions()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val ongoingCall by viewModel.ongoingCall.collectAsStateWithLifecycle()
    val tab = uiState.tab ?: return

    val contactsViewModel: ContactsViewModel = hiltViewModel()
    val selection by contactsViewModel.selection.collectAsStateWithLifecycle()
    val selecting = tab == Tab.CONTACTS && selection.ids.isNotEmpty()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val phoneSettings by viewModel.phoneSettings.collectAsStateWithLifecycle(initialValue = null)
    val leftHanded = phoneSettings?.leftHanded == true
    val callGate = rememberHeimdallCallGate()
    var setupOpen by rememberSaveable { mutableStateOf(false) }
    var setupDismissed by rememberSaveable { mutableStateOf(false) }
    if (setupOpen || (phoneSettings?.phoneSetupCompleted == false && !setupDismissed)) {
        PhoneSetup(skipWhenReady = !setupOpen) {
            setupOpen = false
            setupDismissed = true
            viewModel.completePhoneSetup()
        }
    }

    DialerHomeScaffold(
        title = if (selecting) {
            stringResource(R.string.contacts_selected_count, selection.ids.size)
        } else {
            stringResource(if (tab == Tab.RECENTS) R.string.recents else R.string.contacts)
        },
        selectionActions = if (selecting) {
            {
                IconButton(onClick = { confirmDelete = true }, enabled = !selection.deleting) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        stringResource(R.string.contacts_delete_selected),
                    )
                }
                IconButton(
                    onClick = contactsViewModel::clearSelection,
                    enabled = !selection.deleting,
                ) {
                    Icon(Icons.Rounded.Close, stringResource(R.string.contacts_clear_selection))
                }
            }
        } else {
            null
        },
        isSearching = uiState.isSearching,
        searchText = uiState.searchText,
        searchHintRes = if (tab == Tab.RECENTS) {
            R.string.hint_search_recents
        } else {
            R.string.hint_search_contacts
        },
        onSearchOpen = viewModel.search::open,
        onSearchTextChange = viewModel.search::changeText,
        onSearchClose = viewModel.search::close,
        onSettingsOpen = viewModel::onSettingsOpen,
        bottomBarContent = {
            Reveal(
                visible = tab == Tab.CONTACTS && !selecting,
                modifier = Modifier.align(
                    if (leftHanded) AbsoluteAlignment.CenterLeft else AbsoluteAlignment.CenterRight,
                ),
            ) {
                FloatingBubble(
                    icon = Icons.Rounded.PersonAdd,
                    contentDescription = stringResource(R.string.cd_add_contact),
                    onClick = { externalActions.createContact() },
                )
            }
            MainNavigationBar(
                selectedTab = tab,
                onTabSelected = {
                    if (!selection.deleting) {
                        if (it != tab) contactsViewModel.clearSelection()
                        viewModel.onTabSelected(it)
                    }
                },
                onDialpadClick = viewModel::onDialpadOpen,
                modifier = Modifier.align(Alignment.Center),
            )
        },
    ) { listPadding ->
        ongoingCall?.let {
            OngoingCallBanner(
                ongoingCall = it,
                onClick = { context.startActivity(Intent(context, CallActivity::class.java)) },
            )
        }
        if (selecting && selection.deleteFailed) {
            Text(
                stringResource(R.string.contacts_delete_failed),
                color = MaterialTheme.colorScheme.error,
            )
        }
        when (tab) {
            Tab.RECENTS -> RecentsScreen(
                filter = uiState.searchText,
                contentPadding = listPadding,
                onItemClick = {
                    viewModel.onCallerSelected(CallerTarget.RecentTarget(it.latest.id))
                },
                onItemLongClick = { externalActions.copyToClipboard(it.latest.number) },
            )

            Tab.CONTACTS -> ContactsScreen(
                viewModel = contactsViewModel,
                selectedIds = selection.ids,
                alphabetIndexOnLeft = leftHanded,
                circularLetterPreview = phoneSettings?.circularLetterPreview == true,
                filter = uiState.searchText,
                contentPadding = listPadding,
                onItemClick = {
                    if (selecting) {
                        contactsViewModel.toggleSelected(it.id)
                    } else {
                        context.startActivity(ContactProfileActivity.intent(context, it.id))
                    }
                },
                onItemLongClick = {
                    focusManager.clearFocus()
                    contactsViewModel.toggleSelected(it.id)
                },
            )
        }
    }

    BackHandler(enabled = selecting) { contactsViewModel.clearSelection() }
    if (confirmDelete && selecting) {
        ConfirmDialog(
            title = pluralStringResource(
                R.plurals.contacts_delete_count,
                selection.ids.size,
                selection.ids.size,
            ),
            text = stringResource(R.string.contacts_delete_confirmation),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = contactsViewModel::deleteSelection,
            onDismiss = { confirmDelete = false },
        )
    }

    BottomSheet(
        visible = uiState.isSettingsOpen,
        skipPartiallyExpanded = true,
        onDismiss = viewModel::onSettingsClose,
    ) {
        PreferencesScreen(showDialerPreferences = true, onPhoneSetup = {
            viewModel.onSettingsClose()
            setupOpen = true
        })
    }

    BottomSheet(
        visible = uiState.isDialpadOpen,
        skipPartiallyExpanded = true,
        onDismiss = viewModel::onDialpadClose,
        showDragHandle = false,
        horizontalPadding = 0.dp,
    ) {
        DialpadScreen(
            initialNumber = uiState.dialNumber,
            leftHanded = leftHanded,
            callGate = callGate,
            suggestions = { digits, onCall ->
                DialpadMatches(
                    digits = digits,
                    onCall = onCall,
                    onOpenContact = { id ->
                        context.startActivity(ContactProfileActivity.intent(context, id))
                    },
                )
            },
        )
    }

    BottomSheet(visible = uiState.selectedCaller != null, onDismiss = viewModel::onCallerDismiss) {
        uiState.selectedCaller?.let {
            CallerScreen(target = it, onDismiss = viewModel::onCallerDismiss, callGate = callGate)
        }
    }
}
