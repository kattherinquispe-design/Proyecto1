package com.ejemplo.smartbus.navigation

import android.Manifest
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ejemplo.smartbus.BuildConfig
import com.ejemplo.smartbus.data.driver.DriverDocumentStorage
import com.ejemplo.smartbus.data.driver.DriverGateResult
import com.ejemplo.smartbus.data.driver.DriverRepository
import com.ejemplo.smartbus.data.driver.DriverReviewSimulator
import com.ejemplo.smartbus.data.driver.DriverValidators
import com.ejemplo.smartbus.data.RoomTransitRepository
import com.ejemplo.smartbus.data.model.DriverDocumentType
import com.ejemplo.smartbus.location.DriverLocationService
import com.ejemplo.smartbus.ui.components.rememberCameraCapture
import com.ejemplo.smartbus.ui.screens.DriverDocumentsScreen
import com.ejemplo.smartbus.ui.screens.DriverEarningsScreen
import com.ejemplo.smartbus.ui.screens.DriverHomeScreen
import com.ejemplo.smartbus.ui.screens.DriverOnboardingScreen
import com.ejemplo.smartbus.ui.screens.DriverVehicleScreen
import com.ejemplo.smartbus.ui.screens.VehicleUnit
import kotlinx.coroutines.launch

/**
 * Pantallas del conductor.
 *
 * El rol vive en la sesion, no aqui: cuando el pasajero toca
 * "Trabajar como conductor" la sesion cambia y esta funcion aparece
 * en lugar de las pestañas de pasajero. Al volver, todo lo del
 * pasajero sigue igual, incluyendo los puntos.
 */
private enum class DriverScreen {
    HOME,
    PROFILE,
    DOCUMENTS,
    VEHICLE,
    EARNINGS
}

