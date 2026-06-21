package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BentoColorScheme = darkColorScheme(
    primary = BentoPrimary,
    onPrimary = BentoOnPrimary,
    secondary = BentoSecondary,
    onSecondary = BentoOnSecondary,
    tertiary = BentoTertiary,
    onTertiary = BentoOnTertiary,
    background = BentoBg,
    onBackground = BentoOnSurface,
    surface = BentoSurface,
    onSurface = BentoOnSurface,
    surfaceVariant = BentoSurfaceVariant,
    onSurfaceVariant = BentoOnSurfaceVariant
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, 
    dynamicColor: Boolean = false, 
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BentoColorScheme,
        typography = Typography,
        content = content
    )
}
