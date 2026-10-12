
package com.ejemplo.smartbus.navigation

import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.ejemplo.smartbus.data.BusStopRepository
import com.ejemplo.smartbus.data.RoomTransitRepository
import com.ejemplo.smartbus.data.SessionManager
import com.ejemplo.smartbus.data.TrackingEventManager
import com.ejemplo.smartbus.data.VehiclePositionSync
import com.ejemplo.smartbus.data.findRoutesForDestination
import com.ejemplo.smartbus.data.model.Bus
import com.ejemplo.smartbus.data.model.BusRoute
import com.ejemplo.smartbus.data.model.RouteCandidate
import com.ejemplo.smartbus.data.model.BusStop
import com.ejemplo.smartbus.data.SmartBusSession
import com.ejemplo.smartbus.data.driver.DriverRepository
import com.ejemplo.smartbus.data.model.UserRole
import com.ejemplo.smartbus.location.DriverLocationService
import com.ejemplo.smartbus.location.TrackingManager
import com.ejemplo.smartbus.data.PointsEventManager
import com.ejemplo.smartbus.data.PointsRepository
import com.ejemplo.smartbus.data.SettingsRepository
import com.ejemplo.smartbus.data.RewardsCatalog
import com.ejemplo.smartbus.data.StatisticsRepository
import com.ejemplo.smartbus.ui.components.GoogleSignInResult
import com.ejemplo.smartbus.ui.components.RouteCandidatesPanel
import com.ejemplo.smartbus.ui.components.SmartBusBottomBar
import com.ejemplo.smartbus.ui.components.SmartBusTab
import com.ejemplo.smartbus.ui.screens.DestinationSearchScreen
import com.ejemplo.smartbus.ui.screens.SignInScreen
import com.ejemplo.smartbus.ui.screens.AchievementsScreen
import com.ejemplo.smartbus.ui.screens.HelpScreen
import com.ejemplo.smartbus.ui.screens.HistoryScreen
import com.ejemplo.smartbus.ui.screens.HomeScreen
import com.ejemplo.smartbus.ui.screens.ProfileScreen
import com.ejemplo.smartbus.ui.screens.RedeemedRewardsScreen
import com.ejemplo.smartbus.ui.screens.RewardsScreen
import com.ejemplo.smartbus.ui.screens.SettingsScreen
import com.ejemplo.smartbus.ui.screens.SmartBusMapScreen
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text

