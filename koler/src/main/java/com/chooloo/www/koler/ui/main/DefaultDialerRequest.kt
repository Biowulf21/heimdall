package com.chooloo.www.koler.ui.main

import android.Manifest.permission.CALL_PHONE
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.provider.Settings
import android.telecom.TelecomManager
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.chooloo.www.chooloolib.ui.common.CallGate
import com.chooloo.www.chooloolib.ui.components.ConfirmDialog
import com.chooloo.www.koler.R

internal fun Context.phoneRole(): RoleManager? = getSystemService(RoleManager::class.java)
    ?.takeIf { it.isRoleAvailable(RoleManager.ROLE_DIALER) }

internal fun Context.appSettings() = Intent(
    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
    Uri.fromParts("package", packageName, null),
)

internal fun Context.openPhoneSetting(intent: Intent) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, R.string.phone_setup_unavailable, Toast.LENGTH_SHORT).show()
    } catch (_: SecurityException) {
        Toast.makeText(this, R.string.phone_setup_unavailable, Toast.LENGTH_SHORT).show()
    }
}

@Composable
internal fun rememberHeimdallCallGate(): CallGate {
    val context = LocalContext.current
    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {}
    var offerPermissions by rememberSaveable { mutableStateOf(false) }

    if (offerPermissions) {
        ConfirmDialog(
            title = stringResource(R.string.phone_setup_title),
            text = stringResource(R.string.phone_setup_call_permission),
            confirmLabel = stringResource(R.string.phone_setup_open_settings),
            onConfirm = { context.openPhoneSetting(context.appSettings()) },
            onDismiss = { offerPermissions = false },
        )
    }

    return { number, placeCall ->
        val hasCallPermission = context.checkSelfPermission(CALL_PHONE) == PERMISSION_GRANTED
        val role = context.phoneRole()
        val route = callRoute(
            emergency = number?.let { context.isEmergencyNumber(it) } == true,
            roleAvailable = role != null,
            defaultDialer = role?.isRoleHeld(RoleManager.ROLE_DIALER) == true,
            canCall = hasCallPermission,
        )
        when (route) {
            CallRoute.PLACE_CALL -> placeCall()

            CallRoute.EMERGENCY_DIALER -> number?.let { context.openEmergencyDialer(it) }

            CallRoute.ROLE_PICKER -> role?.let {
                roleLauncher.launch(it.createRequestRoleIntent(RoleManager.ROLE_DIALER))
            }

            CallRoute.PHONE_PERMISSION -> offerPermissions = true

            CallRoute.UNAVAILABLE ->
                Toast.makeText(context, R.string.phone_setup_unavailable, Toast.LENGTH_SHORT).show()
        }
    }
}

internal enum class CallRoute {
    PLACE_CALL,
    EMERGENCY_DIALER,
    ROLE_PICKER,
    PHONE_PERMISSION,
    UNAVAILABLE,
}

internal fun callRoute(
    emergency: Boolean,
    roleAvailable: Boolean,
    defaultDialer: Boolean,
    canCall: Boolean,
): CallRoute = when {
    emergency && (!defaultDialer || !canCall) -> CallRoute.EMERGENCY_DIALER
    emergency -> CallRoute.PLACE_CALL
    !roleAvailable -> CallRoute.UNAVAILABLE
    !defaultDialer -> CallRoute.ROLE_PICKER
    !canCall -> CallRoute.PHONE_PERMISSION
    else -> CallRoute.PLACE_CALL
}

private fun Context.isEmergencyNumber(number: String): Boolean = runCatching {
    getSystemService(TelephonyManager::class.java)
        ?.isEmergencyNumber(PhoneNumberUtils.stripSeparators(number)) == true
}.getOrDefault(false)

private fun Context.openEmergencyDialer(number: String) {
    val systemDialer = getSystemService(TelecomManager::class.java)?.systemDialerPackage
    if (systemDialer == null) {
        Toast.makeText(this, R.string.phone_setup_unavailable, Toast.LENGTH_SHORT).show()
        return
    }
    openPhoneSetting(
        Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null)).setPackage(systemDialer),
    )
}
