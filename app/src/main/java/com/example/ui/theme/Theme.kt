package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PurePrimaryBlue,
    onPrimary = PureTextPrimary,
    primaryContainer = PureDarkElevated,
    onPrimaryContainer = PureSecondaryBlue,
    secondary = PureSecondaryBlue,
    onSecondary = PureDarkBackground,
    secondaryContainer = PureDarkElevated,
    onSecondaryContainer = PureTextPrimary,
    tertiary = PureAccentCyan,
    onTertiary = PureDarkBackground,
    background = PureDarkBackground,
    onBackground = PureTextPrimary,
    surface = PureDarkSurface,
    onSurface = PureTextPrimary,
    surfaceVariant = PureDarkElevated,
    onSurfaceVariant = PureTextSecondary,
    outline = PureBorder,
    outlineVariant = PureBorder,
    error = PureErrorRed,
    onError = PureTextPrimary
)

@Composable
fun PureLockTheme(
    content: @Composable () -> Unit
) {
    // PureLock is explicitly designed with a unified, premium cybersecurity dark identity
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
