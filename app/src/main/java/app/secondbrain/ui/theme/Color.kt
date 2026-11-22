package app.secondbrain.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFFA8452B),
    onPrimary = Color(0xFFFFF8F3),
    primaryContainer = Color(0xFFF2DDD3),
    onPrimaryContainer = Color(0xFF4A1A0C),
    secondary = Color(0xFF5F594E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEAE5DA),
    onSecondaryContainer = Color(0xFF2E2A23),
    tertiary = Color(0xFF5F594E),
    background = Color(0xFFF7F4EE),
    onBackground = Color(0xFF1F1C17),
    surface = Color(0xFFF7F4EE),
    onSurface = Color(0xFF1F1C17),
    surfaceVariant = Color(0xFFEAE5DA),
    onSurfaceVariant = Color(0xFF5F594E),
    surfaceTint = Color(0xFFA8452B),
    outline = Color(0xFF8A8376),
    outlineVariant = Color(0xFFDDD7CA),
    surfaceContainerLowest = Color(0xFFFCFAF6),
    surfaceContainerLow = Color(0xFFF2EFE8),
    surfaceContainer = Color(0xFFEDE9E0),
    surfaceContainerHigh = Color(0xFFE7E2D7),
    surfaceContainerHighest = Color(0xFFE0DACD),
    inverseSurface = Color(0xFF2E2A24),
    inverseOnSurface = Color(0xFFF4EFE6),
    inversePrimary = Color(0xFFE7926F),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFFE7926F),
    onPrimary = Color(0xFF3A1407),
    primaryContainer = Color(0xFF5A2A18),
    onPrimaryContainer = Color(0xFFFADBCB),
    secondary = Color(0xFFB1A999),
    onSecondary = Color(0xFF2A261F),
    secondaryContainer = Color(0xFF2C2822),
    onSecondaryContainer = Color(0xFFE4DDD0),
    tertiary = Color(0xFFB1A999),
    background = Color(0xFF14120F),
    onBackground = Color(0xFFECE6DA),
    surface = Color(0xFF14120F),
    onSurface = Color(0xFFECE6DA),
    surfaceVariant = Color(0xFF2A2620),
    onSurfaceVariant = Color(0xFFB1A999),
    surfaceTint = Color(0xFFE7926F),
    outline = Color(0xFF7D7566),
    outlineVariant = Color(0xFF38332C),
    surfaceContainerLowest = Color(0xFF100E0C),
    surfaceContainerLow = Color(0xFF1A1713),
    surfaceContainer = Color(0xFF1F1C17),
    surfaceContainerHigh = Color(0xFF28241F),
    surfaceContainerHighest = Color(0xFF322E27),
    inverseSurface = Color(0xFFECE6DA),
    inverseOnSurface = Color(0xFF2E2A24),
    inversePrimary = Color(0xFFA8452B),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
)

fun parseHexColor(hex: String, fallback: Color = Color(0xFF8C8272)): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)