@Composable
fun DriverFlow(
    accountId: String,
    onSwitchToPassenger: () -> Unit,
    onSignedOut: () -> Unit
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val repository = remember { DriverRepository(context) }

    val transitRepository = remember {
        RoomTransitRepository.get(context)
    }

    val vehiclesSnapshot by remember {
        transitRepository.observeVehicles()
    }.collectAsState(initial = emptyList())

    val routesSnapshot by remember {
        transitRepository.observeRoutes()
    }.collectAsState(initial = emptyList())

    /**
     * Unidades de transporte disponibles. El conductor se asigna a
     * una de ellas: en una flota real lo hce el operador, aqui se
     * elige a mano porque no hay backend todavia.
     */
    val transitUnits = remember(vehiclesSnapshot, routesSnapshot) {
        vehiclesSnapshot.map { vehicle ->
            VehicleUnit(
                id = vehicle.id,
                code = vehicle.code,
                routeName = routesSnapshot
                    .firstOrNull { it.id == vehicle.routeId }
                    ?.let { "${it.code} - ${it.name}" }
                    ?: "Sin ruta"
            )
        }
    }

    var screen by remember { mutableStateOf(DriverScreen.HOME) }

    val profile by remember(accountId) {
        repository.observeProfile(accountId)
    }.collectAsState(initial = null)

    /**
     * Un conductor que todavia no ha llenado sus datos no pinta nada
     * en el mapa: primero se registra, y despues ve su ruta.
     */
    LaunchedEffect(profile) {
        if (screen == DriverScreen.HOME && profile == null) {
            screen = DriverScreen.PROFILE
        }
    }

    val vehicles by remember(accountId) {
        repository.observeVehicles(accountId)
    }.collectAsState(initial = emptyList())

    val documents by remember(accountId) {
        repository.observeDocuments(accountId)
    }.collectAsState(initial = emptyList())

    val shift by remember(accountId) {
        repository.observeShift(accountId)
    }.collectAsState(initial = null)

    val trips by remember(accountId) {
        repository.observeTrips(accountId)
    }.collectAsState(initial = emptyList())

    val approvedTypes = remember(documents) {
        documents.filter { it.satisfied }.map { it.type }.toSet()
    }

    val primaryVehicle = remember(vehicles) {
        vehicles.firstOrNull { it.isPrimary } ?: vehicles.firstOrNull()
    }

    /**
     * Ruta asignada al conductor: es la ruta de la unidad de transito
     * que el conductor eligio como suya. Es lo que ve en el mapa.
     */
    val assignedRoute = remember(primaryVehicle, vehiclesSnapshot, routesSnapshot) {
        val transitId = primaryVehicle?.transitVehicleId
            ?: return@remember null

        val unit = vehiclesSnapshot.firstOrNull { it.id == transitId }
            ?: return@remember null

        routesSnapshot.firstOrNull { it.id == unit.routeId }
    }

    val isWorking = shift?.isOnline == true

    val blockers = remember(profile, primaryVehicle, approvedTypes) {
        DriverValidators.blockReasons(profile, primaryVehicle, approvedTypes)
    }

    // ============================================================
    // PERMISOS Y PUESTA EN SERVICIO
    // ============================================================

    /**
     * El conductor necesita ubicacion real para que el pasajero lo vea.
     * Si ya la tiene concedida no se le vuelve a preguntar.
     */
    fun hasLocationPermission(): Boolean {

        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }

    /**
     * Revisa las reglas y, si todo esta en orden, arranca el GPS que
     * comparte la posicion con el pasajero.
     */
    fun goOnlineNow() {
        scope.launch {
            when (val result = repository.goOnline(accountId)) {

                is DriverGateResult.Blocked ->
                    snackbar.showSnackbar(result.blockers.first().title)

                DriverGateResult.Allowed -> {
                    DriverLocationService.start(context, accountId)
                    DriverHomeScreenMessages.online(context)
                }
            }
        }
    }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Se conceda o no, se arranca: el servicio es de primer plano,
        // asi que la ubicacion funciona igual con la pantalla encendida.
        goOnlineNow()
    }

    val driverLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->

        val granted =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (!granted) {
            Toast.makeText(
                context,
                "Sin ubicacion no podemos mostrar tu vehiculo al pasajero",
                Toast.LENGTH_LONG
            ).show()
            return@rememberLauncherForActivityResult
        }

        // Android 10+ pide aparte la ubicacion en segundo plano, para
        // seguir compartiendo con la pantalla apagada.
        val needsBackground =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED

        if (needsBackground) {
            backgroundLocationLauncher.launch(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        } else {
            goOnlineNow()
        }
    }

    /**
     * Pide ubicacion (y notificaciones para el aviso del servicio) y,
     * al concederlas, deja al conductor en servicio.
     */
    fun requestLocationThenGoOnline() {

        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }

        driverLocationLauncher.launch(permissions.toTypedArray())
    }

    /**
     * Al salir de la pantalla principal, si el conductor estaba
     * trabajando, el GPS sigue: el servicio esta pensado para
     * sobrevivir al cierre de la app.
     */
    fun leaveScreen() {
        screen = DriverScreen.HOME
    }

    /**
     * Vuelve al modo pasajero cerrando el servicio de GPS para que el
     * pasajero deje de ver el vehiculo. El service termina de cerrar el
     * turno en segundo plano.
     */
    fun returnToPassenger() {
        DriverLocationService.stop(context)
        onSwitchToPassenger()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            when (screen) {

                DriverScreen.HOME -> DriverHomeScreen(
                    vehicle = primaryVehicle,
                    shift = shift,
                    blockers = blockers,
                    isWorking = isWorking,
                    route = assignedRoute,
                    onToggleOnline = {

                        if (isWorking) {

                            scope.launch {
                                repository.goOffline(accountId)
                                DriverLocationService.stop(context)
                                DriverHomeScreenMessages.offline(
                                    context
                                )
                            }

                        } else {

                            if (!hasLocationPermission()) {
                                requestLocationThenGoOnline()
                            } else {
                                goOnlineNow()
                            }
                        }
                    },
                    onOpenProfile = { screen = DriverScreen.PROFILE },
                    onOpenDocuments = { screen = DriverScreen.DOCUMENTS },
                    onOpenVehicle = { screen = DriverScreen.VEHICLE },
                    onOpenEarnings = { screen = DriverScreen.EARNINGS },
                    onSwitchToPassenger = { returnToPassenger() }
                )

                DriverScreen.PROFILE -> DriverOnboardingScreen(
                    accountId = accountId,
                    repository = repository,
                    existing = profile,
                    onBack = { leaveScreen() },
                    onSaved = {
                        screen = DriverScreen.VEHICLE
                    }
                )

                DriverScreen.VEHICLE -> DriverVehicleScreen(
                    accountId = accountId,
                    repository = repository,
                    existing = primaryVehicle,
                    transitUnits = transitUnits,
                    onBack = { leaveScreen() },
                    onSaved = {
                        screen = DriverScreen.DOCUMENTS
                        scope.launch {
                            snackbar.showSnackbar(
                                "Vehiculo guardado. Ahora sube tus documentos."
                            )
                        }
                    }
                )

                DriverScreen.DOCUMENTS -> DocumentsStep(
                    accountId = accountId,
                    repository = repository,
                    documents = documents,
                    approvedTypes = approvedTypes,
                    onBack = { leaveScreen() },
                    onFinish = {
                        screen = DriverScreen.HOME
                        scope.launch {
                            snackbar.showSnackbar(
                                "Documentos enviados. Te avisamos cuando " +
                                    "sean revisados."
                            )
                        }
                    }
                )

                DriverScreen.EARNINGS -> DriverEarningsScreen(
                    trips = trips,
                    onBack = { leaveScreen() }
                )
            }
        }
    }
}

