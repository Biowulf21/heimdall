package com.chooloo.www.chooloolib.domain.enum

import org.junit.Assert.assertEquals
import org.junit.Test

class RecentFilterTest {
    @Test
    fun `all keeps every type and the others keep only their own`() {
        assertEquals(RecentType.entries, RecentType.entries.filter(RecentFilter.ALL::matches))
        assertEquals(
            listOf(RecentType.MISSED),
            RecentType.entries.filter(RecentFilter.MISSED::matches),
        )
        assertEquals(
            listOf(RecentType.INCOMING),
            RecentType.entries.filter(RecentFilter.INCOMING::matches),
        )
        assertEquals(
            listOf(RecentType.OUTGOING),
            RecentType.entries.filter(RecentFilter.OUTGOING::matches),
        )
    }
}
