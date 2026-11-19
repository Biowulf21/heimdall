package com.chooloo.www.koler.ui.main

import org.junit.Assert.assertEquals
import org.junit.Test

class CallRouteTest {
    @Test
    fun regularCallsRequireDefaultRoleAndPhonePermission() {
        assertEquals(CallRoute.ROLE_PICKER, callRoute(false, true, false, true))
        assertEquals(CallRoute.PHONE_PERMISSION, callRoute(false, true, true, false))
        assertEquals(CallRoute.PLACE_CALL, callRoute(false, true, true, true))
        assertEquals(CallRoute.UNAVAILABLE, callRoute(false, false, false, true))
    }

    @Test
    fun emergencyRoutesAlwaysBypassPhoneAppPicker() {
        assertEquals(CallRoute.EMERGENCY_DIALER, callRoute(true, true, false, true))
        assertEquals(CallRoute.EMERGENCY_DIALER, callRoute(true, true, true, false))
        assertEquals(CallRoute.PLACE_CALL, callRoute(true, true, true, true))
    }
}
