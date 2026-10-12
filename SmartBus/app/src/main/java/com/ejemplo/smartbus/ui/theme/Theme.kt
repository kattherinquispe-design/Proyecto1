package com.ejemplo.smartbus.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SmartBusLightScheme =
    lightColorScheme(

        primary = SmartBusBlue,
        onPrimary = Color.White,

        primaryContainer = Color(0xFFE5F0FF),
        onPrimaryContainer = SmartBusBlueDark,

        secondary = SmartBusBlueDark,
        onSecondary = Color.White,

        background = SmartBusBackground,
        onBackground = SmartBusText,

        surface = SmartBusSurface,
        onSurface = SmartBusText,

        surfaceVariant = Color(0xFFF0F3F8),
        onSurfaceVariant = SmartBusTextSecondary,

        error = SmartBusRed,
        onError = Color.White
    )

@Composable
fun SmartBusTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {

    MaterialTheme(
        colorScheme = SmartBusLightScheme,
        typography = SmartBusTypography,
        content = content
    )
}