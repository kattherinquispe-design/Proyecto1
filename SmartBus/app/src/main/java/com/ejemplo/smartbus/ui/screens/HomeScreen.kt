package com.ejemplo.smartbus.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ejemplo.smartbus.data.FirebaseParaderoRepository
import com.ejemplo.smartbus.data.TripHistoryRepository
import com.ejemplo.smartbus.data.model.BusRoute
import com.ejemplo.smartbus.data.model.BusStop
import com.ejemplo.smartbus.ui.components.MistigoLogo
import com.ejemplo.smartbus.ui.components.SmartBusBottomBar
import com.ejemplo.smartbus.ui.components.SmartBusTab
import com.ejemplo.smartbus.ui.theme.AppPalette
import com.ejemplo.smartbus.ui.theme.SmartBusBackground
import com.ejemplo.smartbus.ui.theme.SmartBusBody
import com.ejemplo.smartbus.ui.theme.SmartBusBlue
import com.ejemplo.smartbus.ui.theme.SmartBusDisplay
import com.ejemplo.smartbus.ui.theme.SmartBusGreen
import com.ejemplo.smartbus.ui.theme.SmartBusText
import com.ejemplo.smartbus.ui.theme.SmartBusTextLight
import com.ejemplo.smartbus.ui.theme.SmartBusTextSecondary
import com.google.android.gms.maps.model.LatLng
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    points: Int,
    userName: String = "Viajero",
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
    trackingState: String = "",
    userLocation: LatLng? = null,
    routes: List<BusRoute> = emptyList(),
    onRouteClick: ((BusRoute) -> Unit)? = null,
    onStopClick: ((BusStop) -> Unit)? = null
) {
    val context = LocalContext.current
    val tripHistoryRepo = remember(context) { TripHistoryRepository.get(context) }
    val frequentRoutes by tripHistoryRepo.frequentRoutes.collectAsState()

    // Cargar paraderos desde Firebase
    LaunchedEffect(Unit) {
        FirebaseParaderoRepository.loadStops(context)
    }

    val paraderosState by FirebaseParaderoRepository.stops.collectAsState()
    val nearbyStopsWithDist = remember(paraderosState, userLocation) {
        FirebaseParaderoRepository.getStopsNear(
            userLat = userLocation?.latitude,
            userLng = userLocation?.longitude,
            limit = 3
        )
    }

    val nextRewardPoints = when {
        points < 20 -> 20
        points < 40 -> 40
        points < 60 -> 60
        points < 80 -> 80
        points < 100 -> 100
        else -> 160
    }
    val progress = (points.toFloat() / nextRewardPoints.coerceAtLeast(1)).coerceIn(0f, 1f)
    val remainingPoints = (nextRewardPoints - points).coerceAtLeast(0)

    Scaffold(
        containerColor = SmartBusBackground,
        bottomBar = {
            SmartBusBottomBar(
                selectedTab = SmartBusTab.HOME,
                onTabSelected = { tab ->
                    when (tab) {
                        SmartBusTab.HOME -> Unit
                        SmartBusTab.MAP -> onMapClick()
                        SmartBusTab.REWARDS -> onRewardsClick()
                        SmartBusTab.RANKING -> onHistoryClick()
                        SmartBusTab.PROFILE -> onProfileClick()
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            // =======================================================
            // 1. LOGO MISTIGO
            // =======================================================
            MistigoLogo(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            // =======================================================
            // 2. ENCABEZADO ELEGANTE Y DINÁMICO (Sigue la paleta de Configuración)
            // =======================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    AppPalette.primary,
                                    AppPalette.primaryDark
                                )
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hola, $userName",
                                fontFamily = SmartBusDisplay,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "¡Sigue viajando y gana puntos!",
                                fontFamily = SmartBusBody,
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.92f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.18f))
                                    .clickable { onNotificationsClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsNone,
                                    contentDescription = "Notificaciones",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.18f))
                                    .clickable { onSettingsClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Configuración",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =======================================================
            // 3. BARRA RÁPIDA DE BÚSQUEDA DE DESTINO
            // =======================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSearchClick() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppPalette.primarySoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = AppPalette.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "¿A dónde quieres ir hoy?",
                            fontFamily = SmartBusDisplay,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = SmartBusText
                        )
                        Text(
                            text = "Busca en un radio de 400 m rutas y paraderos",
                            fontFamily = SmartBusBody,
                            fontSize = 12.sp,
                            color = SmartBusTextSecondary
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowForwardIos,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = SmartBusTextLight
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =======================================================
            // 4. TARJETA DE PUNTOS Y RECOMPENSAS (Dinámica con la Paleta)
            // =======================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRewardsClick() },
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    AppPalette.primaryDark,
                                    AppPalette.primary
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "$points puntos",
                                    fontFamily = SmartBusDisplay,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (points >= 20) {
                                        "¡Ya tienes puntos para canjear! (+20 pts por viaje en paradero)"
                                    } else {
                                        "Te faltan $remainingPoints puntos para tu primera recompensa (+20 pts por viaje)"
                                    },
                                    fontFamily = SmartBusBody,
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.92f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.30f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // =======================================================
            // 5. SECCION: PRÓXIMO BUS / HISTORIAL FRECUENTE
            // =======================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Próximo bus",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SmartBusText
                    )
                    Text(
                        text = if (frequentRoutes.isNotEmpty()) "Basado en tu historial de viajes frecuentes" else "Rutas más populares de Arequipa",
                        fontSize = 11.sp,
                        color = SmartBusTextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SmartBusBlue.copy(alpha = 0.08f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = SmartBusBlue,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Historial",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SmartBusBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Si hay historial de rutas tomadas, mostramos hasta las 3 más frecuentes
            if (frequentRoutes.isNotEmpty()) {
                frequentRoutes.take(3).forEach { freq ->
                    val matchedRoute = routes.firstOrNull { it.id == freq.routeId || it.code.equals(freq.code, ignoreCase = true) }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                if (matchedRoute != null && onRouteClick != null) {
                                    onRouteClick(matchedRoute)
                                } else {
                                    onMapClick()
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(AppPalette.primarySoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsBus,
                                    contentDescription = null,
                                    tint = AppPalette.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = freq.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF1E293B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${freq.origin} → ${freq.destination}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFD1FAE5))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "● Llegando",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF059669)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFF1F5F9))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Usado ${freq.tripCount} veces",
                                            fontSize = 10.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "5",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = "min",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            } else {
                // Si no hay historial aún, mostramos las rutas destacadas de Firebase
                val sampleRoutes = routes.take(2).ifEmpty {
                    listOf(
                        BusRoute(id = 1, code = "Ruta 01", name = "Ruta 01", origin = "Ciudad Blanca", destination = "Centro"),
                        BusRoute(id = 2, code = "Ruta 02", name = "Ruta 02", origin = "Paucarpata", destination = "Centro")
                    )
                }

                sampleRoutes.forEach { r ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                if (onRouteClick != null) onRouteClick(r) else onMapClick()
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(AppPalette.primarySoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsBus,
                                    contentDescription = null,
                                    tint = AppPalette.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = r.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF1E293B)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${r.origin} → ${r.destination}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFD1FAE5))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "● Llegando",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF059669)
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "5",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = "min",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // =======================================================
            // 6. SECCION: PARADEROS CERCANOS (GPS TIEMPO REAL)
            // =======================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Paraderos cercanos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                Text(
                    text = "Ver todos",
                    fontFamily = SmartBusDisplay,
                    color = AppPalette.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onMapClick() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            nearbyStopsWithDist.forEach { (stop, distMeters) ->
                val distanceText = if (userLocation != null) {
                    if (distMeters < 1000f) {
                        "${distMeters.roundToInt()} m"
                    } else {
                        String.format("%.1f km", distMeters / 1000f)
                    }
                } else {
                    "350 m"
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            if (onStopClick != null) onStopClick(stop) else onMapClick()
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppPalette.primarySoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = AppPalette.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Paradero ${stop.name.removePrefix("Paradero ").trim()}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Color(0xFF1E293B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = distanceText,
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ArrowForwardIos,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFFCBD5E1)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // =======================================================
            // 7. ESTADO DEL SEGUIMIENTO GPS
            // =======================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTrackingClick() }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (trackingActive) {
                        "[ACTIVO] MistiGo está detectando tu recorrido"
                    } else {
                        "[INACTIVO] MistiGo • Toca para activar seguimiento"
                    },
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}
