package au.edu.jcu.assessment.utilityapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = RainTeal,
    onPrimary = Color.White,
    primaryContainer = SoftTeal,
    onPrimaryContainer = DeepTeal,
    secondary = Slate,
    onSecondary = Color.White,
    secondaryContainer = PaleMint,
    onSecondaryContainer = Ink,
    tertiary = Color(0xFF825619),
    tertiaryContainer = Color(0xFFFFE3AD),
    onTertiaryContainer = Color(0xFF3A260B),
    background = WarmCream,
    onBackground = Ink,
    surface = WarmCream,
    onSurface = Ink,
    surfaceVariant = PaleMint,
    onSurfaceVariant = Slate,
    outline = Color(0xFF74887F),
    outlineVariant = Color(0xFFCCD8CE),
    errorContainer = Color(0xFFFFDAD3),
    onErrorContainer = Color(0xFF6E1C11)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF87D6BF),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF145447),
    onPrimaryContainer = SoftTeal,
    secondary = Color(0xFFAFCCC0),
    secondaryContainer = NightRaised,
    onSecondaryContainer = NightInk,
    tertiary = Color(0xFFEEC27E),
    tertiaryContainer = Color(0xFF604215),
    onTertiaryContainer = Color(0xFFFFE3AD),
    background = Night,
    onBackground = NightInk,
    surface = NightSurface,
    onSurface = NightInk,
    surfaceVariant = NightRaised,
    onSurfaceVariant = Color(0xFFB3C9BD),
    outline = Color(0xFF8AA296),
    outlineVariant = Color(0xFF3E574A),
    errorContainer = Color(0xFF763126),
    onErrorContainer = Color(0xFFFFDAD3)
)

@Composable
fun MobileTech_UtilityAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
