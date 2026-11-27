package com.chooloo.www.chooloolib.ui.theme

import androidx.compose.animation.core.CubicBezierEasing

object Motion {
    /** Strong ease-out for things entering or leaving: fast start, so the response feels immediate. */
    val EaseOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

    /** Search reveal and similar small surface changes; under the 300ms UI ceiling. */
    const val SURFACE_MILLIS = 220
}
