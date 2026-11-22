package app.secondbrain.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// "Pine and mist": cool green-grey neutrals, one deep pine accent, a single amber for warmth.

val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF1B6B66),
    onPrimary = Color(0xFFF2FBFA),
    primaryContainer = Color(0xFFCDE9E5),
    onPrimaryContainer = Color(0xFF08302D),
    secondary = Color(0xFF4A5857),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE1E9E7),
    onSecondaryContainer = Color(0xFF1B2625),
    tertiary = Color(0xFF8F5A0F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF6E2C0),
    onTertiaryContainer = Color(0xFF3A2200),
    background = Color(0xFFF3F6F5),
    onBackground = Color(0xFF141C1B),
    surface = Color(0xFFF3F6F5),
    onSurface = Color(0xFF141C1B),
    surfaceVariant = Color(0xFFE1E9E7),
    onSurfaceVariant = Color(0xFF4A5857),
    surfaceTint = Color(0xFF1B6B66),
    outline = Color(0xFF7A8987),
    outlineVariant = Color(0xFFD3DDDB),
    surfaceContainerLowest = Color(0xFFFAFCFB),
    surfaceContainerLow = Color(0xFFEDF2F1),
    surfaceContainer = Color(0xFFE7EDEC),
    surfaceContainerHigh = Color(0xFFE0E7E6),
    surfaceContainerHighest = Color(0xFFD9E1DF),
    inverseSurface = Color(0xFF242E2D),
    inverseOnSurface = Color(0xFFEAF1F0),
    inversePrimary = Color(0xFF6FC7BF),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF6FC7BF),
    onPrimary = Color(0xFF00302C),
    primaryContainer = Color(0xFF134A45),
    onPrimaryContainer = Color(0xFFC9F0EC),
    secondary = Color(0xFFA5B4B2),
    onSecondary = Color(0xFF1B2625),
    secondaryContainer = Color(0xFF243230),
    onSecondaryContainer = Color(0xFFD8E5E3),
    tertiary = Color(0xFFE0B15E),
    onTertiary = Color(0xFF3A2200),
    tertiaryContainer = Color(0xFF5A3C0A),
    onTertiaryContainer = Color(0xFFF6E2C0),
    background = Color(0xFF0D1413),
    onBackground = Color(0xFFE3EBEA),
    surface = Color(0xFF0D1413),
    onSurface = Color(0xFFE3EBEA),
    surfaceVariant = Color(0xFF243230),
    onSurfaceVariant = Color(0xFFA5B4B2),
    surfaceTint = Color(0xFF6FC7BF),
    outline = Color(0xFF6D7D7B),
    outlineVariant = Color(0xFF2A3736),
    surfaceContainerLowest = Color(0xFF091010),
    surfaceContainerLow = Color(0xFF121A19),
    surfaceContainer = Color(0xFF162120),
    surfaceContainerHigh = Color(0xFF1E2B2A),
    surfaceContainerHighest = Color(0xFF263534),
    inverseSurface = Color(0xFFE3EBEA),
    inverseOnSurface = Color(0xFF1F2A29),
    inversePrimary = Color(0xFF1B6B66),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
)

fun parseHexColor(hex: String, fallback: Color = Color(0xFF7A8987)): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)
