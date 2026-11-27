package com.chooloo.www.chooloolib.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.chooloo.www.chooloolib.domain.enum.DesignStyle
import com.chooloo.www.chooloolib.domain.enum.ThemeMode

val LocalDesignStyle = staticCompositionLocalOf { DesignStyle.CLASSIC }

@Composable
fun ChoolooTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentRgb: Int? = null,
    designStyle: DesignStyle = DesignStyle.CLASSIC,
    // false lets a screen own its insets, e.g. so a drawer can reach the screen edge.
    padded: Boolean = true,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val colorScheme = when {
        accentRgb != null -> heimdallColors(accentRgb, isDark)

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        isDark -> darkColorScheme()

        else -> lightColorScheme()
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(LocalDesignStyle provides designStyle) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = if (accentRgb == null) ChoolooTypography else HeimdallTypography,
            shapes = if (accentRgb == null) ChoolooShapes else HeimdallShapes,
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Box(
                    modifier = if (padded) Modifier.screenPadding() else Modifier,
                ) {
                    content()
                }
            }
        }
    }
}

/** The safe-area and page margins every screen sits inside. */
fun Modifier.screenPadding(): Modifier = safeDrawingPadding()
    .padding(start = Spacing.Page, end = Spacing.Page, top = Spacing.Medium)
