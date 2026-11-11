package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = Color.Black,
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = EmeraldContainer,
    secondary = RoyalBlue,
    onSecondary = Color.White,
    secondaryContainer = RoyalBlueContainer,
    onSecondaryContainer = Color.White,
    tertiary = AmberGold,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    error = DebitRedDark,
    onError = Color.White,
    errorContainer = DebitRedContainerDark,
    onErrorContainer = DebitRedContainer
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldPrimary,
    secondary = RoyalBlue,
    onSecondary = Color.White,
    secondaryContainer = RoyalBlueContainer,
    onSecondaryContainer = RoyalBlue,
    tertiary = AmberGold,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline,
    error = DebitRed,
    onError = Color.White,
    errorContainer = DebitRedContainer,
    onErrorContainer = DebitRed
)

@Composable
fun HisabProTheme(
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
