package ai.tarang.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Brand.Indigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4E2FF),
    onPrimaryContainer = Color(0xFF1A1567),
    secondary = Brand.Teal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDF3F2),
    onSecondaryContainer = Color(0xFF003736),
    tertiary = Color(0xFFE5640A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE3CF),
    onTertiaryContainer = Color(0xFF4A1C00),
    background = Brand.Mist,
    onBackground = Color(0xFF14162B),
    surface = Brand.Mist,
    onSurface = Color(0xFF14162B),
    surfaceVariant = Color(0xFFE9E9F3),
    onSurfaceVariant = Color(0xFF5A5C73),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF0F0F8),
    surfaceContainerHigh = Color(0xFFEAEAF4),
    surfaceContainerHighest = Color(0xFFE3E3EF),
    outline = Color(0xFFC4C5D6),
    outlineVariant = Color(0xFFE1E1EC),
    error = Color(0xFFD92D20),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA5A1FF),
    onPrimary = Color(0xFF1A1567),
    primaryContainer = Color(0xFF332C9E),
    onPrimaryContainer = Color(0xFFE4E2FF),
    secondary = Color(0xFF5FD8D6),
    onSecondary = Color(0xFF003736),
    secondaryContainer = Color(0xFF0B4F4E),
    onSecondaryContainer = Color(0xFFCDF3F2),
    tertiary = Color(0xFFFFA45C),
    onTertiary = Color(0xFF4A1C00),
    tertiaryContainer = Color(0xFF6B2E00),
    onTertiaryContainer = Color(0xFFFFE3CF),
    background = Brand.Ink,
    onBackground = Color(0xFFE6E6F2),
    surface = Brand.Ink,
    onSurface = Color(0xFFE6E6F2),
    surfaceVariant = Color(0xFF23263D),
    onSurfaceVariant = Color(0xFFA9ABC4),
    surfaceContainerLowest = Color(0xFF080A14),
    surfaceContainerLow = Color(0xFF12152A),
    surfaceContainer = Color(0xFF161A31),
    surfaceContainerHigh = Color(0xFF1D2139),
    surfaceContainerHighest = Color(0xFF252A44),
    outline = Color(0xFF3B3F5C),
    outlineVariant = Color(0xFF2A2E48),
    error = Color(0xFFFF8A80),
)

private val base = Typography()
private val TarangType = Typography(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.copy(lineHeight = 26.sp),
)

private val TarangShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

/** Text style for translated output — larger and airier so Indic scripts breathe. */
val TranslationTextStyle = TextStyle(fontSize = 26.sp, lineHeight = 38.sp, fontWeight = FontWeight.Medium)

@Composable
fun TarangTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = TarangType,
        shapes = TarangShapes,
        content = content,
    )
}
