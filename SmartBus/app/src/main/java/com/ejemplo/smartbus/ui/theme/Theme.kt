package com.ejemplo.smartbus.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun SmartBusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = lightColorScheme(
        primary = AppPalette.primary,
        onPrimary = Color.White,
        primaryContainer = AppPalette.primarySoft,
        onPrimaryContainer = AppPalette.primaryDark,
        secondary = AppPalette.primaryDark,
        onSecondary = Color.White,
        background = SmartBusBackground,
        onBackground = SmartBusText,
        surface = SmartBusSurface,
        onSurface = SmartBusText,
        surfaceVariant = Color(0xFFF0F0F3),
        onSurfaceVariant = SmartBusTextSecondary,
        error = SmartBusRed,
        onError = Color.White
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SmartBusTypography,
        shapes = SmartBusShapes
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = SmartBusBackground
        ) {
            content()
        }
    }
}
