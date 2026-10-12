package com.ejemplo.smartbus.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
    onToggleOnline: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenDocuments: () -> Unit,
    onOpenVehicle: () -> Unit,
    onOpenEarnings: () -> Unit,
    onSwitchToPassenger: () -> Unit,
    modifier: Modifier = Modifier
) {

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

    val routeStart = route?.points?.firstOrNull()?.let {
        LatLng(it.latitude, it.longitude)
    }

    // El comienzo de la ruta hace de fondo cuando el GPS todavia no
    // ha devuelto ninguna posicion.
    val initial = driverPosition ?: routeStart ?: LatLng(-12.0464, -77.0428)

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

    Box(modifier = modifier.fillMaxSize()) {

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = false,
                mapType = MapType.NORMAL,
                minZoomPreference = 12f,
                maxZoomPreference = 20f
            ),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = false,
                zoomControlsEnabled = false,
                compassEnabled = true,
                mapToolbarEnabled = false
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 120.dp, bottom = 120.dp)
        ) {
            // La ruta asignada, dibujada de la misma forma que la ve el
            // pasajero, para que el conductor confie en lo que muestra.
            route?.points?.takeIf { it.size >= 2 }?.let { points ->
                Polyline(
                    points = points.map { LatLng(it.latitude, it.longitude) },
                    color = SmartBusBlue,
                    width = 9f
                )
            }

            // Punto de partida y llegada de la ruta, para que de un
            // vistazo el conductor sepa hacia donde va.
            route?.points?.firstOrNull()?.let { start ->
                Marker(
                    state = MarkerState(LatLng(start.latitude, start.longitude)),
                    title = route.origin,
                    snippet = "Inicio de ruta"
                )
            }

            route?.points?.lastOrNull()?.let { end ->
                Marker(
                    state = MarkerState(LatLng(end.latitude, end.longitude)),
                    title = route.destination,
                    snippet = "Fin de ruta"
                )
            }

            driverPosition?.let { position ->
                Marker(
                    state = MarkerState(position),
                    title = "Tu vehiculo",
                    snippet = vehicle?.plate ?: "En servicio"
                )
            }
        }

        // ---- Encabezado: que ruta me toca hoy ----

        Card(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 14.dp, top = 14.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {

                if (route != null) {
                    Text(
                        text = "${route.code} · ${route.name}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SmartBusText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${route.origin} → ${route.destination}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SmartBusTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = "Sin ruta asignada",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SmartBusText
                    )
                    Text(
                        text = "Asigna tu unidad en los ajustes del vehiculo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SmartBusTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isWorking) SmartBusGreen
                                else SmartBusYellow
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isWorking) "En servicio" else "Desconectado",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isWorking) SmartBusGreen else SmartBusTextSecondary
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

        // ---- Boton principal y avisos, pegados abajo ----

        if (!isWorking && blockers.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SmartBusYellow.copy(alpha = 0.12f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Block,
                        contentDescription = null,
                        tint = SmartBusYellow,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pendientes: ${blockers.size}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SmartBusText
                        )
                        Text(
                            text = blockers.first().title,
                            style = MaterialTheme.typography.bodySmall,
                            color = SmartBusTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = if (!isWorking && blockers.isNotEmpty()) 0.dp else 14.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                Text(
                    text = when {
                        isWorking ->
                            "Tu vehiculo se ve en el mapa del pasajero."

                        blockers.isEmpty() ->
                            "Estas listo. Enciende y el pasajero te vera llegar."

                        else ->
                            "Completa lo que falta para poder trabajar."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = SmartBusTextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

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
                        .height(54.dp)
                ) {
                    Text(
                        text = if (isWorking) "Terminar servicio" else "Empezar a conducir",
                        fontSize = 16.sp,
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
                        .height(40.dp)
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
                .padding(end = 16.dp, bottom = 150.dp),
            containerColor = Color.White,
            contentColor = SmartBusBlue,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
        ) {
            Icon(
                Icons.Default.MyLocation,
                contentDescription = "Centrar en mi posicion"
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