@Composable
fun SmartBusNavigation() {
    val context = LocalContext.current
    val pointsRepository = remember { PointsRepository(context) }
    val settingsRepository = remember { SettingsRepository(context) }
    val sessionManager = remember { SessionManager.get(context) }
    val transitRepository = remember { RoomTransitRepository.get(context) }
    val driverRepository = remember { DriverRepository(context) }
    val scope = rememberCoroutineScope()

    val session by sessionManager.session.collectAsState()

    /**
     * El rol decide que mitad de la app se monta. Es lo primero que se
     * mira porque el conductor y el pasajero no comparten interfaz: el
     * conductor solo ve su mapa, el pasajero ve las pestanas de siempre.
     */
    val role = session?.role ?: UserRole.PASSENGER

    /**
     * Si alguien toca "Modo conductor" sin cuenta, primero se le pide
     * entrar y despues se le cambia el rol, para que su registro quede
     * ligado a una cuenta y no a un invitado anonimo.
     */
    var pendingDriverMode by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        transitRepository.seedIfEmpty()
    }

    LaunchedEffect(session?.storageKey, pendingDriverMode) {
        if (pendingDriverMode && session != null) {
            sessionManager.setRole(UserRole.DRIVER)
            pendingDriverMode = false
        }
    }

    if (role == UserRole.DRIVER) {
        DriverFlow(
            accountId = session?.storageKey.orEmpty(),
            onSwitchToPassenger = {
                sessionManager.setRole(UserRole.PASSENGER)
            },
            onSignedOut = {
                DriverLocationService.stop(context)
                sessionManager.signOut()
            }
        )
        return
    }

    var currentTab by remember { mutableStateOf(SmartBusTab.HOME) }
    var overlay by remember { mutableStateOf<ProfileOverlay?>(null) }
    var destination by remember { mutableStateOf<LatLng?>(null) }
    var destinationName by remember { mutableStateOf<String?>(null) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var selectedRoute by remember { mutableStateOf<BusRoute?>(null) }
    var selectedStop by remember { mutableStateOf<BusStop?>(null) }
    var routeCandidates by remember { mutableStateOf<List<RouteCandidate>>(emptyList()) }
    var showRoutePanel by remember { mutableStateOf(false) }

    // Que haz todo el resto: la pantalla de los dos circulos. Se
    // muestra al entrar, y de aqui se sale hacia pasajero o hacia la
    // puerta de registro de conductor.
    

    // Datos de transporte desde la base de datos, no desde listas fijas.
    val stops by transitRepository.observeStops()
        .collectAsState(initial = BusStopRepository.stops)

    val routes by transitRepository.observeRoutes()
        .collectAsState(initial = emptyList())

    val vehicles by transitRepository.observeVehicles()
        .collectAsState(initial = emptyList())
    val trackingManager = remember {
        TrackingManager(context)
    }
    var points by remember { mutableIntStateOf(pointsRepository.getPoints()) }
    val trackingUiState by TrackingEventManager.trackingState.collectAsState()
    val trackingActive = trackingUiState.trackingActive
    var trackingStartedManually by remember { mutableStateOf(false) }

    var redeemedCode by remember { mutableStateOf<String?>(null) }
    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val fineGranted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

            val coarseGranted =
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (fineGranted || coarseGranted) {
                trackingStartedManually = true
                TrackingManager(context).startTracking()
            } else {
                trackingStartedManually = false

                Toast.makeText(
                    context,
                    "Debes permitir la ubicación para usar SmartBus",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    var redeemedRewardIds by remember {
        mutableStateOf(pointsRepository.getRedeemedRewardIds())
    }

    // El pasajero tambien necesita su ubicacion real: apenas entra, si
    // todavia no la tiene se le pide; si ya la concedio, no se le
    // vuelve a molestar y sigue activa.
    LaunchedEffect(Unit) {

        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val notificationsPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }

    // En Android 13+ las notificaciones (puntos, llegada del bus y el
    // aviso de iniciar sesion para guardar los puntos) solo se ven con
    // este permiso. Si ya esta concedido no se vuelve a pedir.
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationsPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }

    // ============================================================
    // MODO PASAJERO (el que ya existia).
    // ============================================================

    // ------------------------------------------------------------
// PUERTA DE CONDUCTOR: al activar desde el menu lateral
// ------------------------------------------------------------

    LaunchedEffect(Unit) {
        VehiclePositionSync.start(context, transitRepository, this)
    }

    // Cada cuenta tiene su propio saldo de puntos.
    LaunchedEffect(session?.storageKey) {

        pointsRepository.useAccount(session?.storageKey)
        points = pointsRepository.getPoints()
        redeemedRewardIds = pointsRepository.getRedeemedRewardIds()
    }

    // Al elegir destino, se busca que rutas sirven ahi.
    LaunchedEffect(destination, stops, routes, vehicles) {

        val target = destination

        if (target == null) {
            routeCandidates = emptyList()
            showRoutePanel = false
            return@LaunchedEffect
        }

        routeCandidates = findRoutesForDestination(
            destinationLat = target.latitude,
            destinationLng = target.longitude,
            stops = stops,
            routes = routes,
            buses = vehicles
        )

        showRoutePanel = true
    }

    LaunchedEffect(currentTab) {
        points = pointsRepository.getPoints()
        redeemedRewardIds = pointsRepository.getRedeemedRewardIds()
    }

    LaunchedEffect(Unit) {
        PointsEventManager.pointsEvents.collect {
            points = pointsRepository.getPoints()
        }
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                LocationServices.getFusedLocationProviderClient(context)
                    .lastLocation
                    .addOnSuccessListener { location ->
                        location?.let {
                            userLocation = LatLng(it.latitude, it.longitude)
                        }
                    }
            } catch (_: SecurityException) {
            }
        }
    }

    /**
     * Pantallas secundarias (ajustes, ayuda, premios, inicio de sesion).
     * Ocupan la pantalla completa y tapan las pestanas mientras estan
     * abiertas, que es lo que se espera de ellas.
     */
    val activeOverlay = overlay
    if (activeOverlay != null) {
        OverlayHost(
            overlay = activeOverlay,
            settingsRepository = settingsRepository,
            pointsRepository = pointsRepository,
            sessionManager = sessionManager,
            points = points,
            onBack = { overlay = null },
            onSignedIn = {
                overlay = null
                if (pendingDriverMode) {
                    sessionManager.setRole(UserRole.DRIVER)
                    pendingDriverMode = false
                }
            },
            onHelpClick = { overlay = ProfileOverlay.HELP },
            onSettingsClick = { overlay = ProfileOverlay.SETTINGS },
            onClearPoints = {
                pointsRepository.clearPoints()
                points = pointsRepository.getPoints()
            }
        )
        return
    }

    when (currentTab) {
        SmartBusTab.HOME -> HomeScreen(
            points = points,
            trackingActive = trackingActive,
            onTrackingClick = {

                val fineGranted =
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                val coarseGranted =
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                if (!fineGranted && !coarseGranted) {

                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )

                } else {

                    if (trackingStartedManually) {
                        TrackingManager(context).stopTracking()
                        trackingStartedManually = false
                    } else {
                        TrackingManager(context).startTracking()
                        trackingStartedManually = true
                    }
                }
            },
            onMapClick = { currentTab = SmartBusTab.MAP },
            onRewardsClick = { currentTab = SmartBusTab.REWARDS },
            onHistoryClick = { currentTab = SmartBusTab.RANKING },
            onProfileClick = { currentTab = SmartBusTab.PROFILE },
            onSearchClick = { currentTab = SmartBusTab.MAP },
            onReportsClick = { currentTab = SmartBusTab.PROFILE },
            onNotificationsClick = { currentTab = SmartBusTab.RANKING },
            onSettingsClick = { overlay = ProfileOverlay.SETTINGS }
        )

        SmartBusTab.MAP -> MapTab(
            destination = destination,
            destinationName = destinationName,
            stops = stops,
            routes = routes,
            vehicles = vehicles,
            selectedRoute = selectedRoute,
            selectedStop = selectedStop,
            routeCandidates = routeCandidates,
            showRoutePanel = showRoutePanel,
            onRouteSelected = { candidate ->
                selectedRoute = candidate.route
                selectedStop = candidate.boardingStop
            },
            onStopSelected = { stop ->
                selectedStop = stop
                selectedRoute = routes.firstOrNull {
                    stop.id in it.stopIds
                }
            },
            onCloseRoutePanel = { showRoutePanel = false },
            onDestinationSelected = { latLng, name ->
                destination = latLng
                destinationName = name
                selectedRoute = null
            },
            onClearDestination = {
                destination = null
                destinationName = null
                selectedRoute = null
                selectedStop = null
            },
            userLocation = userLocation,
            onUserLocationChange = { latLng ->
                userLocation = latLng
            },
            currentTab = currentTab,
            onTabChange = { currentTab = it }
        )

        SmartBusTab.REWARDS -> RewardsTab(
            points = points,
            pointsRepository = pointsRepository,
            redeemedCode = redeemedCode,
            redeemedRewardIds = redeemedRewardIds,
            onRedeemed = { code ->
                redeemedCode = code
                redeemedRewardIds = pointsRepository.getRedeemedRewardIds()
                points = pointsRepository.getPoints()
            },
            currentTab = currentTab,
            onTabChange = { currentTab = it }
        )

        SmartBusTab.RANKING -> HistoryTab(
            pointsRepository = pointsRepository,
            currentTab = currentTab,
            onTabChange = { currentTab = it },
            onSettingsClick = { overlay = ProfileOverlay.SETTINGS },
            onHelpClick = { overlay = ProfileOverlay.HELP }
        )

        SmartBusTab.PROFILE -> ProfileTab(
            points = points,
            pointsRepository = pointsRepository,
            session = session,
            onSignInClick = { overlay = ProfileOverlay.SIGN_IN },
            onSignOutClick = {
                DriverLocationService.stop(context)
                sessionManager.signOut()
                pointsRepository.useAccount(null)
                points = pointsRepository.getPoints()
            },
            onBecomeDriverClick = {
                if (session == null) {
                    pendingDriverMode = true
                    overlay = ProfileOverlay.SIGN_IN
                } else {
                    sessionManager.setRole(UserRole.DRIVER)
                }
            },
            onSettingsClick = { overlay = ProfileOverlay.SETTINGS },
            onHelpClick = { overlay = ProfileOverlay.HELP },
            onHistoryClick = { currentTab = SmartBusTab.RANKING },
            onRedeemedClick = { overlay = ProfileOverlay.REDEEMED },
            onAchievementsClick = { overlay = ProfileOverlay.ACHIEVEMENTS },
            currentTab = currentTab,
            onTabChange = { currentTab = it }
        )
    }
}

