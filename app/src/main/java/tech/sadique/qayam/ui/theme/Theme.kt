package tech.sadique.qayam.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import tech.sadique.qayam.data.model.AppThemeMode

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = GoldAccent,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFFFF0C2),
    onSecondaryContainer = Color(0xFF4C3800),
    tertiary = Color(0xFF1B6A88),
    onTertiary = Color.White,
    background = SandBackgroundLight,
    onBackground = Color(0xFF191C1B),
    surface = SandSurfaceLight,
    onSurface = Color(0xFF191C1B),
    surfaceVariant = SandSurfaceVariant,
    onSurfaceVariant = Color(0xFF404944),
    outline = Color(0xFF707973),
    outlineVariant = Color(0xFFC0C9C2)
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = Color(0xFF70F6CB),
    secondary = DarkSecondary,
    onSecondary = Color(0xFF422E00),
    secondaryContainer = Color(0xFF5E4300),
    onSecondaryContainer = Color(0xFFFFE088),
    tertiary = Color(0xFF88D2F5),
    onTertiary = Color(0xFF003548),
    background = DarkBackground,
    onBackground = Color(0xFFE1E3DF),
    surface = DarkSurface,
    onSurface = Color(0xFFE1E3DF),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFBFC9C2),
    outline = Color(0xFF89938D),
    outlineVariant = Color(0xFF404944)
)

private val NightMosqueColorScheme = darkColorScheme(
    primary = MosquePrimary,
    onPrimary = Color(0xFF00382B),
    primaryContainer = MosqueSurfaceVariant,
    onPrimaryContainer = Color(0xFF80FCD2),
    secondary = MosqueGold,
    onSecondary = Color(0xFF473600),
    secondaryContainer = Color(0xFF654D00),
    onSecondaryContainer = Color(0xFFFFEEB2),
    tertiary = Color(0xFF90DCFF),
    onTertiary = Color(0xFF003549),
    background = MosqueBackground,
    onBackground = Color(0xFFE0E5E2),
    surface = MosqueSurface,
    onSurface = Color(0xFFE0E5E2),
    surfaceVariant = MosqueSurfaceVariant,
    onSurfaceVariant = Color(0xFFBCC7C1),
    outline = Color(0xFF84938B),
    outlineVariant = Color(0xFF284841)
)

@Composable
fun SalahTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    // Brand-only schemes (no dynamic color): the emerald/gold identity is
    // intentional across all modes, including Android 12+.
    val systemDark = isSystemInDarkTheme()

    val colorScheme = when (themeMode) {
        AppThemeMode.NIGHT_MOSQUE -> NightMosqueColorScheme
        AppThemeMode.DARK -> DarkColorScheme
        AppThemeMode.LIGHT -> LightColorScheme
        AppThemeMode.SYSTEM -> if (systemDark) DarkColorScheme else LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
