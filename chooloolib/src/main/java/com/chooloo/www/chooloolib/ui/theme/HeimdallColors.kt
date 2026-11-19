package com.chooloo.www.chooloolib.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val typography = Typography()
internal val HeimdallTypography = typography.copy(
    headlineSmall = typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = typography.titleMedium.copy(fontWeight = FontWeight.Medium),
)
internal val HeimdallShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

internal fun heimdallColors(rgb: Int, dark: Boolean): androidx.compose.material3.ColorScheme {
    val accent = Color(0xFF000000.toInt() or rgb)
    val background = Color(if (dark) 0xFF171E24 else 0xFFF8FAFC)
    val surface = Color(if (dark) 0xFF1D252D else 0xFFFFFFFF)
    val container = Color(if (dark) 0xFF252F39 else 0xFFEAF0F5)
    val high = Color(if (dark) 0xFF2C3843 else 0xFFDFE8F1)
    val highest = Color(if (dark) 0xFF34414E else 0xFFD8E3EE)
    val foreground = Color(if (dark) 0xFFEAF1F8 else 0xFF17212B)
    val muted = Color(if (dark) 0xFFB7C5D3 else 0xFF495B6C)
    val softAccent = lerp(container, accent, if (dark) 0.20f else 0.12f)
    val primary = readableAccent(accent, highest, foreground)
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = accentInk(primary),
        primaryContainer = accent,
        onPrimaryContainer = accentInk(accent),
        inversePrimary = readableAccent(accent, foreground, background),
        secondary = primary,
        onSecondary = accentInk(primary),
        secondaryContainer = softAccent,
        onSecondaryContainer = foreground,
        tertiary = primary,
        onTertiary = accentInk(primary),
        tertiaryContainer = softAccent,
        onTertiaryContainer = foreground,
        background = background,
        onBackground = foreground,
        surface = surface,
        onSurface = foreground,
        surfaceVariant = container,
        onSurfaceVariant = muted,
        surfaceTint = primary,
        surfaceDim = background,
        surfaceBright = surface,
        surfaceContainerLowest = background,
        surfaceContainerLow = surface,
        surfaceContainer = container,
        surfaceContainerHigh = high,
        surfaceContainerHighest = highest,
        outline = muted,
        outlineVariant = high,
    )
}

internal fun accentInk(accent: Color): Color =
    if (contrastRatio(accent, Color.Black) >= contrastRatio(accent, Color.White)) {
        Color.Black
    } else {
        Color.White
    }

private fun readableAccent(accent: Color, background: Color, foreground: Color): Color =
    (0..20).asSequence()
        .map { lerp(accent, foreground, it / 20f) }
        .first { contrastRatio(it, background) >= 4.5f }

internal fun contrastRatio(a: Color, b: Color): Float {
    val lightA = a.luminance() + 0.05f
    val lightB = b.luminance() + 0.05f
    return maxOf(lightA, lightB) / minOf(lightA, lightB)
}