/**
 * Pantallas secundarias abiertas desde Perfil, Inicio o el historial.
 */
private enum class ProfileOverlay {
    SETTINGS,
    HELP,
    REDEEMED,
    ACHIEVEMENTS,
    SIGN_IN
}

@Composable
private fun OverlayHost(
    overlay: ProfileOverlay,
    settingsRepository: SettingsRepository,
    pointsRepository: PointsRepository,
    sessionManager: SessionManager,
    points: Int,
    onBack: () -> Unit,
    onSignedIn: () -> Unit,
    onHelpClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onClearPoints: () -> Unit
) {

    val context = LocalContext.current

    val statistics = remember(pointsRepository, points) {
        StatisticsRepository(pointsRepository).getStatistics()
    }

    when (overlay) {

        ProfileOverlay.SETTINGS -> SettingsScreen(
            settings = settingsRepository,
            onBack = onBack,
            onHelpClick = onHelpClick,
            onClearPoints = onClearPoints
        )

        ProfileOverlay.HELP -> HelpScreen(
            onBack = onBack,
            onSettingsClick = onSettingsClick
        )

        ProfileOverlay.REDEEMED -> RedeemedRewardsScreen(
            redeemed = pointsRepository.getRedeemedRewards(),
            onBack = onBack
        )

        ProfileOverlay.ACHIEVEMENTS -> AchievementsScreen(
            points = points,
            boardingCount = statistics.boardingCount,
            rewardsRedeemed = statistics.rewardsRedeemed,
            onBack = onBack
        )

        ProfileOverlay.SIGN_IN -> SignInScreen(
            onBack = onBack,
            onGoogleResult = { result ->
                when (result) {

                    is GoogleSignInResult.Success -> {
                        sessionManager.signInWithGoogle(
                            userId = result.userId,
                            name = result.name,
                            email = result.email,
                            photoUri = result.photoUri,
                            idToken = result.idToken
                        )
                        onSignedIn()
                    }

                    is GoogleSignInResult.Error ->
                        Toast.makeText(
                            context,
                            result.message,
                            Toast.LENGTH_LONG
                        ).show()

                    GoogleSignInResult.Cancelled -> Unit
                }
            },
            onSignInWithEmail = { email, name ->
                sessionManager.signInWithEmail(email, name)
                onSignedIn()
            },
            onContinueAsGuest = { name ->
                sessionManager.signInAsGuest("Invitado")
                onSignedIn()
            }
        )
    }
}

