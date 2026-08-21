package com.example.motormates.ui.theme

<<<<<<< HEAD
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = MotorMatesRed,
    secondary = MotorMatesRedLight,
    tertiary = MotorMatesRedLight,
    background = MotorMatesBackground,
    surface = MotorMatesSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
=======
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = md3_dark_primary,
    onPrimary = md3_dark_onPrimary,
    primaryContainer = md3_dark_primaryContainer,
    onPrimaryContainer = md3_dark_onPrimaryContainer,
    secondary = md3_dark_secondary,
    onSecondary = md3_dark_onSecondary,
    secondaryContainer = md3_dark_secondaryContainer,
    onSecondaryContainer = md3_dark_onSecondaryContainer,
    tertiary = md3_dark_tertiary,
    onTertiary = md3_dark_onTertiary,
    tertiaryContainer = md3_dark_tertiaryContainer,
    onTertiaryContainer = md3_dark_onTertiaryContainer,
    error = md3_dark_error,
    onError = md3_dark_onError,
    errorContainer = md3_dark_errorContainer,
    onErrorContainer = md3_dark_onErrorContainer,
    background = md3_dark_background,
    onBackground = md3_dark_onBackground,
    surface = md3_dark_surface,
    onSurface = md3_dark_onSurface,
    surfaceVariant = md3_dark_surfaceVariant,
    onSurfaceVariant = md3_dark_onSurfaceVariant,
    outline = md3_dark_outline,
    outlineVariant = md3_dark_outlineVariant,
    scrim = md3_dark_scrim,
    inverseSurface = md3_dark_inverseSurface,
    inverseOnSurface = md3_dark_inverseOnSurface,
    inversePrimary = md3_dark_inversePrimary
)

private val LightColorScheme = lightColorScheme(
    primary = md3_light_primary,
    onPrimary = md3_light_onPrimary,
    primaryContainer = md3_light_primaryContainer,
    onPrimaryContainer = md3_light_onPrimaryContainer,
    secondary = md3_light_secondary,
    onSecondary = md3_light_onSecondary,
    secondaryContainer = md3_light_secondaryContainer,
    onSecondaryContainer = md3_light_onSecondaryContainer,
    tertiary = md3_light_tertiary,
    onTertiary = md3_light_onTertiary,
    tertiaryContainer = md3_light_tertiaryContainer,
    onTertiaryContainer = md3_light_onTertiaryContainer,
    error = md3_light_error,
    onError = md3_light_onError,
    errorContainer = md3_light_errorContainer,
    onErrorContainer = md3_light_onErrorContainer,
    background = md3_light_background,
    onBackground = md3_light_onBackground,
    surface = md3_light_surface,
    onSurface = md3_light_onSurface,
    surfaceVariant = md3_light_surfaceVariant,
    onSurfaceVariant = md3_light_onSurfaceVariant,
    outline = md3_light_outline,
    outlineVariant = md3_light_outlineVariant,
    scrim = md3_light_scrim,
    inverseSurface = md3_light_inverseSurface,
    inverseOnSurface = md3_light_inverseOnSurface,
    inversePrimary = md3_light_inversePrimary
>>>>>>> origin/master
)

@Composable
fun MotorMatesTheme(
<<<<<<< HEAD
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
=======
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Para esta app, el "modo oscuro" del sistema muestra la paleta clara
    // (md3_light_*), y el "modo claro" del sistema muestra la paleta ya
    // construida en todas las pantallas (md3_dark_*, que usa los colores
    // de marca originales: MotorMatesBackground, MotorMatesRed, etc.).
    val colorScheme = if (darkTheme) LightColorScheme else DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
>>>>>>> origin/master
        typography = Typography,
        content = content
    )
}