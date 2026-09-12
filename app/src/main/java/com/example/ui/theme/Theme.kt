package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// High-Contrast Dark Scheme (Black canvas with White typography & crisp vector borders)
private val DarkColorScheme = darkColorScheme(
    primary = VTWhite,
    onPrimary = VTBlack,
    primaryContainer = VTDarkCard,
    onPrimaryContainer = VTWhite,
    secondary = VTWhite,
    onSecondary = VTBlack,
    secondaryContainer = VTDarkBorder,
    onSecondaryContainer = VTWhite,
    tertiary = VTWhite,
    onTertiary = VTBlack,
    background = VTBlack,
    onBackground = VTWhite,
    surface = VTDarkSurface,
    onSurface = VTWhite,
    surfaceVariant = VTDarkCard,
    onSurfaceVariant = VTWhite,
    outline = VTDarkBorder,
    outlineVariant = Color(0xFF262626),
    error = VTAccentRed,
    onError = VTWhite
)

// High-Contrast Light Scheme (White canvas with Black typography & crisp vector borders)
private val LightColorScheme = lightColorScheme(
    primary = VTBlack,
    onPrimary = VTWhite,
    primaryContainer = VTLightCard,
    onPrimaryContainer = VTBlack,
    secondary = VTBlack,
    onSecondary = VTWhite,
    secondaryContainer = VTLightBorder,
    onSecondaryContainer = VTBlack,
    tertiary = VTBlack,
    onTertiary = VTWhite,
    background = VTWhite,
    onBackground = VTBlack,
    surface = VTLightSurface,
    onSurface = VTBlack,
    surfaceVariant = VTLightCard,
    onSurfaceVariant = VTBlack,
    outline = VTLightBorder,
    outlineVariant = Color(0xFFE0E0E0),
    error = VTAccentRed,
    onError = VTWhite
)

@Composable
fun VTPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
