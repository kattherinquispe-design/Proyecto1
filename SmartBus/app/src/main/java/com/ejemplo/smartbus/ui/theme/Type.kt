package com.ejemplo.smartbus.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ejemplo.smartbus.R

/**
 * Tipografia mixta:
 * - Sora (fuente variable, moderna y con mucho estilo) para titulares.
 * - Poppins (limpia y legible) para textos de cuerpo.
 * Da un look unico, mas estetico que la tipografia por defecto.
 */
val SmartBusDisplay = FontFamily(
    Font(R.font.sora_var, FontWeight.Normal),
    Font(R.font.sora_var, FontWeight.Medium),
    Font(R.font.sora_var, FontWeight.SemiBold),
    Font(R.font.sora_var, FontWeight.Bold)
)

val SmartBusBody = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold)
)

val SmartBusTypography = Typography(

    displayLarge = TextStyle(
        fontFamily = SmartBusDisplay,
        fontSize = 36.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-1.0).sp
    ),

    displaySmall = TextStyle(
        fontFamily = SmartBusDisplay,
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.6).sp
    ),

    headlineLarge = TextStyle(
        fontFamily = SmartBusDisplay,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
    ),

    headlineMedium = TextStyle(
        fontFamily = SmartBusDisplay,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.3).sp
    ),

    headlineSmall = TextStyle(
        fontFamily = SmartBusDisplay,
        fontSize = 21.sp,
        fontWeight = FontWeight.Bold
    ),

    titleLarge = TextStyle(
        fontFamily = SmartBusDisplay,
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold
    ),

    titleMedium = TextStyle(
        fontFamily = SmartBusBody,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold
    ),

    titleSmall = TextStyle(
        fontFamily = SmartBusBody,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold
    ),

    bodyLarge = TextStyle(
        fontFamily = SmartBusBody,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal
    ),

    bodyMedium = TextStyle(
        fontFamily = SmartBusBody,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal
    ),

    bodySmall = TextStyle(
        fontFamily = SmartBusBody,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal
    ),

    labelLarge = TextStyle(
        fontFamily = SmartBusBody,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.2.sp
    ),

    labelMedium = TextStyle(
        fontFamily = SmartBusBody,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp
    ),

    labelSmall = TextStyle(
        fontFamily = SmartBusBody,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.4.sp
    )
)
