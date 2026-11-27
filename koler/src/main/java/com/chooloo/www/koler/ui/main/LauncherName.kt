package com.chooloo.www.koler.ui.main

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/** Switches the home-screen label between "Phone" and "Heimdall" via the two launcher aliases. */
object LauncherName {
    internal const val DEFAULT_ALIAS = "com.chooloo.www.koler.LauncherDefault"
    internal const val BRAND_ALIAS = "com.chooloo.www.koler.LauncherBrand"

    fun isBrand(context: Context): Boolean = context.packageManager
        .getComponentEnabledSetting(ComponentName(context.packageName, BRAND_ALIAS)) ==
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED

    fun apply(context: Context, brand: Boolean) {
        // Enable the new alias before disabling the old one so there is never zero launcher entries.
        componentStates(brand).entries
            .sortedByDescending { it.value == PackageManager.COMPONENT_ENABLED_STATE_ENABLED }
            .forEach { (alias, state) ->
                context.packageManager.setComponentEnabledSetting(
                    ComponentName(context.packageName, alias),
                    state,
                    PackageManager.DONT_KILL_APP,
                )
            }
    }

    internal fun componentStates(brand: Boolean): Map<String, Int> = mapOf(
        BRAND_ALIAS to enabledIf(brand),
        DEFAULT_ALIAS to enabledIf(!brand),
    )

    private fun enabledIf(enabled: Boolean) = if (enabled) {
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    } else {
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }
}
