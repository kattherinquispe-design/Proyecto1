package com.ejemplo.smartbus.ui.screens

import android.provider.Settings
import android.net.Uri
import android.content.ContextWrapper
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.activity.result.IntentSenderRequest
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ejemplo.smartbus.data.BusLiveRepository
import com.ejemplo.smartbus.data.model.Bus
import com.ejemplo.smartbus.data.model.BusRoute
import com.ejemplo.smartbus.data.model.BusStop
import com.ejemplo.smartbus.location.BusStopDetector
import com.ejemplo.smartbus.location.FixQuality
import com.ejemplo.smartbus.location.LocationFixEvaluator
import com.ejemplo.smartbus.ui.theme.SmartBusBlue
import com.ejemplo.smartbus.ui.theme.SmartBusDivider
import com.ejemplo.smartbus.ui.theme.SmartBusGreen
import com.ejemplo.smartbus.ui.theme.SmartBusText
import com.ejemplo.smartbus.ui.theme.SmartBusTextSecondary
import com.ejemplo.smartbus.ui.theme.SmartBusTextLight
import com.ejemplo.smartbus.ui.theme.SmartBusYellow
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
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
import kotlin.math.roundToInt

private const val REQUEST_ENABLE_LOCATION = 4201

/**
 * Recuerda si ya le preguntamos al usuario por el permiso de ubicacion.
 * Sin esto no hay forma de distinguir "todavia no le hemos preguntado"
 * de "lo rechazo para siempre".
 */
