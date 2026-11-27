package com.chooloo.www.koler.ui.main

import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherNameTest {
    @Test
    fun exactlyOneAliasIsEnabledForEitherChoice() {
        for (brand in listOf(true, false)) {
            val states = LauncherName.componentStates(brand)
            assertEquals(1, states.values.count { it == COMPONENT_ENABLED_STATE_ENABLED })
            assertEquals(1, states.values.count { it == COMPONENT_ENABLED_STATE_DISABLED })
            val enabled = if (brand) LauncherName.BRAND_ALIAS else LauncherName.DEFAULT_ALIAS
            assertEquals(COMPONENT_ENABLED_STATE_ENABLED, states[enabled])
        }
    }
}
