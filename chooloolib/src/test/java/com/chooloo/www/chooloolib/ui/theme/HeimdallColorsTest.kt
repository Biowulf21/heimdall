package com.chooloo.www.chooloolib.ui.theme

import com.chooloo.www.chooloolib.ui.preferences.parseAccentHex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HeimdallColorsTest {
    @Test
    fun acceptsOnlyOpaqueRgbHex() {
        assertEquals(0x42A5F5, parseAccentHex(" #42a5f5 "))
        assertNull(parseAccentHex("#42A"))
        assertNull(parseAccentHex("#FF42A5F5"))
        assertNull(parseAccentHex("#42A5ZZ"))
    }

    @Test
    fun customAccentRemainsLegibleAcrossLightAndDarkSurfaces() {
        for (dark in listOf(false, true)) {
            for (accent in listOf(0x000000, 0xFFFFFF, 0x42A5F5, 0x777777, 0xEE1111)) {
                val colors = heimdallColors(accent, dark)
                assertTrue(contrastRatio(colors.primary, colors.surfaceContainerHighest) >= 4.5f)
                assertTrue(
                    contrastRatio(colors.onPrimaryContainer, colors.primaryContainer) >= 4.5f,
                )
            }
        }
    }
}
