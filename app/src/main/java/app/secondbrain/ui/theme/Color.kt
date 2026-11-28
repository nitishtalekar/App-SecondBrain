package app.secondbrain.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import app.secondbrain.data.Accent

// Black and white, with one accent the user chooses. The accent fills the tertiary roles,
// which mark anything still waiting on the user.

private val MonoLight: ColorScheme = lightColorScheme(
    primary = Color(0xFF111111),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0E0E0),
    onPrimaryContainer = Color(0xFF111111),
    secondary = Color(0xFF525252),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0E0E0),
    onSecondaryContainer = Color(0xFF111111),
    background = Color(0xFFFCFCFC),
    onBackground = Color(0xFF111111),
    surface = Color(0xFFFCFCFC),
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFE7E7E7),
    onSurfaceVariant = Color(0xFF525252),
    surfaceTint = Color(0xFF111111),
    outline = Color(0xFF767676),
    outlineVariant = Color(0xFFDCDCDC),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F2F2),
    surfaceContainer = Color(0xFFEBEBEB),
    surfaceContainerHigh = Color(0xFFE7E7E7),
    surfaceContainerHighest = Color(0xFFE0E0E0),
    inverseSurface = Color(0xFF262626),
    inverseOnSurface = Color(0xFFF2F2F2),
    inversePrimary = Color(0xFFF5F5F5),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

private val MonoDark: ColorScheme = darkColorScheme(
    primary = Color(0xFFF5F5F5),
    onPrimary = Color(0xFF111111),
    primaryContainer = Color(0xFF333333),
    onPrimaryContainer = Color(0xFFF5F5F5),
    secondary = Color(0xFFABABAB),
    onSecondary = Color(0xFF1C1C1C),
    secondaryContainer = Color(0xFF333333),
    onSecondaryContainer = Color(0xFFF5F5F5),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF5F5F5),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFABABAB),
    surfaceTint = Color(0xFFF5F5F5),
    outline = Color(0xFF7A7A7A),
    outlineVariant = Color(0xFF2C2C2C),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF111111),
    surfaceContainer = Color(0xFF161616),
    surfaceContainerHigh = Color(0xFF1C1C1C),
    surfaceContainerHighest = Color(0xFF242424),
    inverseSurface = Color(0xFFE7E7E7),
    inverseOnSurface = Color(0xFF1C1C1C),
    inversePrimary = Color(0xFF111111),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
)

fun appColorScheme(dark: Boolean, accent: Accent): ColorScheme {
    val base = if (dark) MonoDark else MonoLight
    val tones = if (dark) accent.dark else accent.light
    return base.copy(
        tertiary = Color(tones.color),
        onTertiary = if (dark) Color.Black else Color.White,
        tertiaryContainer = Color(tones.container),
        onTertiaryContainer = Color(tones.onContainer),
    )
}

/** Colours that tell clusters apart on the globe. Never used as text. */
private val LightClusterColors = listOf(
    Color(0xFFC4503A), Color(0xFF9C6B00), Color(0xFF2F7D4A), Color(0xFF007C85), Color(0xFF1C6FC2),
    Color(0xFF5B52C8), Color(0xFF9440A8), Color(0xFFC03A6E), Color(0xFF6B7A1F), Color(0xFF9A5530),
)

private val DarkClusterColors = listOf(
    Color(0xFFFF8F78), Color(0xFFEDC16A), Color(0xFF7FD49A), Color(0xFF5FD3DA), Color(0xFF74B6FF),
    Color(0xFFA8A0FF), Color(0xFFD993EA), Color(0xFFFF8DB6), Color(0xFFBFD063), Color(0xFFE5A47C),
)

val LocalClusterColors = staticCompositionLocalOf { LightClusterColors }

fun clusterColors(dark: Boolean): List<Color> = if (dark) DarkClusterColors else LightClusterColors

@Composable
fun clusterColor(index: Int): Color {
    val colors = LocalClusterColors.current
    return colors[index % colors.size]
}
