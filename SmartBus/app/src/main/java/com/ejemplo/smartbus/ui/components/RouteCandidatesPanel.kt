package com.ejemplo.smartbus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus as BusIcon
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ejemplo.smartbus.data.model.RouteCandidate
import com.ejemplo.smartbus.ui.theme.AppPalette
import com.ejemplo.smartbus.ui.theme.SmartBusBody
import com.ejemplo.smartbus.ui.theme.SmartBusDisplay
import com.ejemplo.smartbus.ui.theme.SmartBusGreen
import com.ejemplo.smartbus.ui.theme.SmartBusText
import com.ejemplo.smartbus.ui.theme.SmartBusTextSecondary

/**
 * Panel compacto y estetico con las rutas directas que llegan al destino buscado.
 * Ocupa un espacio prudente en pantalla, se cierra inmediatamente al seleccionar
 * una ruta para no tapar el mapa, y permite reabrirse facilmente.
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Indicador de arrastre sutil
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCBD5E1))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Cabecera compacta
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Rutas al destino (radio 400 m)",
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = SmartBusDisplay,
                        fontWeight = FontWeight.Bold,
                        color = SmartBusText
                    )
                    if (!destinationName.isNullOrBlank()) {
                        Text(
                            text = destinationName,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = SmartBusBody,
                            color = SmartBusTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = SmartBusTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (candidates.isEmpty()) {
                Text(
                    text = "No hay rutas registradas cerca de este punto. Prueba buscando otra zona.",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = SmartBusBody,
                    color = SmartBusTextSecondary,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 210.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(candidates, key = { it.route.id }) { candidate ->
                        RouteCandidateRow(
                            candidate = candidate,
                            isSelected = candidate.route.id == selectedRouteId,
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

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) AppPalette.primarySoft else Color(0xFFF8FAFC),
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono de bus con color de paleta
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppPalette.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BusIcon,
                    contentDescription = null,
                    tint = AppPalette.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Detalle de la ruta
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ruta " + candidate.route.code,
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = SmartBusDisplay,
                        fontWeight = FontWeight.Bold,
                        color = SmartBusText
                    )

                    // Badge de ETA
                    if (eta != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFDCFCE7)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(11.dp),
                                    tint = SmartBusGreen
                                )
                                Text(
                                    text = if (eta == 0) "Llegando" else "" + eta + " min",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SmartBusBody,
                                    color = SmartBusGreen
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = "Sin buses en vivo",
                                fontSize = 10.sp,
                                fontFamily = SmartBusBody,
                                color = SmartBusTextSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = candidate.route.origin + " → " + candidate.route.destination,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = SmartBusBody,
                    color = SmartBusTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                // Distancias y paradero
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    candidate.distanceToUserMeters?.let { uDist ->
                        if (uDist < 50000f) {
                            Text(
                                text = "🚶 " + formatMeters(uDist) + " de ti",
                                fontSize = 10.sp,
                                fontFamily = SmartBusBody,
                                color = SmartBusTextSecondary
                            )
                        }
                    }

                    candidate.distanceToDestinationMeters?.let { dDist ->
                        Text(
                            text = "📍 A " + formatMeters(dDist),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = SmartBusBody,
                            color = AppPalette.primary
                        )
                    }

                    Text(
                        text = "• " + candidate.buses.size + " bus(es)",
                        fontSize = 10.sp,
                        fontFamily = SmartBusBody,
                        color = SmartBusTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Seleccionar",
                tint = AppPalette.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun formatMeters(meters: Float): String =
    if (meters >= 1000f) {
        String.format("%.1f km", meters / 1000f)
    } else {
        "" + meters.toInt() + " m"
    }