@Composable
private fun MapTab(
    destination: LatLng?,
    destinationName: String?,
    stops: List<BusStop>,
    routes: List<BusRoute>,
    vehicles: List<Bus>,
    selectedRoute: BusRoute?,
    selectedStop: BusStop?,
    routeCandidates: List<RouteCandidate>,
    showRoutePanel: Boolean,
    onRouteSelected: (RouteCandidate) -> Unit,
    onStopSelected: (BusStop) -> Unit,
    onCloseRoutePanel: () -> Unit,
    onDestinationSelected: (LatLng, String) -> Unit,
    onClearDestination: () -> Unit,
    userLocation: LatLng?,
    onUserLocationChange: (LatLng) -> Unit,
    currentTab: SmartBusTab,
    onTabChange: (SmartBusTab) -> Unit
) {
    var showSearch by remember { mutableStateOf(false) }

    if (showSearch) {
        DestinationSearchScreen(
            onBack = { showSearch = false },
            currentLocation = userLocation,
            onLocationSelected = { latLng, name ->
                onDestinationSelected(latLng, name)
                showSearch = false
            }
        )
        return
    }

    Scaffold(
        bottomBar = {
            SmartBusBottomBar(
                selectedTab = currentTab,
                onTabSelected = onTabChange
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            SmartBusMapScreen(
                stops = stops,
                routes = routes,
                vehicles = vehicles,
                selectedRoute = selectedRoute,
                selectedStop = selectedStop,
                destination = destination,
                destinationName = destinationName,
                onSearchClick = { showSearch = true },
                onClearDestination = onClearDestination,
                onStopClick = onStopSelected,
                onUserLocationChange = onUserLocationChange
            )

            if (showRoutePanel) {

                RouteCandidatesPanel(
                    candidates = routeCandidates,
                    selectedRouteId = selectedRoute?.id,
                    destinationName = destinationName,
                    onSelectRoute = onRouteSelected,
                    onClose = onCloseRoutePanel,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun RewardsTab(
    points: Int,
    pointsRepository: PointsRepository,
    redeemedCode: String?,
    redeemedRewardIds: Set<Int>,
    onRedeemed: (String) -> Unit,
    currentTab: SmartBusTab,
    onTabChange: (SmartBusTab) -> Unit
) {
    RewardsScreen(
        rewards = RewardsCatalog.rewards,
        points = points,
        onRedeem = { reward ->
            val code = pointsRepository.redeemRewardOnce(
                rewardId = reward.id,
                rewardName = reward.name,
                points = reward.points
            )
            if (code != null) {
                onRedeemed(code)
            }
        },
        onBack = { onTabChange(SmartBusTab.HOME) },
        redeemedCode = redeemedCode,
        redeemedRewardIds = redeemedRewardIds,
        onTabSelected = onTabChange
    )
}

@Composable
private fun HistoryTab(
    pointsRepository: PointsRepository,
    currentTab: SmartBusTab,
    onTabChange: (SmartBusTab) -> Unit,
    onSettingsClick: () -> Unit,
    onHelpClick: () -> Unit
) {
    HistoryScreen(
        history = pointsRepository.getHistory(),
        currentTab = currentTab,
        onTabChange = onTabChange,
        onBack = { onTabChange(SmartBusTab.HOME) },
        onSettingsClick = onSettingsClick,
        onHelpClick = onHelpClick
    )
}

@Composable
private fun ProfileTab(
    points: Int,
    pointsRepository: PointsRepository,
    session: SmartBusSession?,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onBecomeDriverClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onHelpClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onRedeemedClick: () -> Unit,
    onAchievementsClick: () -> Unit,
    currentTab: SmartBusTab,
    onTabChange: (SmartBusTab) -> Unit
) {
    ProfileScreen(
        points = points,
        session = session,
        onSignInClick = onSignInClick,
        onSignOutClick = onSignOutClick,
        onBecomeDriverClick = onBecomeDriverClick,
        statistics = remember(pointsRepository, points) {
            StatisticsRepository(pointsRepository).getStatistics()
        },
        onBack = { onTabChange(SmartBusTab.HOME) },
        onSettingsClick = onSettingsClick,
        onHelpClick = onHelpClick,
        onHistoryClick = onHistoryClick,
        onRedeemedClick = onRedeemedClick,
        onAchievementsClick = onAchievementsClick,
        onTabSelected = onTabChange
    )
}
