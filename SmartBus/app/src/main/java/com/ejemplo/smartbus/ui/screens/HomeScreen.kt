package com.ejemplo.smartbus.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ejemplo.smartbus.ui.components.SmartBusBottomBar
import com.ejemplo.smartbus.ui.components.SmartBusBlueButton
import com.ejemplo.smartbus.ui.components.SmartBusBusCard
import com.ejemplo.smartbus.ui.components.SmartBusPointsCard
import com.ejemplo.smartbus.ui.components.SmartBusSectionTitle
import com.ejemplo.smartbus.ui.components.SmartBusStopCard
import com.ejemplo.smartbus.ui.components.SmartBusTab
import com.ejemplo.smartbus.ui.theme.SmartBusBackground
import com.ejemplo.smartbus.ui.theme.SmartBusBlue
import com.ejemplo.smartbus.ui.theme.SmartBusText
import com.ejemplo.smartbus.ui.theme.SmartBusTextSecondary

@Composable
fun HomeScreen(
    points: Int,
    trackingActive: Boolean,
    onTrackingClick: () -> Unit,
    onMapClick: () -> Unit,
    onRewardsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSearchClick: () -> Unit,
    onReportsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onSettingsClick: () -> Unit,

    // Se mantiene para que tu SmartBusNavigation actual
    // no dé error si todavía lo está enviando.
    trackingState: String = ""
) {

    Scaffold(
        containerColor = SmartBusBackground,

        bottomBar = {

            SmartBusBottomBar(
                selectedTab = SmartBusTab.HOME,

                onTabSelected = { tab ->

                    when (tab) {

                        SmartBusTab.HOME -> {
                            // Ya estamos en Inicio
                        }

                        SmartBusTab.MAP -> {
                            onMapClick()
                        }

                        SmartBusTab.REWARDS -> {
                            onRewardsClick()
                        }

                        SmartBusTab.RANKING -> {
                            // El ranking lo conectaremos
                            // con su pantalla correspondiente.
                        }

                        SmartBusTab.PROFILE -> {
                            onProfileClick()
                        }
                    }
                }
            )
        }

    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 18.dp,
                    vertical = 16.dp
                )
        ) {

            // ==================================================
            // ENCABEZADO
            // ==================================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Hola, usuario",
                        style = MaterialTheme.typography.headlineMedium,
                        color = SmartBusText
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = "¡Sigue viajando y gana puntos!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SmartBusTextSecondary
                    )
                }

                IconButton(
                    onClick = onNotificationsClick
                ) {

                    Icon(
                        imageVector = Icons.Default.NotificationsNone,
                        contentDescription = "Notificaciones",
                        tint = SmartBusText
                    )
                }

                IconButton(
                    onClick = onSettingsClick
                ) {

                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configuración",
                        tint = SmartBusText
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            // ==================================================
            // TARJETA DE PUNTOS
            // ==================================================

            SmartBusPointsCard(
                points = points,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(
                modifier = Modifier.height(22.dp)
            )

            // ==================================================
            // PRÓXIMO BUS
            // ==================================================

            SmartBusSectionTitle(
                title = "Próximo bus"
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            SmartBusBusCard(
                routeName = "Ruta 01",
                destination = "Ciudad Blanca → Centro",
                eta = "5",
                status = if (trackingActive) {
                    "Llegando"
                } else {
                    "Disponible"
                },
                onClick = onMapClick
            )

            Spacer(
                modifier = Modifier.height(22.dp)
            )

            // ==================================================
            // PARADEROS CERCANOS
            // ==================================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                SmartBusSectionTitle(
                    title = "Paraderos cercanos",
                    modifier = Modifier.weight(1f)
                )

                TextButton(
                    onClick = onMapClick
                ) {

                    Text(
                        text = "Ver todos",
                        color = SmartBusBlue
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            SmartBusStopCard(
                name = "Paradero Ciudad Blanca",
                distance = "350 m",
                onClick = onMapClick
            )

            Spacer(
                modifier = Modifier.height(9.dp)
            )

            SmartBusStopCard(
                name = "Paradero Paucarpata",
                distance = "720 m",
                onClick = onMapClick
            )

            Spacer(
                modifier = Modifier.height(9.dp)
            )

            SmartBusStopCard(
                name = "Paradero Centro",
                distance = "1.1 km",
                onClick = onMapClick
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            // ==================================================
            // ESTADO DEL GPS
            // ==================================================

            Text(
                text = if (trackingActive) {
                    "[ACTIVO] SmartBus está detectando tu recorrido"
                } else {
                    "[INACTIVO] Seguimiento GPS detenido"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = SmartBusTextSecondary
            )

            if (trackingState.isNotBlank()) {

                Spacer(
                    modifier = Modifier.height(5.dp)
                )

                Text(
                    text = trackingState,
                    style = MaterialTheme.typography.bodySmall,
                    color = SmartBusBlue
                )
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            // ==================================================
            // BOTÓN GPS
            // ==================================================

            SmartBusBlueButton(
                text = if (trackingActive) {
                    "Detener seguimiento"
                } else {
                    "Activar seguimiento GPS"
                },

                onClick = onTrackingClick,

                modifier = Modifier.fillMaxWidth()
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            // ==================================================
            // BUSCAR DESTINO
            // ==================================================

            OutlinedButton(
                onClick = onSearchClick,

                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),

                shape = RoundedCornerShape(14.dp)
            ) {

                Text(
                    text = "Buscar destino"
                )
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            // ==================================================
            // REPORTES
            // ==================================================

            OutlinedButton(
                onClick = onReportsClick,

                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),

                shape = RoundedCornerShape(14.dp)
            ) {

                Text(
                    text = "Reportar problema Reportar problema"
                )
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )
        }
    }
}