package com.ejemplo.smartbus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ejemplo.smartbus.data.driver.DriverBlocker
import com.ejemplo.smartbus.data.model.BusRoute
import com.ejemplo.smartbus.data.model.DriverShift
import com.ejemplo.smartbus.data.model.DriverTrip
import com.ejemplo.smartbus.data.model.DriverVehicle
import com.ejemplo.smartbus.ui.theme.SmartBusBlue
import com.ejemplo.smartbus.ui.theme.SmartBusGreen
import com.ejemplo.smartbus.ui.theme.SmartBusRed
import com.ejemplo.smartbus.ui.theme.SmartBusText
import com.ejemplo.smartbus.ui.theme.SmartBusTextSecondary
import com.ejemplo.smartbus.ui.theme.SmartBusYellow
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Ventana del conductor.
 *
 * Minimalista a proposito: el conductor esta manejando, no quiere
 * menus. Todo lo que ve es el mapa con la ruta que le toca y un solo
 * boton grande para empezar o terminar de trabajar.
 */
@Composable
fun DriverHomeScreen(
    vehicle: DriverVehicle?,
    shift: DriverShift?,
    blockers: List<DriverBlocker>,
    isWorking: Boolean,
    route: BusRoute?,
    availableRoutes: List<BusRoute> = emptyList(),
    onSelectRoute: (BusRoute) -> Unit = {},
    onToggleOnline: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenDocuments: () -> Unit,
    onOpenVehicle: () -> Unit,
    onOpenEarnings: () -> Unit,
    onSwitchToPassenger: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRouteSelector by remember { mutableStateOf(false) }

    /**
     * Posicion del conductor en el mapa, solo si esta trabajando y su
     * GPS llego hace poco. Un punto viejo no sirve para nada.
     */
    val driverPosition: LatLng? = remember(shift, isWorking) {
        if (isWorking && shift != null && shift.hasFreshLocation()) {
            val lat = shift.lastLatitude
            val lng = shift.lastLongitude
            if (lat != null && lng != null) LatLng(lat, lng) else null
        } else {
            null
        }
    }

    val routeSubida = remember(route) { route?.subidaPoints.orEmpty() }
    val routeBajada = remember(route) { route?.bajadaPoints.orEmpty() }
    val routeAllPoints = remember(route) {
        (route?.subidaPoints.orEmpty() + route?.bajadaPoints.orEmpty())
            .ifEmpty { route?.points.orEmpty() }
    }

    val routeStart = remember(routeAllPoints) {
        routeAllPoints.firstOrNull()?.let {
            LatLng(it.latitude, it.longitude)
        }
    }

    // El comienzo de la ruta hace de fondo cuando el GPS todavia no
    // ha devuelto ninguna posicion (Arequipa por defecto).
    val initial = driverPosition ?: routeStart ?: LatLng(-16.4090, -71.5375)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initial, 13f)
    }

    val scope = rememberCoroutineScope()

    var centeredOnDriver by remember { mutableStateOf(false) }

    // Al empezar a trabajar la camara sigue al conductor la primera
    // vez; despues el puede moverla a mano sin que lo persiga.
    LaunchedEffect(driverPosition) {
        val position = driverPosition
        if (!centeredOnDriver && position != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(position, 15f),
                600
            )
            centeredOnDriver = true
        }
    }

    // Cuando el conductor escoge su ruta de trabajo, centrar el mapa en esa ruta
    LaunchedEffect(route?.id) {
        val start = routeStart
        if (start != null && driverPosition == null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(start, 13.5f),
                600
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = false,
                mapType = MapType.NORMAL,
                minZoomPreference = 11f,
                maxZoomPreference = 20f
            ),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = false,
                zoomControlsEnabled = false,
                compassEnabled = true,
                mapToolbarEnabled = false
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 130.dp, bottom = 180.dp)
        ) {
            // Se marca UNICAMENTE la ruta donde trabaja el conductor
            val routeColor = SmartBusBlue

            if (routeSubida.size >= 2 || routeBajada.size >= 2) {
                if (routeSubida.size >= 2) {
                    Polyline(
                        points = routeSubida.map { LatLng(it.latitude, it.longitude) },
                        color = routeColor,
                        width = 11f
                    )
                }
                if (routeBajada.size >= 2) {
                    Polyline(
                        points = routeBajada.map { LatLng(it.latitude, it.longitude) },
                        color = routeColor.copy(alpha = 0.78f),
                        width = 9f
                    )
                }
            } else if (routeAllPoints.size >= 2) {
                Polyline(
                    points = routeAllPoints.map { LatLng(it.latitude, it.longitude) },
                    color = routeColor,
                    width = 10f
                )
            }

            // Punto de partida y llegada de la ruta elegida
            if (route != null) {
                routeAllPoints.firstOrNull()?.let { start ->
                    Marker(
                        state = MarkerState(LatLng(start.latitude, start.longitude)),
                        title = route.origin.ifBlank { route.name },
                        snippet = "Inicio de ruta (${route.code})"
                    )
                }

                routeAllPoints.lastOrNull()?.let { end ->
                    Marker(
                        state = MarkerState(LatLng(end.latitude, end.longitude)),
                        title = route.destination.ifBlank { route.name },
                        snippet = "Fin de ruta (${route.code})"
                    )
                }
            }

            driverPosition?.let { position ->
                Marker(
                    state = MarkerState(position),
                    title = "Tu vehiculo",
                    snippet = vehicle?.plate ?: "En servicio"
                )
            }
        }

        // ---- Encabezado: que ruta me toca hoy + boton para elegir ruta ----

        Card(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 14.dp, top = 14.dp, end = 190.dp)
                .clickable { showRouteSelector = true },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {

                if (route != null) {
                    Text(
                        text = "${route.code} · ${route.name}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SmartBusText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (route.origin.isNotBlank() || route.destination.isNotBlank()) {
                        Text(
                            text = "${route.origin} → ${route.destination}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SmartBusTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(
                        text = "Sin ruta seleccionada",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SmartBusRed
                    )
                    Text(
                        text = "Toca aquí para escoger tu ruta de trabajo",
                        style = MaterialTheme.typography.bodySmall,
                        color = SmartBusBlue
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isWorking) SmartBusGreen
                                else SmartBusYellow
                            )
                    )
                    Text(
                        text = if (isWorking) "En servicio" else "Desconectado",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isWorking) SmartBusGreen else SmartBusTextSecondary
                    )
                    Text(
                        text = "· Cambiar ruta",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SmartBusBlue
                    )
                }
            }
        }

        // ---- Accesos rapidos, discretos para no robar atencion ----

        Card(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 14.dp, end = 14.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(modifier = Modifier.padding(4.dp)) {
                IconButton(onClick = { showRouteSelector = true }) {
                    Icon(
                        Icons.Default.Route,
                        contentDescription = "Elegir ruta de trabajo",
                        tint = SmartBusBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onOpenProfile) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Mis datos",
                        tint = SmartBusBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onOpenDocuments) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = "Mis documentos",
                        tint = SmartBusBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onOpenVehicle) {
                    Icon(
                        Icons.Default.DirectionsCar,
                        contentDescription = "Mi vehiculo",
                        tint = SmartBusBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onOpenEarnings) {
                    Icon(
                        Icons.Default.Payments,
                        contentDescription = "Mis ganancias",
                        tint = SmartBusBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // ---- Boton principal y avisos, apilados abajo sin solaparse ----

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!isWorking && blockers.isNotEmpty()) {
                val primaryBlocker = blockers.first()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            when {
                                primaryBlocker.title.contains("documento", ignoreCase = true) ||
                                    primaryBlocker.title.contains("rechazado", ignoreCase = true) ->
                                    onOpenDocuments()
                                primaryBlocker.title.contains("vehiculo", ignoreCase = true) ||
                                    primaryBlocker.title.contains("SOAT", ignoreCase = true) ||
                                    primaryBlocker.title.contains("Inspeccion", ignoreCase = true) ->
                                    onOpenVehicle()
                                primaryBlocker.title.contains("ruta", ignoreCase = true) ->
                                    if (vehicle != null && availableRoutes.isNotEmpty()) {
                                        showRouteSelector = true
                                    } else {
                                        onOpenVehicle()
                                    }
                                else -> onOpenProfile()
                            }
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF8E1)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Block,
                            contentDescription = null,
                            tint = SmartBusRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🔒 Seguridad activa · Pendientes (${blockers.size}): ${primaryBlocker.title}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = SmartBusText
                            )
                            Text(
                                text = "${primaryBlocker.detail} (Toca aquí para completar)",
                                style = MaterialTheme.typography.bodySmall,
                                color = SmartBusTextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when {
                                isWorking ->
                                    "✅ En línea: tu bus y ruta se transmiten en vivo."

                                blockers.isEmpty() ->
                                    "✅ Verificado y seguro. Listo para salir en ruta."

                                else ->
                                    "Completa tus requisitos y ruta para conectarte con pasajeros."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (blockers.isEmpty() || isWorking) SmartBusGreen else SmartBusTextSecondary,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedButton(
                            onClick = { showRouteSelector = true },
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Route,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (route != null) "Ruta: ${route.code}" else "Elegir ruta",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onToggleOnline,
                        enabled = isWorking || blockers.isEmpty(),
                        colors = if (isWorking) {
                            ButtonDefaults.buttonColors(
                                containerColor = SmartBusRed,
                                contentColor = Color.White
                            )
                        } else {
                            ButtonDefaults.buttonColors(
                                containerColor = SmartBusBlue,
                                contentColor = Color.White
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = if (isWorking) {
                                "Terminar servicio"
                            } else if (blockers.isEmpty()) {
                                "Empezar a conducir"
                            } else {
                                "🔒 Completa requisitos para conectarte"
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Boton pequeno y amarillo para salir del modo conductor
                    // sin tener que buscar el menu: esta siempre a la vista.
                    Button(
                        onClick = onSwitchToPassenger,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SmartBusYellow,
                            contentColor = SmartBusText
                        ),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .height(38.dp)
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Modo pasajero",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // ---- Volver a centrarse en el conductor ----

        FloatingActionButton(
            onClick = {
                val target = driverPosition ?: routeStart
                if (target != null) {
                    scope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(target, 15f),
                            500
                        )
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 210.dp),
            containerColor = Color.White,
            contentColor = SmartBusBlue,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
        ) {
            Icon(
                Icons.Default.MyLocation,
                contentDescription = "Centrar en mi posicion"
            )
        }

        // ---- Dialogo para elegir la ruta donde trabaja el conductor ----
        if (showRouteSelector) {
            AlertDialog(
                onDismissRequest = { showRouteSelector = false },
                title = {
                    Text(
                        text = "🛣️ Escoge la ruta donde trabajas",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Se marcará únicamente tu ruta seleccionada en el mapa y los pasajeros te verán en esta línea:",
                            style = MaterialTheme.typography.bodySmall,
                            color = SmartBusTextSecondary
                        )
                        if (availableRoutes.isEmpty()) {
                            Text(
                                text = "Cargando rutas disponibles...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SmartBusText
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 340.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(availableRoutes, key = { it.id }) { itemRoute ->
                                    val selected = route?.id == itemRoute.id
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onSelectRoute(itemRoute)
                                                showRouteSelector = false
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (selected) {
                                                SmartBusBlue.copy(alpha = 0.12f)
                                            } else {
                                                Color(0xFFF5F7FA)
                                            }
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (selected) SmartBusGreen else SmartBusBlue
                                                    )
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${itemRoute.code} - ${itemRoute.name}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SmartBusText
                                                )
                                                if (itemRoute.origin.isNotBlank() || itemRoute.destination.isNotBlank()) {
                                                    Text(
                                                        text = "${itemRoute.origin} → ${itemRoute.destination}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = SmartBusTextSecondary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            if (selected) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = "Seleccionada",
                                                    tint = SmartBusGreen,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showRouteSelector = false }) {
                        Text("Cerrar")
                    }
                }
            )
        }
    }
}

/**
 * Ganancias del dia. El filtro usa el dia local, no el UTC, porque
 * el conductor razona con su reloj de pared.
 */
internal fun todayTrips(trips: List<DriverTrip>): List<DriverTrip> {

    val formatter = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

    val today = formatter.format(Date())

    return trips.filter { trip ->
        formatter.format(Date(trip.startedAt)) == today
    }
}