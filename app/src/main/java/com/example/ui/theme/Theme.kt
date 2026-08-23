package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = GeometricDarkPrimary,
    onPrimary = GeometricDarkOnPrimary,
    primaryContainer = GeometricDarkPrimaryContainer,
    onPrimaryContainer = GeometricDarkOnPrimaryContainer,
    secondary = GeometricDarkSecondary,
    onSecondary = GeometricDarkOnSecondary,
    secondaryContainer = GeometricDarkSecondaryContainer,
    onSecondaryContainer = GeometricDarkOnSecondaryContainer,
    tertiary = GeometricDarkTertiary,
    onTertiary = GeometricDarkOnTertiary,
    tertiaryContainer = GeometricDarkTertiaryContainer,
    onTertiaryContainer = GeometricDarkOnTertiaryContainer,
    background = GeometricDarkBackground,
    onBackground = GeometricDarkTextPrimary,
    surface = GeometricDarkSurface,
    onSurface = GeometricDarkTextPrimary,
    surfaceVariant = GeometricDarkSurfaceVariant,
    onSurfaceVariant = GeometricDarkTextSecondary,
    outline = GeometricDarkOutline,
    outlineVariant = GeometricDarkOutlineVariant
)

private val LightColorScheme = lightColorScheme(
    primary = GeometricPrimary,
    onPrimary = GeometricOnPrimary,
    primaryContainer = GeometricPrimaryContainer,
    onPrimaryContainer = GeometricOnPrimaryContainer,
    secondary = GeometricSecondary,
    onSecondary = GeometricOnSecondary,
    secondaryContainer = GeometricSecondaryContainer,
    onSecondaryContainer = GeometricOnSecondaryContainer,
    tertiary = GeometricTertiary,
    onTertiary = GeometricOnTertiary,
    tertiaryContainer = GeometricTertiaryContainer,
    onTertiaryContainer = GeometricOnTertiaryContainer,
    background = GeometricBackground,
    onBackground = GeometricTextPrimary,
    surface = GeometricSurface,
    onSurface = GeometricTextPrimary,
    surfaceVariant = GeometricSurfaceVariant,
    onSurfaceVariant = GeometricTextSecondary,
    outline = GeometricOutline,
    outlineVariant = GeometricOutlineVariant
)

@Composable
fun StudyTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our handcrafted rich palette for consistent branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
