package com.ejemplo.smartbus.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val SmartBusTypography = Typography(

    displayLarge = androidx.compose.ui.text.TextStyle(
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.8).sp
    ),

    headlineLarge = androidx.compose.ui.text.TextStyle(
        fontSize = 27.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.4).sp
    ),

    headlineMedium = androidx.compose.ui.text.TextStyle(
        fontSize = 23.sp,
        fontWeight = FontWeight.Bold
    ),

    titleLarge = androidx.compose.ui.text.TextStyle(
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold
    ),

    titleMedium = androidx.compose.ui.text.TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold
    ),

    bodyLarge = androidx.compose.ui.text.TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal
    ),

    bodyMedium = androidx.compose.ui.text.TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal
    ),

    bodySmall = androidx.compose.ui.text.TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal
    ),

    labelLarge = androidx.compose.ui.text.TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold
    )
)