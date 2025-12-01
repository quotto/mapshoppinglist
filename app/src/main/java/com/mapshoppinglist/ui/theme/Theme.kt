package com.mapshoppinglist.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = BrandOnPrimary,
    primaryContainer = BrandPrimaryLight,
    onPrimaryContainer = BrandOnSurface,
    secondary = BrandSecondary,
    onSecondary = BrandOnPrimary,
    secondaryContainer = BrandSecondaryContainer,
    onSecondaryContainer = BrandOnSecondaryContainer,
    tertiary = BrandAccent,
    onTertiary = BrandOnPrimary,
    tertiaryContainer = BrandAccentLight,
    onTertiaryContainer = BrandSecondary,
    error = BrandError,
    onError = BrandOnPrimary,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = BrandSurface,
    onBackground = BrandOnSurface,
    surface = BrandSurface,
    onSurface = BrandOnSurface,
    surfaceVariant = BrandSecondaryContainer,
    onSurfaceVariant = BrandOnSurfaceVariant,
    outline = BrandOutline,
    outlineVariant = BrandNeutralBorder,
    scrim = Color(0x66000000),
    inversePrimary = BrandPrimaryLight,
    inverseSurface = BrandSurfaceHigh,
    inverseOnSurface = BrandAccentLight
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimaryLight,
    onPrimary = BrandOnPrimary,
    primaryContainer = BrandPrimaryDark,
    onPrimaryContainer = BrandAccentLight,
    secondary = BrandSecondaryContainer,
    onSecondary = BrandOnSecondaryContainer,
    secondaryContainer = BrandSecondary,
    onSecondaryContainer = BrandOnPrimary,
    tertiary = BrandAccent,
    onTertiary = BrandOnPrimary,
    tertiaryContainer = Color(0xFF7F3127),
    onTertiaryContainer = BrandAccentLight,
    error = BrandError,
    onError = BrandOnPrimary,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = BrandSurfaceHigh,
    onBackground = BrandAccentLight,
    surface = BrandSurfaceHigh,
    onSurface = BrandAccentLight,
    surfaceVariant = Color(0xFF314659),
    onSurfaceVariant = BrandNeutralBorder,
    outline = BrandOutline,
    outlineVariant = Color(0xFF3E4D5A),
    scrim = Color(0x99000000),
    inversePrimary = BrandPrimary,
    inverseSurface = BrandSurface,
    inverseOnSurface = BrandOnSurface
)

@Composable
fun MapShoppingListTheme(useDarkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
