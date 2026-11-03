package com.ejemplo.smartbus.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ======================================================
// SMARTBUS - PALETA PRINCIPAL
// Fondo gris muy claro, texto azul marino oscuro y un
// acento vivo (fucsia/coral por defecto) elegible en
// Configuracion.
// ======================================================

val SmartBusBackground = Color(0xFFF4F4F6)
val SmartBusSurface = Color(0xFFFFFFFF)

val SmartBusText = Color(0xFF1B1C2E)
val SmartBusTextSecondary = Color(0xFF707584)
val SmartBusTextLight = Color(0xFF9AA0AE)

val SmartBusGreen = Color(0xFF22B573)
val SmartBusGreenLight = Color(0xFFE5F8F0)

val SmartBusRed = Color(0xFFE94B5F)
val SmartBusRedLight = Color(0xFFFFE7EA)

val SmartBusDivider = Color(0xFFECECF1)

// Barra inferior oscura estilo moderno (foto de referencia)
val SmartBusNavyBar = Color(0xFF232631)

val Transparent = Color.Transparent

// ======================================================
// COLORES DINAMICOS
// Su valor viene del acento elegido en Configuracion
// (AppPalette), asi toda la app cambia al instante.
// ======================================================

val SmartBusBlue: Color get() = AppPalette.primary
val SmartBusBlueDark: Color get() = AppPalette.primaryDark
val SmartBusBlueLight: Color get() = AppPalette.primarySoft

val SmartBusYellow: Color get() = AppPalette.primaryDark
val SmartBusYellowLight: Color get() = AppPalette.primarySoft

val SmartBusPink: Color get() = AppPalette.primary
val SmartBusPinkLight: Color get() = AppPalette.primarySoft

val SmartBusPurple: Color get() = AppPalette.primaryDark
val SmartBusPurpleLight: Color get() = AppPalette.primarySoft

// Degradado horizontal del acento: de intenso a coral suave.
val SmartBusGradient: Brush
    get() = Brush.horizontalGradient(
        colors = listOf(AppPalette.primary, AppPalette.primaryDark)
    )
