package com.civic.app.ui.theme

import android.content.Context
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Dark mode: white on true black, with an oxblood red. Light mode: black on white, with a signal red.
private val Black = Color(0xFF000000)
private val White = Color(0xFFFFFFFF)
private val Oxblood = Color(0xFFA4161A) // dark-mode accent; white text on it is 7:1
private val OxbloodDeep = Color(0xFF4A0A0C)
private val SignalRed = Color(0xFFE10600) // light-mode accent; 5:1 on white
private val SignalRedWash = Color(0xFFFFE3E1)

private val DarkColors = darkColorScheme(
    primary = Oxblood,
    onPrimary = White,
    primaryContainer = OxbloodDeep,
    onPrimaryContainer = Color(0xFFFFD9D6),
    secondary = Color(0xFFD9D9D9),
    onSecondary = Black,
    secondaryContainer = Color(0xFF262626),
    onSecondaryContainer = White,
    tertiary = Color(0xFFBDBDBD),
    onTertiary = Black,
    tertiaryContainer = Color(0xFF2E2E2E),
    onTertiaryContainer = White,
    background = Black,
    onBackground = White,
    surface = Black,
    onSurface = White,
    surfaceVariant = Color(0xFF1C1C1C),
    onSurfaceVariant = Color(0xFFB3B3B3),
    surfaceContainerLowest = Black,
    surfaceContainerLow = Color(0xFF0F0F0F),
    surfaceContainer = Color(0xFF141414),
    surfaceContainerHigh = Color(0xFF1C1C1C),
    surfaceContainerHighest = Color(0xFF262626),
    inverseSurface = White,
    inverseOnSurface = Black,
    inversePrimary = SignalRed,
    outline = Color(0xFF5C5C5C),
    outlineVariant = Color(0xFF2B2B2B),
    error = Color(0xFFFF6B6B),
    onError = Black,
)

private val LightColors = lightColorScheme(
    primary = SignalRed,
    onPrimary = White,
    primaryContainer = SignalRedWash,
    onPrimaryContainer = Color(0xFF5C0000),
    secondary = Color(0xFF2B2B2B),
    onSecondary = White,
    secondaryContainer = Color(0xFFEDEDED),
    onSecondaryContainer = Black,
    tertiary = Color(0xFF424242),
    onTertiary = White,
    tertiaryContainer = Color(0xFFE6E6E6),
    onTertiaryContainer = Black,
    background = White,
    onBackground = Black,
    surface = White,
    onSurface = Black,
    surfaceVariant = Color(0xFFF2F2F2),
    onSurfaceVariant = Color(0xFF575757),
    surfaceContainerLowest = White,
    surfaceContainerLow = Color(0xFFFAFAFA),
    surfaceContainer = Color(0xFFF5F5F5),
    surfaceContainerHigh = Color(0xFFF0F0F0),
    surfaceContainerHighest = Color(0xFFEAEAEA),
    inverseSurface = Black,
    inverseOnSurface = White,
    inversePrimary = Oxblood,
    outline = Color(0xFF8A8A8A),
    outlineVariant = Color(0xFFDDDDDD),
    error = Color(0xFFB00020),
    onError = White,
)

private val Sans = FontFamily.SansSerif

// Heavy, tightly tracked headings over a calm body; the hierarchy comes from weight, not colour.
private val CivicTypography = Typography(
    displaySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Black, fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-1).sp),
    headlineMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Black, fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.8).sp),
    headlineSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.6).sp),
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp, lineHeight = 26.sp, letterSpacing = (-0.4).sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = (-0.1).sp),
    titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp),
)

// Corners grow with the size of the thing: badges are nearly square, sheets are soft.
private val CivicShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/** Dark is the default look; the header toggle switches to light. */
@Composable
fun CivicTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = CivicTypography,
        shapes = CivicShapes,
        content = content,
    )
}

/** Remembers the header toggle across launches. */
object ThemePreference {
    private const val PREFS = "civic_ui"
    private const val KEY_DARK = "dark_theme"

    fun isDark(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_DARK, true)

    fun setDark(context: Context, dark: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_DARK, dark).apply()
    }
}