/**
 * Paso de documentos con el selector de archivos de verdad.
 *
 * El archivo se copia al almacenamiento interno y de ahi se guarda
 * solo la ruta: si se guardara la Uri, al reinstalar o al limpiar la
 * galeria el documento dejaria de existir.
 */
@Composable
private fun DocumentsStep(
    accountId: String,
    repository: DriverRepository,
    documents: List<com.ejemplo.smartbus.data.model.DriverDocument>,
    approvedTypes: Set<DriverDocumentType>,
    onBack: () -> Unit,
    onFinish: () -> Unit
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pendingType by remember { mutableStateOf<DriverDocumentType?>(null) }
    var chooseSourceFor by remember { mutableStateOf<DriverDocumentType?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun onFileReady(path: String?) {

        val type = pendingType
        pendingType = null

        if (path == null || type == null) {
            return
        }

        busy = true

        scope.launch {

            if (path.isHttpOrFileUri()) {

                // Viene de la galeria: copiar al almacenamiento interno
                // antes de guardar la ruta.
                val copied = DriverDocumentStorage.copyIntoAppStorage(
                    context = context,
                    accountId = accountId,
                    type = type,
                    source = Uri.parse(path)
                )

                if (copied == null) {
                    Toast.makeText(
                        context,
                        "No se pudo leer el archivo. Intenta con una foto.",
                        Toast.LENGTH_LONG
                    ).show()
                    busy = false
                    return@launch
                }

                repository.uploadDocument(
                    accountId = accountId,
                    type = type,
                    filePath = copied
                )

            } else {

                // Viene de la camara: ya es un archivo del telefono.
                repository.uploadDocument(
                    accountId = accountId,
                    type = type,
                    filePath = path
                )
            }

            busy = false
        }
    }

    // Galeria: te devuelve una Uri de forma `content://`.
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val type = pendingType
        if (uri == null || type == null) {
            pendingType = null
            return@rememberLauncherForActivityResult
        }
        onFileReady(uri.toString())
    }

    // Camara: te devuelve ya la ruta absoluta de la foto.
    val capture = rememberCameraCapture { path ->
        onFileReady(path)
    }

    Column(modifier = Modifier.fillMaxSize()) {

        DriverDocumentsScreen(
            documents = documents,
            approvedTypes = approvedTypes,
            onUpload = { type ->
                chooseSourceFor = type
            },
            onDelete = { type ->
                scope.launch {
                    DriverDocumentStorage.delete(context, accountId, type)
                    repository.deleteDocument(accountId, type)
                }
            },
            onBack = onBack
        )

        // Al tocar "Subir", se pregunta de donde: foto con la camara
        // o un archivo de la galeria. Para un DNI lo natural es la
        // camara; para un documento ya escaneado, la galeria.
        chooseSourceFor?.let { type ->

            AlertDialog(
                onDismissRequest = { chooseSourceFor = null },
                title = { Text("Agregar ${type.title.lowercase()}") },
                text = {
                    Column {
                        Text(
                            text = "La foto debe verse completa y legible.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = type.explanation,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {
                    Row {
                        TextButton(
                            onClick = {
                                pendingType = type
                                chooseSourceFor = null
                                capture()
                            }
                        ) {
                            Text("Camara")
                        }
                        TextButton(
                            onClick = {
                                pendingType = type
                                chooseSourceFor = null
                                picker.launch("image/*")
                            }
                        ) {
                            Text("Galeria")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { chooseSourceFor = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        if (busy) {

            Text(
                text = "Guardando documento...",
                modifier = Modifier.padding(16.dp)
            )
        }

        // En release esto no existe: la aprobacion la hace un operador
        // desde el backend, nunca el propio conductor.
        if (BuildConfig.DEBUG) {

            DebugReviewBar(
                onApprove = {
                    scope.launch {
                        DriverReviewSimulator(repository)
                            .approveAllPending(accountId)
                    }
                }
            )
        }
    }
}

/**
 * Boton de revision simulada. Solo en debug.
 */
@Composable
private fun DebugReviewBar(onApprove: () -> Unit) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        TextButton(onClick = onApprove) {
            Text("[debug] Aprobar documentos pendientes")
        }
    }
}

private fun String.isHttpOrFileUri(): Boolean =
    startsWith("content://") || startsWith("file://")

private object DriverHomeScreenMessages {

    fun online(context: android.content.Context) {
        Toast.makeText(
            context,
            "Estas en servicio. Tu ubicacion es visible para los pasajeros.",
            Toast.LENGTH_LONG
        ).show()
    }

    fun offline(context: android.content.Context) {
        Toast.makeText(
            context,
            "Terminaste el servicio.",
            Toast.LENGTH_SHORT
        ).show()
    }
}