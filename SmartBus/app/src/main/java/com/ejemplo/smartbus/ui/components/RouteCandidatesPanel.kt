package com.ejemplo.smartbus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus as BusIcon
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ejemplo.smartbus.data.model.Bus
import com.ejemplo.smartbus.data.model.RouteCandidate
import com.ejemplo.smartbus.ui.theme.SmartBusBlue
import com.ejemplo.smartbus.ui.theme.SmartBusGreen
import com.ejemplo.smartbus.ui.theme.SmartBusText
import com.ejemplo.smartbus.ui.theme.SmartBusTextSecondary

/**
 * Panel con las rutas que sirven a un destino.
 *
 * Al elegir un destino, se busca la parada mas cercana y se listan
 * todas las rutas que pasan por ahi con los buses que hay ahora mismo
 * y su tiempo estimado de llegada. Al pulsar una ruta, el mapa dibuja
 * su recorrido y se enfoca en ella.
 */
@Composable
fun RouteCandidatesPanel(
    candidates: List<RouteCandidate>,
    selectedRouteId: Int?,
    destinationName: String?,
    onSelectRoute: (RouteCandidate) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(modifier = Modifier.weight(1f)) {

                    Text(
                        text = "Buses hacia tu destino",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SmartBusText
                    )

                    if (!destinationName.isNullOrBlank()) {

                        Text(
                            text = destinationName,
                            style = MaterialTheme.typography.bodySmall,
                            color = SmartBusTextSecondary
                        )
                    }
                }

                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cerrar"
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (candidates.isEmpty()) {

                Text(
                    text = "No hay ninguna ruta registrada cerca de ese destino. Cuando conectes la base de datos con las rutas reales, apareceran aqui.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SmartBusTextSecondary
                )

            } else {

                LazyColumn(
                    modifier = Modifier.heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    items(candidates, key = { it.route.id }) { candidate ->

                        RouteCandidateRow(
                            candidate = candidate,
                            isSelected =
                                candidate.route.id == selectedRouteId,
                            onClick = {
                                onSelectRoute(candidate)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteCandidateRow(
    candidate: RouteCandidate,
    isSelected: Boolean,
    onClick: () -> Unit
) {

    val eta = candidate.etaMinutes

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) {
                    SmartBusBlue.copy(alpha = 0.10f)
                } else {
                    Color(0xFFF5F6F8)
                }
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box2()

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {

            Text(
                text = "Ruta ${candidate.route.code}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = SmartBusText
            )

            Text(
                text = "${candidate.route.origin} → ${candidate.route.destination}",
                style = MaterialTheme.typography.bodySmall,
                color = SmartBusTextSecondary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                TinyLabel(
                    icon = {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = SmartBusTextSecondary
                        )
                    },
                    text =
                        "${candidate.boardingStop.name} · ${formatMeters(candidate.distanceToStopMeters)}"
                )

                TinyLabel(
                    icon = {
                        Icon(
                            Icons.Default.BusIcon,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = SmartBusTextSecondary
                        )
                    },
                    text =
                        "${candidate.buses.size} bus(es)"
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(horizontalAlignment = Alignment.End) {

            if (eta != null) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {

                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = SmartBusGreen
                    )

                    Text(
                        text = if (eta == 0) "Llegando" else "$eta min",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SmartBusGreen
                    )
                }

            } else {

                Text(
                    text = "Sin bus",
                    style = MaterialTheme.typography.bodySmall,
                    color = SmartBusTextSecondary
                )
            }

            candidate.closestBus?.let { bus ->

                Text(
                    text = bus.code,
                    style = MaterialTheme.typography.bodySmall,
                    color = SmartBusTextSecondary
                )
            }
        }
    }
}

@Composable
private fun Box2() {

    Column(
        modifier = Modifier.size(38.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Icon(
            Icons.Default.BusIcon,
            contentDescription = null,
            tint = SmartBusBlue,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun TinyLabel(
    icon: @Composable () -> Unit,
    text: String
) {

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {

        icon()

        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = SmartBusTextSecondary
        )
    }
}

private fun formatMeters(meters: Float): String =
    if (meters >= 1000f) {
        String.format("%.1f km", meters / 1000f)
    } else {
        "${meters.toInt()} m"
    }