private const val KEY_ASKED_LOCATION = "asked_location"
@Composable
fun SmartBusMapScreen(
    destination: LatLng? = null,
    destinationName: String? = null,
    onSearchClick: () -> Unit = {},
    onClearDestination: () -> Unit = {},
    stops: List<BusStop> = emptyList(),
    routes: List<BusRoute> = emptyList(),
    selectedRoute: BusRoute? = null,
    selectedStop: BusStop? = null,
    onStopClick: (BusStop) -> Unit = {},
    vehicles: List<Bus>? = null,
    onUserLocationChange: (LatLng) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var locationPermissionGranted by remember { mutableStateOf(false) }
    var hasFinePermission by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    var isWaitingForFix by remember { mutableStateOf(false) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var userLocationAccuracy by remember { mutableFloatStateOf(Float.MAX_VALUE) }
    var isGpsAccurate by remember { mutableStateOf(false) }
    var gpsSignalState by remember { mutableStateOf("Buscando ubicacion...") }
    var lastFixAt by remember { mutableLongStateOf(0L) }
    var lastFixElapsed by remember { mutableLongStateOf(0L) }
    var fixQuality by remember { mutableStateOf(FixQuality.STALE) }
    var isLocationServiceOn by remember { mutableStateOf(true) }
    var isPermanentlyDenied by remember { mutableStateOf(false) }
    var nearestStop by remember { mutableStateOf<BusStop?>(null) }
    var nearestDistance by remember { mutableStateOf<Float?>(null) }

    var isFollowingUser by remember { mutableStateOf(true) }
    var hasMovedManually by remember { mutableStateOf(false) }
    var hasCenteredOnce by remember { mutableStateOf(false) }
    var isProgrammaticMove by remember { mutableStateOf(false) }

    /**
     * Si la navegacion ya trae los vehiculos de la base de datos, se
     * usan esos. Si no, se recurre al repositorio en vivo para que el
     * mapa siga funcionando aislado.
     */
    val liveBuses by BusLiveRepository.buses.collectAsState(initial = emptyList<Bus>())

    val buses = vehicles ?: liveBuses

    LaunchedEffect(userLocation) {
        userLocation?.let { onUserLocationChange(it) }
    }

    val activity = context.findActivity()

    val permissionPrefs =
        remember(context) {
            context.getSharedPreferences("smartbus_permission", Context.MODE_PRIVATE)
        }

    /**
     * Android no dice si un permiso esta "para siempre" denegado:
     * shouldShowRequestPermissionRationale tambien devuelve false
     * antes de la primera pregunta. Por eso guardamos si ya se
     * pregunto, para no abrir Ajustes cuando el usuario todavia no
     * ha visto el dialogo (que era lo que impidia pedir el permiso).
     */
    fun updatePermissionState() {

        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        hasFinePermission = fine
        locationPermissionGranted = fine || coarse

        if (!locationPermissionGranted) {
            val asked = permissionPrefs.getBoolean(KEY_ASKED_LOCATION, false)
            val rationale = activity != null &&
                activity.shouldShowRequestPermissionRationale(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )

            isPermanentlyDenied = asked && !rationale
            showPermissionRationale = asked && rationale
        } else {
            isPermanentlyDenied = false
            showPermissionRationale = false
        }
    }

    /**
     * El GPS del telefono puede estar apagado. Con el permiso concedido
     * pero la ubicacion del sistema desactivada, Android no entrega
     * ninguna posicion: por eso lo comprobamos y lo pedimos explicitamente.
     */
    fun updateLocationServiceState() {

        val systemLocationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

        isLocationServiceOn =
            systemLocationManager?.isLocationEnabled() ?: true
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        permissionPrefs.edit().putBoolean(KEY_ASKED_LOCATION, true).apply()
        updatePermissionState()
    }

    fun requestLocationPermission() {
        permissionPrefs.edit().putBoolean(KEY_ASKED_LOCATION, true).apply()
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    /**
     * Abre los ajustes del sistema cuando el usuario ya denies el
     * permiso y Android deja de mostrar el dialogo.
     */
    fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:" + context.packageName)
        )
        runCatching { context.startActivity(intent) }
    }

    LaunchedEffect(Unit) {
        updatePermissionState()
        updateLocationServiceState()

        if (!locationPermissionGranted) {
            requestLocationPermission()
        }
    }

    /**
     * Al volver de Ajustes o al desbloquear el telefono, el permiso y el
     * GPS pueden haber cambiado. Sin esto, la app se quedaba mostrando
     * "sin permiso" o "sin senal" aunque ya estuviera todo activado.
     */
    DisposableEffect(Unit) {

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    updatePermissionState()
                    updateLocationServiceState()
                }
                else -> Unit
            }
        }

        (activity as? LifecycleOwner)?.lifecycle?.addObserver(observer)

        onDispose {
            (activity as? LifecycleOwner)?.lifecycle?.removeObserver(observer)
        }
    }

    val fusedClient: FusedLocationProviderClient = remember(context) {
        LocationServices.getFusedLocationProviderClient(context)
    }

    val settingsResolutionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { }

    fun enableLocationSettings(context: Context, onReady: () -> Unit) {
        val settingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(
                LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L).build()
            )
            .setAlwaysShow(true)
            .build()

        LocationServices.getSettingsClient(context)
            .checkLocationSettings(settingsRequest)
            .addOnSuccessListener {
                updateLocationServiceState()
                onReady()
            }
            .addOnFailureListener { error ->
                if (error is ResolvableApiException) {
                    try {
                        settingsResolutionLauncher.launch(
                            IntentSenderRequest.Builder(error.resolution).build()
                        )
                    } catch (_: Exception) {
                    }
                }
                onReady()
            }
    }

    val initialLocation: LatLng = when {
        userLocation != null -> userLocation!!
        destination != null -> destination
        selectedRoute != null && selectedRoute.points.isNotEmpty() ->
            LatLng(selectedRoute.points.first().latitude, selectedRoute.points.first().longitude)
        selectedStop != null -> LatLng(selectedStop.latitude, selectedStop.longitude)
        else -> LatLng(0.0, 0.0)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            initialLocation,
            if (destination != null) 16f else 2f
        )
    }

    var activeCallback: LocationCallback? by remember { mutableStateOf(null) }

    /**
     * Aplica una lectura de GPS ya validada: centra la camara si toca,
     * actualiza la precision y busca el paradero mas cercano.
     */
    fun applyFix(location: Location, quality: FixQuality, nowElapsed: Long) {

        lastFixElapsed = nowElapsed
        lastFixAt = System.currentTimeMillis()
        userLocationAccuracy = location.accuracy
        fixQuality = quality
        isGpsAccurate =
            quality == FixQuality.EXCELLENT ||
                quality == FixQuality.GOOD
        isWaitingForFix = false
        gpsSignalState = LocationFixEvaluator.labelOf(quality)

        val currentLatLng = LatLng(location.latitude, location.longitude)
        userLocation = currentLatLng

        if (!hasCenteredOnce || (isFollowingUser && !hasMovedManually)) {
            val isFirstFix = !hasCenteredOnce
            hasCenteredOnce = true
            isFollowingUser = true
            hasMovedManually = false
            isProgrammaticMove = true
            scope.launch {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f),
                    if (isFirstFix) 700 else 250
                )
                isProgrammaticMove = false
            }
        }

        var closest: BusStop? = null
        var closestDistance = Float.MAX_VALUE
        stops.forEach { stop ->
            val distance = BusStopDetector.calculateDistance(location, stop)
            if (distance < closestDistance) {
                closestDistance = distance
                closest = stop
            }
        }
        nearestStop = closest
        nearestDistance =
            if (closestDistance != Float.MAX_VALUE) closestDistance else null
    }

    /**
     * Evalua una lectura de GPS y la aplica si esUtil.
     *
     * Las lecturas en vivo siempre se evaluan con [isCachedHint] = false,
     * de modo que nunca se descartan por estar lejos de una posicion
     * anterior. Eso era lo que dejaba al usuario clavado en un barrio
     * donde nunca estuvo.
     */
    fun acceptFix(location: Location, isCachedHint: Boolean): Boolean {

        val nowElapsed = SystemClock.elapsedRealtime()

        val decision = LocationFixEvaluator.evaluate(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = location.accuracy,
            elapsedRealtimeMs = nowElapsed,
            fixElapsedRealtimeMs =
                if (location.elapsedRealtimeNanos > 0L) {
                    location.elapsedRealtimeNanos / 1_000_000L
                } else {
                    0L
                },
            currentLatitude = userLocation?.latitude,
            currentLongitude = userLocation?.longitude,
            currentAcceptedAtElapsedMs = lastFixElapsed,
            isCachedHint = isCachedHint
        )

        if (!decision.accept) {
            if (!hasCenteredOnce) {
                isWaitingForFix = true
                gpsSignalState = LocationFixEvaluator.labelOf(decision.quality)
            }
            return false
        }

        applyFix(location, decision.quality, nowElapsed)
        return true
    }

    val startLocation = {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1500L)
            .setMinUpdateIntervalMillis(750L)
            .setMinUpdateDistanceMeters(0f)
            .setWaitForAccurateLocation(false)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {

                var accepted = 0

                result.locations.forEach { location ->
                    if (acceptFix(location, isCachedHint = false)) {
                        accepted++
                    }
                }

                if (accepted == 0 && !hasCenteredOnce) {
                    isWaitingForFix = true
                }
            }
        }

        try {
            fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
            activeCallback = callback

            isWaitingForFix = true

            // La ultima posicion que el sistema tiene guardada solo se usa
            // para arrancar. Si es mas vieja de 30 segundos se descarta:
            // puede ser de otro barrio, y por eso la app mostraba un
            // lugar donde el usuario nunca estuvo.
            fusedClient.lastLocation
                .addOnSuccessListener { last ->
                    if (last != null) {
                        acceptFix(last, isCachedHint = true)
                    }
                }

        } catch (_: SecurityException) {
        }
    }


    LaunchedEffect(destination) {
        if (destination != null) {
            hasMovedManually = false
            isFollowingUser = false
            isProgrammaticMove = true
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(destination, 17f),
                700
            )
            isProgrammaticMove = false
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCenteredOnce) {
            if (userLocation != null) {
                isProgrammaticMove = true
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(initialLocation, 16f),
                    600
                )
                isProgrammaticMove = false
            } else {
                isProgrammaticMove = true
                cameraPositionState.animate(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 2f)
                    ),
                    400
                )
                isProgrammaticMove = false
            }
        }
    }

    LaunchedEffect(cameraPositionState.isMoving) {
        if (cameraPositionState.isMoving && !isProgrammaticMove) {
            hasMovedManually = true
            isFollowingUser = false
        }
    }

    DisposableEffect(locationPermissionGranted, isLocationServiceOn) {
        if (!locationPermissionGranted || !isLocationServiceOn) {
            onDispose { }
        } else {
            enableLocationSettings(context) {
                updateLocationServiceState()
                startLocation()
            }
            onDispose {
                activeCallback?.let { callback ->
                    try {
                        fusedClient.removeLocationUpdates(callback)
                    } catch (_: Exception) {
                    }
                }
                activeCallback = null
            }
        }
    }

    fun centerOnUser() {
        val target = userLocation
        if (target == null) {
            requestLocationPermission()
            return
        }
        isFollowingUser = true
        hasMovedManually = false
        scope.launch {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(target, 17f),
                550
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = hasFinePermission,
                mapType = MapType.NORMAL,
                minZoomPreference = 12f,
                maxZoomPreference = 20f
            ),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = hasFinePermission,
                zoomControlsEnabled = false,
                compassEnabled = true,
                mapToolbarEnabled = false
            ),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            selectedRoute?.points?.takeIf { it.size >= 2 }?.let { points ->
                Polyline(
                    points = points.map { LatLng(it.latitude, it.longitude) },
                    color = SmartBusBlue,
                    width = 7f
                )
            }

            stops.forEach { stop ->
                Marker(
                    state = MarkerState(
                        position = LatLng(stop.latitude, stop.longitude)
                    ),
                    title = stop.name,
                    snippet = "Paradero disponible",
                    onClick = {
                        onStopClick(stop)
                        true
                    }
                )
            }

            buses.forEach { bus ->
                Marker(
                    state = MarkerState(
                        position = LatLng(bus.latitude, bus.longitude)
                    ),
                    title = vehicleTitle(bus),
                    snippet = vehicleSnippet(bus)
                )
            }

            destination?.let { target ->
                Marker(
                    state = MarkerState(position = target),
                    title = destinationName ?: "Destino",
                    snippet = "Destino seleccionado"
                )
            }

            // Posicion real del usuario. Google Maps ya dibuja su punto
            // azul nativo; este marcador la hace explicita y siempre
            // visible mientras llega el fix.
            userLocation?.let { here ->
                Marker(
                    state = MarkerState(position = here),
                    title = "Mi ubicacion",
                    snippet = if (userLocationAccuracy != Float.MAX_VALUE) {
                        "Precision de ${userLocationAccuracy.roundToInt()} m"
                    } else {
                        "Ubicacion en tiempo real"
                    }
                )
            }
        }

        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSearchClick() }
                    .padding(start = 16.dp, top = 14.dp, end = 6.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar destino",
                    tint = SmartBusBlue
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = destinationName ?: "A donde quieres ir?",
                        color = if (destinationName != null) SmartBusText else SmartBusTextLight,
                        fontWeight = if (destinationName != null) {
                            FontWeight.SemiBold
                        } else {
                            FontWeight.Normal
                        },
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (destinationName != null) {
                        Text(
                            text = "Toca para cambiar el destino",
                            color = SmartBusTextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }

                if (destination != null) {
                    IconButton(onClick = { onClearDestination() }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Limpiar destino",
                            tint = SmartBusTextSecondary
                        )
                    }
                }

                IconButton(onClick = { centerOnUser() }) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Centrar en mi ubicacion",
                        tint = if (locationPermissionGranted) SmartBusBlue else SmartBusTextLight
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 92.dp, end = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(if (isGpsAccurate) SmartBusGreen else SmartBusYellow)
                )
                Column {
                    Text(
                        text = gpsSignalState,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SmartBusText
                    )
                    if (userLocationAccuracy != Float.MAX_VALUE) {
                        Text(
                            text = "±${userLocationAccuracy.roundToInt()} m",
                            fontSize = 10.sp,
                            color = SmartBusTextSecondary
                        )
                    }
                }
            }
        }

        if (!locationPermissionGranted) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 28.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        tint = SmartBusBlue,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "Activa tu ubicacion",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SmartBusText
                    )
                    Text(
                        text = if (isPermanentlyDenied) {
                            "El permiso de ubicacion quedo bloqueado. Activalo en Ajustes para que SmartBus pueda mostrarte donde estas y los buses mas cercanos."
                        } else {
                            "SmartBus necesita tu ubicacion para mostrarte los buses mas cercanos y calcular tu ETA."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = SmartBusTextSecondary
                    )
                    androidx.compose.material3.Button(
                        onClick = {
                            if (isPermanentlyDenied) {
                                openAppSettings()
                            } else {
                                requestLocationPermission()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (isPermanentlyDenied) {
                                "Abrir ajustes"
                            } else {
                                "Permitir ubicacion"
                            }
                        )
                    }
                }
            }
        }

        // GPS del telefono apagado: con el permiso concedido pero sin
        // ubicacion del sistema, Android no entrega ninguna posicion.
        if (locationPermissionGranted && !isLocationServiceOn) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 28.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        tint = SmartBusBlue,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "GPS apagado",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SmartBusText
                    )
                    Text(
                        text = "La ubicacion del telefono esta desactivada. Enciendela para que SmartBus te localize de verdad.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SmartBusTextSecondary
                    )
                    androidx.compose.material3.Button(
                        onClick = {
                            enableLocationSettings(context) {
                                updateLocationServiceState()
                                startLocation()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Encender GPS")
                    }
                }
            }
        }

        if (locationPermissionGranted && !hasFinePermission) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 28.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Text(
                        text = "Ubicacion aproximada",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SmartBusText
                    )
                    Text(
                        text = "Tu ubicacion precisa esta desactivada, por eso el mapa puede ser inexacto.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SmartBusTextSecondary
                    )
                    androidx.compose.material3.Button(
                        onClick = { requestLocationPermission() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Activar ubicacion precisa")
                    }
                }
            }
        }

        if (locationPermissionGranted && hasFinePermission && isWaitingForFix) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 96.dp, start = 28.dp, end = 28.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Obteniendo tu ubicacion real",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SmartBusText
                    )
                    Text(
                        text = gpsSignalState,
                        style = MaterialTheme.typography.bodySmall,
                        color = SmartBusTextSecondary
                    )
                    Text(
                        text = "El mapa se movera a tu posicion en cuanto el GPS responda.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SmartBusTextLight
                    )
                }
            }
        }

        if (showPermissionRationale) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showPermissionRationale = false },
                title = { Text("Permiso de ubicacion") },
                text = {
                    Text(
                        "SmartBus usa tu ubicacion en tiempo real para " +
                            "mostrarte el bus mas cercano, tu ETA y validar " +
                            "los abordajes. Sin este permiso el mapa no " +
                            "puede mostrar tu posicion."
                    )
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            showPermissionRationale = false
                            requestLocationPermission()
                        }
                    ) {
                        Text("Permitir")
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(
                        onClick = { showPermissionRationale = false }
                    ) {
                        Text("Ahora no")
                    }
                }
            )
        }

        val stop = nearestStop
        val distance = nearestDistance
        if (stop != null && distance != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 104.dp, end = 76.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(SmartBusGreen)
                    )
                    Column {
                        Text(
                            text = stop.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SmartBusText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${distance.roundToInt()} m · Paradero mas cercano",
                            fontSize = 11.sp,
                            color = SmartBusTextSecondary
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { centerOnUser() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 96.dp),
            containerColor = Color.White,
            contentColor = SmartBusBlue,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Centrar en mi ubicacion"
            )
        }
    }
}

