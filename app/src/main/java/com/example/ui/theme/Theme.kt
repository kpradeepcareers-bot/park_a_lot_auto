package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ParkBluePrimaryDark,
    onPrimary = ParkBlueOnPrimaryDark,
    primaryContainer = ParkBlueContainerDark,
    onPrimaryContainer = ParkBlueOnContainerDark,
    secondary = ParkTealSecondaryDark,
    onSecondary = ParkTealOnSecondaryDark,
    secondaryContainer = ParkTealContainerDark,
    onSecondaryContainer = ParkTealOnContainerDark,
    tertiary = ParkEvGreenDark,
    tertiaryContainer = ParkEvGreenContainerDark,
    onTertiaryContainer = ParkEvGreenOnContainerDark,
    background = ParkBackgroundDark,
    surface = ParkSurfaceDark,
    surfaceVariant = ParkSurfaceVariantDark,
    outline = ParkOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = ParkBluePrimary,
    onPrimary = ParkBlueOnPrimary,
    primaryContainer = ParkBlueContainer,
    onPrimaryContainer = ParkBlueOnContainer,
    secondary = ParkTealSecondary,
    onSecondary = ParkTealOnSecondary,
    secondaryContainer = ParkTealContainer,
    onSecondaryContainer = ParkTealOnContainer,
    tertiary = ParkEvGreenTertiary,
    tertiaryContainer = ParkEvGreenContainer,
    onTertiaryContainer = ParkEvGreenOnContainer,
    background = ParkBackgroundLight,
    surface = ParkSurfaceLight,
    surfaceVariant = ParkSurfaceVariantLight,
    outline = ParkOutlineLight
)

// Alias for backwards compatibility if needed
val ParkBlueContainerContainer = ParkBlueContainer

@Composable
fun ParkALotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use intentional themed colors by default for consistent brand identity
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

// Keep alias for template compatibility
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = ParkALotTheme(darkTheme, dynamicColor, content)
