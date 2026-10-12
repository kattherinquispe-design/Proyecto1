package com.ejemplo.smartbus.ui.screens

import android.location.Geocoder
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
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
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
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
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
    currentLocation: LatLng? = null
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    val results = remember { mutableStateListOf<GeocodeResult>() }
    var isSearching by remember { mutableStateOf(false) }
    var showResults by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    fun clearResults() {
        results.clear()
        showResults = false
        isSearching = false
    }

    fun searchPlaces(text: String) {
        if (text.isBlank()) {
            clearResults()
            return
        }
        coroutineScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { isSearching = true }
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocationName(text, 10)
                val list = mutableListOf<GeocodeResult>()
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
                        list.add(
                            GeocodeResult(
                                name = title,
                                address = fullAddress.ifBlank { "$lat,$lon" },
                                location = LatLng(lat, lon)
                            )
                        )
                    }
                }
                withContext(Dispatchers.Main) {
                    results.clear()
                    results.addAll(list.distinctBy { it.location.latitude to it.location.longitude })
                    showResults = results.isNotEmpty()
                    isSearching = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isSearching = false
                    showResults = false
                }
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
                                searchPlaces(it)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            placeholder = {
                                Text("¿A dónde quieres ir?", color = Color(0xFF9AA5B1))
                            },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Buscar",
                                    tint = Color(0xFF1A73E8)
                                )
                            },
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = {
                                        query = ""
                                        clearResults()
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
                                    if (results.isNotEmpty()) {
                                        val first = results.first()
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                        onLocationSelected(first.location, first.name)
                                    } else if (query.isNotBlank()) {
                                        searchPlaces(query)
                                    } else {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                    }
                                }
                            ),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1A73E8),
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
                                    .background(Color(0xFF1A73E8))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Buscando...", fontSize = 13.sp, color = Color(0xFF64748B))
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    AnimatedVisibility(visible = currentLocation != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    onLocationSelected(
                                        currentLocation!!,
                                        "Mi ubicación actual"
                                    )
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
                                        .background(Color(0xFF1A73E8).copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = null,
                                        tint = Color(0xFF1A73E8)
                                    )
                                }
                                Column {
                                    Text(
                                        "Usar mi ubicación actual",
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1F2937)
                                    )
                                    Text(
                                        "Ir desde donde estoy ahora",
                                        fontSize = 13.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }
                }

                if (showResults && results.isNotEmpty()) {
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

                if (query.isNotBlank() && !isSearching && (!showResults || results.isEmpty())) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Pulsa «Hecho» (Done) o espera para buscar",
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
 * El geocoder del dispositivo puede devolver nombres inservibles
 * (solo numeros o genericos). Se descartan para no mostrar un
 * destino sin sentido en el mapa.
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