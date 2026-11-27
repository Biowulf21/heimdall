package com.chooloo.www.chooloolib.domain.enum

/** Call-type filter for the recents list, picked from the dialer's drawer. */
enum class RecentFilter {
    ALL,
    MISSED,
    INCOMING,
    OUTGOING,
    ;

    fun matches(type: RecentType): Boolean = when (this) {
        ALL -> true
        MISSED -> type == RecentType.MISSED
        INCOMING -> type == RecentType.INCOMING
        OUTGOING -> type == RecentType.OUTGOING
    }
}
