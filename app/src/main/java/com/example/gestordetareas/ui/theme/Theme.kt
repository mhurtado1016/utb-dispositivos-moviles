package com.example.gestordetareas.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary            = Teal40,
    onPrimary          = Color.White,
    primaryContainer   = Teal90,
    onPrimaryContainer = Teal10,

    secondary            = Slate40,
    onSecondary          = Color.White,
    secondaryContainer   = Slate90,
    onSecondaryContainer = Teal10,

    tertiary            = Indigo40,
    onTertiary          = Color.White,
    tertiaryContainer   = Indigo90,
    onTertiaryContainer = Teal10,

    error            = Red40,
    onError          = Color.White,

    background = SurfaceLight,
    onBackground = Teal10,
    surface = SurfaceLight,
    onSurface = Teal10,
    surfaceVariant    = Teal90,
    onSurfaceVariant  = Slate40,
)

private val DarkColorScheme = darkColorScheme(
    primary            = Teal80,
    onPrimary          = Teal20,
    primaryContainer   = Teal30,
    onPrimaryContainer = Teal90,

    secondary            = Slate80,
    onSecondary          = Teal20,
    secondaryContainer   = Teal20,
    onSecondaryContainer = Slate90,

    tertiary            = Indigo80,
    onTertiary          = Teal20,
    tertiaryContainer   = Indigo40,
    onTertiaryContainer = Indigo90,

    error            = Red80,
    onError          = Red40,

    background = SurfaceDark,
    onBackground = Teal90,
    surface = SurfaceDark,
    onSurface = Teal90,
    surfaceVariant    = Teal20,
    onSurfaceVariant  = Slate80,
)

@Composable
fun GestorDeTareasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography  = Typography,
        content     = content
    )
}
