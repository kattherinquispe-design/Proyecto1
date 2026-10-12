package com.ejemplo.smartbus.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ejemplo.smartbus.ui.theme.SmartBusBlue
import com.ejemplo.smartbus.ui.theme.SmartBusGreenLight
import com.ejemplo.smartbus.ui.theme.SmartBusText
import com.ejemplo.smartbus.ui.theme.SmartBusTextSecondary

@Composable
fun HelpScreen(
    onBack: () -> Unit,
    onSettingsClick: () -> Unit
) {

    var openQuestion by remember { mutableStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "Ayuda",
                style = MaterialTheme.typography.headlineMedium
            )

            IconButton(onClick = onBack) {

                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SmartBusGreenLight
                )
            ) {

                Column(modifier = Modifier.padding(16.dp)) {

                    Text(
                        text = "Como funcionan los puntos",
                        style = MaterialTheme.typography.titleMedium,
                        color = SmartBusText
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Ganas 50 puntos cada vez que subes a un bus " +
                            "en un paradero registrado. El sistema valida " +
                            "que un bus real este cerca y que te hayas " +
                            "movido, por lo que no se pueden sumar puntos " +
                            "sin viajar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SmartBusTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "PREGUNTAS FRECUENTES",
                style = MaterialTheme.typography.labelMedium,
                color = SmartBusTextSecondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            HelpQuestion(
                icon = Icons.Default.GpsFixed,
                question = "Por que no me suma puntos?",
                answer = "Verifica que el seguimiento este activo, que " +
                    "estes dentro del radio del paradero y que un bus " +
                    "este detenido cerca. Tambien debe existir un cambio " +
                    "de ubicacion, es decir, que te hayas movido.",
                expanded = openQuestion == 0,
                onToggle = {
                    openQuestion = if (openQuestion == 0) -1 else 0
                }
            )

            HelpQuestion(
                icon = Icons.Default.Map,
                question = "Por que el mapa no carga?",
                answer = "Comprueba tu conexion a internet y que la " +
                    "clave de Google Maps este configurada en el " +
                    "proyecto. Sin esa clave el mapa aparece en blanco.",
                expanded = openQuestion == 1,
                onToggle = {
                    openQuestion = if (openQuestion == 1) -1 else 1
                }
            )

            HelpQuestion(
                icon = Icons.Default.Bolt,
                question = "Cuantos puntos cuesta cada recompensa?",
                answer = "Cada recompensa indica su costo en puntos. " +
                    "Una recompensa solo se puede canjear una vez y " +
                    "recibiras un codigo para presentarlo.",
                expanded = openQuestion == 2,
                onToggle = {
                    openQuestion = if (openQuestion == 2) -1 else 2
                }
            )

            HelpQuestion(
                icon = Icons.Default.EmojiEvents,
                question = "Puedo canjear dos veces la misma recompensa?",
                answer = "No. Cada recompensa se marca como canjeada " +
                    "en tu dispositivo y queda bloqueada de forma " +
                    "permanente.",
                expanded = openQuestion == 3,
                onToggle = {
                    openQuestion = if (openQuestion == 3) -1 else 3
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "OTRAS OPCIONES",
                style = MaterialTheme.typography.labelMedium,
                color = SmartBusTextSecondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            HelpLink(
                icon = Icons.Default.BugReport,
                title = "Ajustes de seguimiento y avisos",
                onClick = onSettingsClick
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "SmartBus · version 1.0",
                style = MaterialTheme.typography.bodySmall,
                color = SmartBusTextSecondary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HelpQuestion(
    icon: ImageVector,
    question: String,
    answer: String,
    expanded: Boolean,
    onToggle: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {

        Column(modifier = Modifier.padding(15.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SmartBusBlue,
                    modifier = Modifier.size(22.dp)
                )

                Spacer(modifier = Modifier.size(14.dp))

                Text(
                    text = question,
                    modifier = Modifier.weight(1f),
                    color = SmartBusText
                )

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = SmartBusTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (expanded) {

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SmartBusTextSecondary
                )
            }
        }
    }
}

@Composable
private fun HelpLink(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SmartBusBlue,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.size(14.dp))

            Text(
                text = title,
                modifier = Modifier.weight(1f),
                color = SmartBusText
            )

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = SmartBusTextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}