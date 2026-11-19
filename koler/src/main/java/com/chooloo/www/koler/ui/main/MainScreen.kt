package com.chooloo.www.koler.ui.main

import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chooloo.www.chooloolib.R
import com.chooloo.www.chooloolib.ui.caller.CallerScreen
import com.chooloo.www.chooloolib.ui.caller.CallerTarget
import com.chooloo.www.chooloolib.ui.common.rememberExternalActions
import com.chooloo.www.chooloolib.ui.components.BottomSheet
import com.chooloo.www.chooloolib.ui.components.FloatingBubble
import com.chooloo.www.chooloolib.ui.components.Reveal
import com.chooloo.www.chooloolib.ui.contacts.ContactsScreen
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
        title = stringResource(if (tab == Tab.RECENTS) R.string.recents else R.string.contacts),
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
                visible = tab == Tab.CONTACTS,
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
                onTabSelected = viewModel::onTabSelected,
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
                alphabetIndexOnLeft = leftHanded,
                filter = uiState.searchText,
                contentPadding = listPadding,
                onItemClick = {
                    context.startActivity(ContactProfileActivity.intent(context, it.id))
                },
            )
        }
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
