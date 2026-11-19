package com.chooloo.www.koler.ui.main

import android.Manifest.permission.CALL_PHONE
import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.READ_CALL_LOG
import android.Manifest.permission.READ_CONTACTS
import android.Manifest.permission.READ_PHONE_STATE
import android.Manifest.permission.WRITE_CALL_LOG
import android.Manifest.permission.WRITE_CONTACTS
import android.app.NotificationManager
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.chooloo.www.koler.R

@Composable
internal fun PhoneSetup(skipWhenReady: Boolean, onDone: () -> Unit) {
    val context = LocalContext.current
    val role = context.phoneRole()
    var requested by rememberSaveable { mutableStateOf(false) }
    var missingFullscreen by rememberSaveable { mutableStateOf(context.needsFullscreenAccess()) }
    var remaining by rememberSaveable { mutableStateOf(false) }

    fun finishRequests() {
        requested = true
        missingFullscreen = context.needsFullscreenAccess()
        remaining = context.missingPhonePermissions().isNotEmpty() ||
            role?.isRoleHeld(RoleManager.ROLE_DIALER) == false
        if (!missingFullscreen && !remaining) onDone()
    }

    val permissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { finishRequests() }
    val pickRole =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val missing = context.missingPhonePermissions()
            if (missing.isEmpty()) finishRequests() else permissions.launch(missing)
        }
    val fullscreen =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            finishRequests()
        }

    LaunchedEffect(Unit) {
        if (skipWhenReady && context.missingPhonePermissions().isEmpty() &&
            role?.isRoleHeld(RoleManager.ROLE_DIALER) != false && !missingFullscreen
        ) {
            onDone()
        }
    }

    val message = when {
        !requested -> R.string.phone_setup_intro
        missingFullscreen -> R.string.phone_setup_fullscreen
        else -> R.string.phone_setup_limited
    }
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(stringResource(R.string.phone_setup_title)) },
        text = { Text(stringResource(message)) },
        confirmButton = {
            when {
                !requested -> TextButton(onClick = {
                    val missing = context.missingPhonePermissions()
                    when {
                        role != null && !role.isRoleHeld(RoleManager.ROLE_DIALER) ->
                            pickRole.launch(role.createRequestRoleIntent(RoleManager.ROLE_DIALER))

                        missing.isNotEmpty() -> permissions.launch(missing)

                        else -> finishRequests()
                    }
                }) { Text(stringResource(R.string.phone_setup_continue)) }

                missingFullscreen && Build.VERSION.SDK_INT >= 34 -> TextButton(onClick = {
                    fullscreen.launch(
                        Intent(
                            Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                            Uri.fromParts("package", context.packageName, null),
                        ),
                    )
                }) { Text(stringResource(R.string.phone_setup_open_settings)) }

                remaining -> TextButton(onClick = {
                    context.openPhoneSetting(context.appSettings())
                }) {
                    Text(stringResource(R.string.phone_setup_open_settings))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDone) {
                Text(
                    stringResource(
                        if (requested) R.string.phone_setup_done else R.string.phone_setup_skip,
                    ),
                )
            }
        },
    )
}

private fun Context.needsFullscreenAccess(): Boolean = Build.VERSION.SDK_INT >= 34 &&
    getSystemService(NotificationManager::class.java)?.canUseFullScreenIntent() == false

private fun Context.missingPhonePermissions(): Array<String> = buildList {
    addAll(
        listOf(
            CALL_PHONE,
            READ_PHONE_STATE,
            READ_CONTACTS,
            WRITE_CONTACTS,
            READ_CALL_LOG,
            WRITE_CALL_LOG,
        ),
    )
    if (Build.VERSION.SDK_INT >= 33) add(POST_NOTIFICATIONS)
}.filter { checkSelfPermission(it) != PERMISSION_GRANTED }.toTypedArray()
