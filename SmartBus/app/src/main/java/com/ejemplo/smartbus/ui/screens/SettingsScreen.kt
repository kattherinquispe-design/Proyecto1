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
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.ejemplo.smartbus.data.SettingsRepository
import com.ejemplo.smartbus.ui.theme.SmartBusBlue
import com.ejemplo.smartbus.ui.theme.SmartBusDivider
import com.ejemplo.smartbus.ui.theme.SmartBusGreen
import com.ejemplo.smartbus.ui.theme.SmartBusRed
import com.ejemplo.smartbus.ui.theme.SmartBusText
import com.ejemplo.smartbus.ui.theme.SmartBusTextSecondary

@Composable
fun SettingsScreen(
    settings: SettingsRepository,
    onBack: () -> Unit,
    onHelpClick: () -> Unit,
    onClearPoints: () -> Unit
) {

    var showClearDialog by remember { mutableStateOf(false) }

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
                text = "Configuración",
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

            SettingsSectionTitle("Seguimiento")

            SettingsCard {

                SettingsToggleRow(
                    icon = Icons.Default.PlayCircle,
                    title = "Seguimiento automatico",
                    subtitle = "Inicia el GPS al abrir la aplicacion",
                    checked = settings.autoTracking,
                    onCheckedChange = { settings.setAutoTracking(it) }
                )

                SettingsDivider()

                SettingsToggleRow(
                    icon = Icons.Default.DirectionsBus,
                    title = "Inicio al abordar",
                    subtitle = "Registrar el viaje automaticamente",
                    checked = settings.autoStartOnBoard,
                    onCheckedChange = { settings.setAutoStartOnBoard(it) }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            SettingsSectionTitle("Ubicación")

            SettingsCard {

                SettingsToggleRow(
                    icon = Icons.Default.GpsFixed,
                    title = "Ubicacion precisa",
                    subtitle = "Mejor precision, mayor consumo de bateria",
                    checked = settings.highAccuracyLocation,
                    onCheckedChange = { settings.setHighAccuracyLocation(it) }
                )

                SettingsDivider()

                Column(modifier = Modifier.padding(15.dp)) {

                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Icon(
                            imageVector = Icons.Default.Straighten,
                            contentDescription = null,
                            tint = SmartBusBlue,
                            modifier = Modifier.size(22.dp)
                        )

                        Spacer(modifier = Modifier.size(14.dp))

                        Text(
                            text = "Unidades de distancia",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                        FilterChip(
                            selected =
                                settings.distanceUnits ==
                                    SettingsRepository.UNITS_METRIC,
                            onClick = {
                                settings.setDistanceUnits(
                                    SettingsRepository.UNITS_METRIC
                                )
                            },
                            label = { Text("Kilometros / m") }
                        )

                        FilterChip(
                            selected =
                                settings.distanceUnits ==
                                    SettingsRepository.UNITS_IMPERIAL,
                            onClick = {
                                settings.setDistanceUnits(
                                    SettingsRepository.UNITS_IMPERIAL
                                )
                            },
                            label = { Text("Millas / pies") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            SettingsSectionTitle("Avisos")

            SettingsCard {

                SettingsToggleRow(
                    icon = Icons.Default.Notifications,
                    title = "Notificaciones",
                    subtitle = "Recibir avisos de la aplicacion",
                    checked = settings.notificationsEnabled,
                    onCheckedChange = { settings.setNotificationsEnabled(it) }
                )

                SettingsDivider()

                SettingsToggleRow(
                    icon = Icons.Default.BluetoothSearching,
                    title = "Avisos de abordaje",
                    subtitle = "Notificar cuando se valide un abordaje",
                    checked = settings.boardingAlerts,
                    onCheckedChange = { settings.setBoardingAlerts(it) }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            SettingsSectionTitle("Datos")

            SettingsCard {

                SettingsActionRow(
                    icon = Icons.Default.Refresh,
                    title = "Restablecer configuracion",
                    subtitle = "Volver a los valores originales",
                    onClick = { settings.resetToDefaults() }
                )

                SettingsDivider()

                SettingsActionRow(
                    icon = Icons.Default.Delete,
                    title = "Borrar puntos e historial",
                    subtitle = "Esta accion no se puede deshacer",
                    titleColor = SmartBusRed,
                    onClick = { showClearDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            SettingsSectionTitle("Ayuda")

            SettingsCard {

                SettingsActionRow(
                    icon = Icons.Default.Info,
                    title = "Centro de ayuda",
                    subtitle = "Preguntas frecuentes y contacto",
                    onClick = onHelpClick
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "SmartBus · version 1.0",
                style = MaterialTheme.typography.bodySmall,
                color = SmartBusTextSecondary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showClearDialog) {

        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Borrar puntos e historial") },
            text = {
                Text(
                    "Se eliminaran todos tus puntos, tu historial de " +
                        "abordajes y las recompensas ya canjeadas. " +
                        "Esta accion no se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onClearPoints()
                    showClearDialog = false
                }) {
                    Text("Borrar todo", color = SmartBusRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {

    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = SmartBusTextSecondary,
        modifier = Modifier.padding(
            start = 4.dp,
            bottom = 8.dp
        )
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {

        Column(content = { content() })
    }
}

@Composable
private fun SettingsDivider() {

    HorizontalDivider(
        color = SmartBusDivider,
        modifier = Modifier.padding(start = 51.dp)
    )
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
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

        Column(modifier = Modifier.weight(1f)) {

            Text(
                text = title,
                color = SmartBusText
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = SmartBusTextSecondary
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SmartBusGreen,
                uncheckedTrackColor = SmartBusDivider
            )
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    titleColor: Color = SmartBusText
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (titleColor == SmartBusRed) SmartBusRed else SmartBusBlue,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.size(14.dp))

        Column(modifier = Modifier.weight(1f)) {

            Text(
                text = title,
                color = titleColor
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = SmartBusTextSecondary
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = SmartBusTextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}