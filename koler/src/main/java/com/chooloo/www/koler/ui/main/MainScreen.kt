package com.chooloo.www.koler.ui.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.TelecomManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.chooloo.www.chooloolib.domain.enum.RecentFilter
import com.chooloo.www.chooloolib.ui.caller.CallerScreen
import com.chooloo.www.chooloolib.ui.caller.CallerTarget
import com.chooloo.www.chooloolib.ui.common.rememberExternalActions
import com.chooloo.www.chooloolib.ui.components.BottomSheet
import com.chooloo.www.chooloolib.ui.components.ConfirmDialog
import com.chooloo.www.chooloolib.ui.contacts.ContactsScreen
import com.chooloo.www.chooloolib.ui.contacts.ContactsViewModel
import com.chooloo.www.chooloolib.ui.dialpad.DialpadScreen
import com.chooloo.www.chooloolib.ui.recents.RecentsScreen
import com.chooloo.www.chooloolib.ui.theme.Spacing
import com.chooloo.www.koler.ui.call.CallActivity
import com.chooloo.www.koler.ui.contact.ContactProfileActivity
import com.chooloo.www.koler.ui.main.MainViewModel.Tab
import com.chooloo.www.koler.ui.settings.SettingsActivity

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
        selectionTitle = if (selecting) {
            stringResource(R.string.contacts_selected_count, selection.ids.size)
        } else {
            null
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
        drawerContent = { closeDrawer ->
            val bugReportUrl = stringResource(R.string.app_bug_reporting_url)
            val donationUrl = stringResource(R.string.donation_link)
            fun run(action: () -> Unit): () -> Unit = {
                closeDrawer()
                action()
            }
            HomeDrawer(
                recentFilter = uiState.recentFilter.takeIf { tab == Tab.RECENTS },
                favoritesOnly = uiState.favoritesOnly && tab == Tab.CONTACTS,
                onRecentFilter = {
                    closeDrawer()
                    if (!selection.deleting) {
                        contactsViewModel.clearSelection()
                        viewModel.onRecentFilter(it)
                    }
                },
                onFavorites = run {
                    if (!selection.deleting) viewModel.onFavoritesOnly(true)
                },
                onBlockedNumbers = run(externalActions::openManageBlockedNumbers),
                onVoicemail = run { callGate(null) { callVoicemail(context) } },
                onSettings = run {
                    context.startActivity(Intent(context, SettingsActivity::class.java))
                },
                onPhoneSetup = run { setupOpen = true },
                onHelp = run { externalActions.openUrl(bugReportUrl) },
                onDonate = run { externalActions.openUrl(donationUrl) },
            )
        },
        bottomBarContent = {
            MainNavigationBar(
                selectedTab = tab,
                onTabSelected = {
                    if (!selection.deleting) {
                        if (it != tab) contactsViewModel.clearSelection()
                        viewModel.onTabSelected(it)
                    }
                },
                isPrimaryVisible = !selecting,
                leftHanded = leftHanded,
                onDialpadClick = viewModel::onDialpadOpen,
                onAddContactClick = { externalActions.createContact() },
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
        val activeFilter = when {
            tab == Tab.RECENTS && uiState.recentFilter != RecentFilter.ALL ->
                stringResource(uiState.recentFilter.labelRes)

            tab == Tab.CONTACTS && uiState.favoritesOnly -> stringResource(R.string.favourites)

            else -> null
        }
        activeFilter?.let {
            InputChip(
                selected = true,
                onClick = {
                    viewModel.onRecentFilter(RecentFilter.ALL)
                    viewModel.onFavoritesOnly(false)
                },
                label = { Text(it) },
                trailingIcon = {
                    Icon(
                        Icons.Rounded.Close,
                        stringResource(R.string.cd_clear_filter),
                        Modifier.size(InputChipDefaults.IconSize),
                    )
                },
                modifier = Modifier.padding(start = Spacing.Small),
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
                typeFilter = uiState.recentFilter,
                contentPadding = listPadding,
                onItemClick = {
                    viewModel.onCallerSelected(CallerTarget.RecentTarget(it.latest.id))
                },
                onItemLongClick = { externalActions.copyToClipboard(it.latest.number) },
            )

            Tab.CONTACTS -> ContactsScreen(
                viewModel = contactsViewModel,
                selectedIds = selection.ids,
                alphabetIndexOnLeft = leftHanded.takeUnless { uiState.isSearching },
                circularLetterPreview = phoneSettings?.circularLetterPreview == true,
                favoritesOnly = uiState.favoritesOnly,
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

private val RecentFilter.labelRes
    get() = when (this) {
        RecentFilter.ALL -> R.string.filter_all_calls
        RecentFilter.MISSED -> R.string.filter_missed
        RecentFilter.INCOMING -> R.string.filter_incoming
        RecentFilter.OUTGOING -> R.string.filter_outgoing
    }

/** Telecom shows its own prompt when the SIM has no voicemail number set. */
@SuppressLint("MissingPermission") // The call gate only runs this once CALL_PHONE is granted.
private fun callVoicemail(context: Context) {
    context.getSystemService(TelecomManager::class.java)
        ?.placeCall(Uri.fromParts(PhoneAccount.SCHEME_VOICEMAIL, "", null), Bundle())
}