/**
 * Titulo del marcador de un vehiculo.
 *
 * Si el vehiculo tiene conductor verificado se muestra su nombre y
 * los ultimos 4 de la placa, que es como lo identifica el pasajero.
 * Si viene del simulador se dice explicitamente: una demostracion no
 * debe parecer un auto real en servicio.
 */
private fun vehicleTitle(bus: Bus): String {

    val plate = if (bus.plateCount > 0) {
        " · placa ${bus.plateCount}"
    } else {
        ""
    }

    return when {
        bus.isMock -> "Bus ${bus.code} (demo)"
        bus.driverName.isNotBlank() ->
            "${bus.driverName.substringBefore(' ')} · ${bus.code}$plate"

        else -> "Bus ${bus.code}$plate"
    }
}

private fun vehicleSnippet(bus: Bus): String {

    val speed = "${bus.speedKmh.roundToInt()} km/h"

    return when {
        bus.isMock ->
            "$speed · posicion simulada, no hay conductor real"

        bus.capacity > 0 ->
            "$speed · ${bus.capacity} lugares · ${bus.status}"

        else ->
            "$speed · ${bus.status}"
    }
}

/**
 * Obtiene la Activity actual para usar
 * shouldShowRequestPermissionRationale.
 */
internal fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) {
            return current
        }
        current = current.baseContext
    }
    return null
}
