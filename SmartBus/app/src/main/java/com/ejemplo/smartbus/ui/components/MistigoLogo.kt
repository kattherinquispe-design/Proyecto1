package com.ejemplo.smartbus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ejemplo.smartbus.ui.theme.SmartBusBody
import com.ejemplo.smartbus.ui.theme.SmartBusDisplay
import com.ejemplo.smartbus.ui.theme.SmartBusGradient
import com.ejemplo.smartbus.ui.theme.SmartBusText
import com.ejemplo.smartbus.ui.theme.SmartBusTextSecondary

/**
 * Logo de la app (icono + nombre).
 *
 * Se usa en HomeScreen. El nombre del composable se mantiene como
 * MistigoLogo para no romper las llamadas existentes.
 */
@Composable
fun MistigoLogo(
    modifier: Modifier = Modifier,
    title: String = "MistiGo",
    subtitle: String = "Tu bus, en tiempo real"
) {

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SmartBusGradient),
            contentAlignment = Alignment.Center
        ) {

            Icon(
                imageVector = Icons.Default.DirectionsBus,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(
            modifier = Modifier.width(10.dp)
        )

        Column {

            Text(
                text = title,
                fontFamily = SmartBusDisplay,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = SmartBusText
            )

            Text(
                text = subtitle,
                fontFamily = SmartBusBody,
                fontSize = 12.sp,
                color = SmartBusTextSecondary
            )
        }
    }
}
