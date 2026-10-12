package com.ejemplo.smartbus.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val SmartBusShapes = Shapes(
    small = RoundedCornerShape(8.dp),      // Chips, Badges, Filtros ("Todas", "Comida")
    medium = RoundedCornerShape(16.dp),    // Tarjetas principales, list items, campos de texto
    large = RoundedCornerShape(24.dp),     // Banners (Banner de puntos), Modales flotantes
    extraLarge = RoundedCornerShape(32.dp) // Modales full redondeados
)