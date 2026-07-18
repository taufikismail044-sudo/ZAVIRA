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
    primary = BaznasGreenDarkTheme,
    onPrimary = Color(0xFF003914),
    primaryContainer = BaznasGreen,
    onPrimaryContainer = Color(0xFFD3EAD8),
    secondary = BaznasGoldDarkTheme,
    onSecondary = Color(0xFF422F00),
    secondaryContainer = Color(0xFF5E4500),
    tertiary = BaznasGreenLight,
    background = DarkBackground,
    onBackground = TextWhite,
    surface = DarkSurface,
    onSurface = TextWhite,
    surfaceVariant = Color(0xFF2C3530),
    onSurfaceVariant = TextMuted
)

private val LightColorScheme = lightColorScheme(
    primary = BaznasGreen,
    onPrimary = Color.White,
    primaryContainer = SoftGreenBg,
    onPrimaryContainer = BaznasGreen,
    secondary = BaznasGold,
    onSecondary = Color.White,
    secondaryContainer = SoftGreenBg,
    tertiary = BaznasGreenLight,
    background = SoftGreenBg,
    onBackground = TextDark,
    surface = SoftGreenSurface,
    onSurface = TextDark,
    surfaceVariant = Color(0xFFE2E9E3),
    onSurfaceVariant = TextLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    // Set to false by default to preserve BAZNAS branding identity rather than system-dynamic colors
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
