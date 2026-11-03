package com.ejemplo.smartbus.ui.screens

import android.location.Geocoder
import com.ejemplo.smartbus.data.TripHistoryRepository
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ejemplo.smartbus.data.FirebaseRouteRepository
import com.ejemplo.smartbus.data.model.BusRoute
import com.ejemplo.smartbus.ui.theme.SmartBusBlue
import com.ejemplo.smartbus.ui.theme.SmartBusGreen
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class GeocodeResult(
    val name: String,
    val address: String,
    val location: LatLng
)

@Composable
fun DestinationSearchScreen(
    onBack: () -> Unit,
    onLocationSelected: (LatLng, String) -> Unit,
    onRouteSelected: ((BusRoute) -> Unit)? = null,
    currentLocation: LatLng? = null
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    val results = remember { mutableStateListOf<GeocodeResult>() }
    val routeResults = remember { mutableStateListOf<BusRoute>() }
    var isSearching by remember { mutableStateOf(false) }
    var showResults by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    // Al abrir la pantalla, cargar rutas disponibles de Firebase
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
        val initialRoutes = FirebaseRouteRepository.loadRoutes()
        routeResults.clear()
        routeResults.addAll(initialRoutes.take(12))
    }

    fun clearResults() {
        results.clear()
        routeResults.clear()
        showResults = false
        isSearching = false
    }

    fun performSearch(text: String) {
        searchJob?.cancel()
        if (text.isBlank()) {
            coroutineScope.launch {
                val initialRoutes = FirebaseRouteRepository.loadRoutes()
                withContext(Dispatchers.Main) {
                    routeResults.clear()
                    routeResults.addAll(initialRoutes.take(12))
                    results.clear()
                    isSearching = false
                    showResults = false
                }
            }
            return
        }

        searchJob = coroutineScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { isSearching = true }

            // 1. Buscar en Firebase Firestore colección "rutas"
            val matchedFirebaseRoutes = FirebaseRouteRepository.searchRoutes(text)

            // 2. Buscar en Geocoder de Google Maps
            val geocodeList = mutableListOf<GeocodeResult>()
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocationName(text, 8)
                addresses?.forEach { addr ->
                    val lat = addr.latitude
                    val lon = addr.longitude
                    if (lat != 0.0 && lon != 0.0) {
                        val title = usableName(
                            listOfNotNull(
                                addr.featureName,
                                addr.thoroughfare,
                                addr.locality,
                                addr.subAdminArea,
                                addr.adminArea
                            ),
                            text
                        )
                        val fullAddress = buildString {
                            val parts = listOfNotNull(
                                addr.thoroughfare,
                                addr.subThoroughfare,
                                addr.locality,
                                addr.adminArea,
                                addr.countryName
                            )
                            append(parts.joinToString(", "))
                            if (isEmpty()) append("$lat, $lon")
                        }
                        geocodeList.add(
                            GeocodeResult(
                                name = title,
                                address = fullAddress.ifBlank { "$lat,$lon" },
                                location = LatLng(lat, lon)
                            )
                        )
                    }
                }
            } catch (_: Exception) {
            }

            withContext(Dispatchers.Main) {
                routeResults.clear()
                routeResults.addAll(matchedFirebaseRoutes)

                results.clear()
                results.addAll(geocodeList.distinctBy { it.location.latitude to it.location.longitude })

                showResults = true
                isSearching = false
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFFF5F7FA)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                onBack()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Volver"
                            )
                        }

                        OutlinedTextField(
                            value = query,
                            onValueChange = {
                                query = it
                                performSearch(it)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            placeholder = {
                                Text("Busca ruta, paradero o lugar...", color = Color(0xFF9AA5B1))
                            },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Buscar",
                                    tint = SmartBusBlue
                                )
                            },
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = {
                                        query = ""
                                        performSearch("")
                                        focusRequester.requestFocus()
                                        keyboardController?.show()
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Limpiar"
                                        )
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    if (routeResults.isNotEmpty()) {
                                        val firstRoute = routeResults.first()
                                        if (onRouteSelected != null) {
                                            onRouteSelected(firstRoute)
                                        } else {
                                            val pt = firstRoute.points.firstOrNull()
                                            if (pt != null) {
                                                onLocationSelected(LatLng(pt.latitude, pt.longitude), firstRoute.name)
                                            }
                                        }
                                    } else if (results.isNotEmpty()) {
                                        val firstPlace = results.first()
                                        onLocationSelected(firstPlace.location, firstPlace.name)
                                    }
                                }
                            ),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SmartBusBlue,
                                unfocusedBorderColor = Color(0xFFE5E7EB),
                                focusedContainerColor = Color(0xFFF8FAFF),
                                unfocusedContainerColor = Color(0xFFF2F4F7)
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    AnimatedVisibility(visible = isSearching) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 44.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(SmartBusBlue)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Buscando en Firebase y mapa...", fontSize = 13.sp, color = Color(0xFF64748B))
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Opción rápida de usar ubicación actual
                item {
                    AnimatedVisibility(visible = currentLocation != null && query.isBlank()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    currentLocation?.let {
                                        onLocationSelected(it, "Mi ubicación actual")
                                    }
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SmartBusBlue.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = null,
                                        tint = SmartBusBlue
                                    )
                                }
                                Column {
                                    Text(
                                        "Usar mi ubicación actual",
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1F2937)
                                    )
                                    Text(
                                        "Buscar buses que pasen por donde estoy",
                                        fontSize = 13.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }
                }

                // =======================================================
                // SECCION: RUTAS REALES DESDE FIREBASE
                // =======================================================
                if (routeResults.isNotEmpty()) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                        ) {
                            Text(
                                text = if (query.isBlank()) "Rutas de Bus disponibles (Firebase)" else "Rutas encontradas en Firebase (${routeResults.size})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SmartBusBlue
                            )
                        }
                    }

                    items(routeResults, key = { "route_${it.id}" }) { route ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    TripHistoryRepository.get(context).recordTrip(route)
                                    if (onRouteSelected != null) {
                                        onRouteSelected(route)
                                    } else {
                                        val pt = route.points.firstOrNull() ?: route.subidaPoints.firstOrNull()
                                        if (pt != null) {
                                            onLocationSelected(LatLng(pt.latitude, pt.longitude), route.name)
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SmartBusBlue.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsBus,
                                        contentDescription = null,
                                        tint = SmartBusBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = route.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF1F2937),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${route.origin} → ${route.destination}",
                                        fontSize = 13.sp,
                                        color = Color(0xFF64748B),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(SmartBusBlue.copy(alpha = 0.08f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "Subida: ${route.subidaPoints.size} pts",
                                                fontSize = 11.sp,
                                                color = SmartBusBlue,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(SmartBusGreen.copy(alpha = 0.12f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "Bajada: ${route.bajadaPoints.size} pts",
                                                fontSize = 11.sp,
                                                color = SmartBusGreen,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }

                // =======================================================
                // SECCION: DIRECCIONES Y LUGARES DE GOOGLE MAPS
                // =======================================================
                if (showResults && results.isNotEmpty()) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                        ) {
                            Text(
                                text = "Direcciones y Lugares (${results.size})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569)
                            )
                        }
                    }

                    items(results, key = { "${it.location.latitude},${it.location.longitude}" }) { r ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    onLocationSelected(r.location, r.name)
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
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFF1F5F9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = Color(0xFF475569)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        r.name,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1F2937),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        r.address,
                                        fontSize = 13.sp,
                                        color = Color(0xFF64748B),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }

                if (query.isNotBlank() && !isSearching && (!showResults || (results.isEmpty() && routeResults.isEmpty()))) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "No se encontraron rutas ni lugares para \"$query\"",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Filtra nombres genéricos devueltos por el geocoder.
 */
private fun usableName(
    candidates: List<String>,
    fallbackQuery: String
): String {
    val cleaned = candidates
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .filter { candidate ->
            val hasLetters = candidate.any { it.isLetter() }
            val isOnlyNumber = candidate.all { it.isDigit() || it == '-' || it == ' ' }
            val isGeneric = candidate.lowercase() in GENERIC_PLACE_NAMES
            hasLetters && !isOnlyNumber && !isGeneric
        }

    return cleaned.firstOrNull()
        ?: fallbackQuery.trim().ifBlank { "Destino seleccionado" }
}

private val GENERIC_PLACE_NAMES = setOf(
    "unnamed road",
    "unnamed",
    "null",
    "none"
)
