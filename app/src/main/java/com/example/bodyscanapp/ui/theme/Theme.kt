package com.example.bodyscanapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = BodyScanPrimary,
    secondary = BodyScanSecondary,
    tertiary = Pink40,
    background = BodyScanBackground,
    surface = BodyScanBackground,
    onBackground = BodyScanPrimary,
    onSurface = BodyScanPrimary
)

@Composable
fun BodyScanAppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}