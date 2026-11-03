package com.ejemplo.smartbus.location

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ejemplo.smartbus.MainActivity
import com.ejemplo.smartbus.data.BoardingRewardManager
import com.ejemplo.smartbus.data.BusStopRepository
import com.ejemplo.smartbus.data.PointsEventManager
import com.ejemplo.smartbus.data.PointsRepository
import com.ejemplo.smartbus.data.SessionManager
import com.ejemplo.smartbus.data.SmartBusNotificationManager
import com.ejemplo.smartbus.data.TransitRepository
import com.ejemplo.smartbus.data.TrackingEventManager
import com.ejemplo.smartbus.data.RoomTransitRepository
import com.ejemplo.smartbus.data.model.Bus
import com.ejemplo.smartbus.data.model.BusStop
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch


class LocationTrackingService : Service() {

    // =========================================================
    // GPS
    // =========================================================

    private lateinit var fusedLocationClient:
            FusedLocationProviderClient

    private lateinit var locationCallback:
            LocationCallback


    // =========================================================
    // DETECTORES
    //
    // Dos caminos para el mismo abordaje:
    //  - El principal comprueba que el pasajero y el conductor
    //    viajaron juntos en el mismo vehiculo.
    //  - El de respaldo valida por paradero + velocidad, por si
    //    el feed del conductor todavia no esta disponible.
    // =========================================================

    private val boardingDetector =
        BusBoardingDetector()

    private val tripCoPresenceDetector =
        TripCoPresenceDetector()


    // =========================================================
    // REPOSITORIOS
    // =========================================================

    private lateinit var pointsRepository:
        PointsRepository

    private lateinit var rewardManager:
        BoardingRewardManager

    private lateinit var smartBusNotificationManager:
        SmartBusNotificationManager

    private lateinit var transitRepository:
        TransitRepository


    // =========================================================
    // BUSES REALES (ROOM)
    //
    // El simulador no sirve para confirmar un abordaje: aqui se
    // lee la tabla de vehiculos, donde el conductor espejea su
    // GPS real. Sin esto el pasajero compararia contra datos de
    // demostracion.
    // =========================================================

    @Volatile
    private var realBuses: List<Bus> = emptyList()

    private val serviceScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default
    )


    // =========================================================
    // CONTROL
    // =========================================================

    private var tracking = false
    private var wakeLock: PowerManager.WakeLock? = null

    private var arrivalNotificationStopId: Int? = null

    /**
     * Paradero donde se detectó la espera más reciente.
     *
     * El abordaje por co-presencia ocurre lejos de la UI del
     * paradero, así que se recuerda aqui para que los puntos
     * queden asociados al lugar donde subiste.
     */
    private var lastStopId: Int = 0


    companion object {

        private const val CHANNEL_ID =
            "smartbus_location_channel"

        private const val NOTIFICATION_ID =
            1001

        const val ACTION_START =
            "SMARTBUS_START_LOCATION"

        const val ACTION_STOP =
            "SMARTBUS_STOP_LOCATION"

        private const val POINTS_PER_TRIP = 20
    }


    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate() {

        super.onCreate()

        fusedLocationClient =
            LocationServices
                .getFusedLocationProviderClient(this)

        pointsRepository =
            PointsRepository(this)

        rewardManager =
            BoardingRewardManager(this)

        smartBusNotificationManager =
            SmartBusNotificationManager(this)

        transitRepository =
            RoomTransitRepository.get(this)

        observeRealBuses()

        createNotificationChannel()

        createLocationCallback()
    }


    // =========================================================
    // BUSES REALES
    //
    // Se suscribe una sola vez a la tabla de vehiculos. El
    // conductor espejea ahi su GPS real, asi que este es el dato
    // con el que se comprueba que el pasajero va con el bus.
    // =========================================================

    private fun observeRealBuses() {

        serviceScope.launch {

            try {

                kotlinx.coroutines.flow.combine(
                    transitRepository.observeVehicles(),
                    com.ejemplo.smartbus.data.BusLiveRepository.buses
                ) { roomVehicles, liveVehicles ->
                    (liveVehicles + roomVehicles)
                        .filter { !it.isMock }
                        .distinctBy { it.id }
                }.collect { buses ->
                    realBuses = buses
                }

            } catch (exception: Exception) {

                android.util.Log.w(
                    "SmartBusTracking",
                    "No se pudieron leer los buses: ${exception.message}"
                )
            }
        }
    }


    // =========================================================
    // ON START COMMAND
    // =========================================================

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        when (intent?.action) {

            ACTION_START -> {
                startTracking()
            }

            ACTION_STOP -> {
                stopTracking()
                stopSelf()
            }

            null -> {
                startTracking()
            }
        }

        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (tracking) {
            val restartIntent = Intent(applicationContext, LocationTrackingService::class.java).apply {
                action = ACTION_START
            }
            try {
                ContextCompat.startForegroundService(applicationContext, restartIntent)
            } catch (_: Exception) {
            }
        }
    }


    // =========================================================
    // INICIAR SEGUIMIENTO
    // =========================================================

    private fun startTracking() {

        if (tracking) {
            return
        }


        // -----------------------------------------------------
        // PERMISOS
        // -----------------------------------------------------

        val fineGranted =
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED


        val coarseGranted =
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED


        if (!fineGranted && !coarseGranted) {

            TrackingEventManager.stopTracking()

            stopSelf()

            return
        }


        // -----------------------------------------------------
        // FOREGROUND SERVICE
        // -----------------------------------------------------

        val notification =
            createPersistentNotification()


        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.Q
        ) {

            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo
                    .FOREGROUND_SERVICE_TYPE_LOCATION
            )

        } else {

            startForeground(
                NOTIFICATION_ID,
                notification
            )
        }


        tracking = true
        acquireWakeLock()

        arrivalNotificationStopId = null

        TrackingEventManager.startTracking()


        // -----------------------------------------------------
        // GPS
        // -----------------------------------------------------

        val locationRequest =
            LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                5_000L
            )
                .setMinUpdateIntervalMillis(
                    2_000L
                )
                .setMinUpdateDistanceMeters(
                    10f
                )
                .build()


        try {

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                mainLooper
            )

        } catch (
            exception: SecurityException
        ) {

            tracking = false

            TrackingEventManager.stopTracking()

            stopSelf()
        }
    }


    // =========================================================
    // CALLBACK
    // =========================================================

    private fun createLocationCallback() {

        locationCallback =
            object : LocationCallback() {

                override fun onLocationResult(
                    result: LocationResult
                ) {

                    for (location in result.locations) {

                        processLocation(location)
                    }
                }
            }
    }


    // =========================================================
    // PROCESAR UBICACIÓN
    // =========================================================

    private fun processLocation(
        location: Location
    ) {

        // =====================================================
        // VERIFICACIÓN PRINCIPAL: PASAJERO + CONDUCTOR JUNTOS
        //
        // Si las dos ubicaciones coinciden durante un trayecto
        // real, el abordaje queda confirmado sin depender solo
        // de la velocidad.
        // =====================================================

        val activeStops = com.ejemplo.smartbus.data.FirebaseParaderoRepository.stops.value.ifEmpty {
            BusStopRepository.stops
        }

        val currentStop = BusStopDetector.findCurrentStop(location, activeStops)
        if (currentStop != null) {
            lastStopId = currentStop.id
            if (arrivalNotificationStopId != currentStop.id) {
                smartBusNotificationManager.showArrivalAtStop(currentStop.name)
                arrivalNotificationStopId = currentStop.id
            }
        }

        if (processCoPresence(location, activeStops)) {
            return
        }

        val result =
            boardingDetector.processLocation(
                location = location,
                stops = activeStops,
                buses = realBuses.ifEmpty { com.ejemplo.smartbus.data.BusLiveRepository.buses.value }
            )


        when (result) {


            // =================================================
            // ENTRÓ AL PARADERO
            // =================================================

            is BoardingResult.AtStop -> {

                val stop =
                    result.stop

                // Se recuerda el paradero para que un abordaje
                // confirmado despues por co-presencia quede
                // asociado al lugar donde subiste.
                lastStopId = stop.id

                TrackingEventManager.updateAtStop(
                    stopName =
                        stop.name,

                    distanceMeters =
                        result.distanceMeters
                )


                if (
                    arrivalNotificationStopId !=
                    stop.id
                ) {

                    smartBusNotificationManager
                        .showArrivalAtStop(
                            stop.name
                        )

                    arrivalNotificationStopId =
                        stop.id
                }
            }


            // =================================================
            // ESPERANDO EN EL PARADERO
            // =================================================

            is BoardingResult.WaitingAtStop -> {

                TrackingEventManager
                    .updateWaitingAtStop(

                        stopName =
                            result.stop.name,

                        distanceMeters =
                            result.distanceMeters,

                        timeAtStopMs =
                            result.timeAtStopMs
                    )
            }


            // =================================================
            // MOVIMIENTO, DISTANCIA INSUFICIENTE
            // =================================================

            is BoardingResult.MovingButNotEnough -> {

                TrackingEventManager.updateMoving(

                    stopName =
                        result.stop.name,

                    speedKmh =
                        result.speedKmh,

                    distanceMeters =
                        result.distanceMeters
                )
            }


            // =================================================
            // MOVIMIENTO EN VALIDACIÓN
            // =================================================

            is BoardingResult.MovingForValidation -> {

                TrackingEventManager.updateMoving(

                    stopName =
                        result.stop.name,

                    speedKmh =
                        result.speedKmh,

                    distanceMeters =
                        result.distanceMeters
                )
            }


            // =================================================
            // ABORDAJE VALIDADO
            // =================================================

            is BoardingResult.BoardingValidated -> {

                val stop =
                    result.stop


                val canReward =
                    rewardManager.canReward(
                        stop.id
                    )


                if (canReward) {

                    pointsRepository.addBoardingPoints(

                        stopId =
                            stop.id,

                        stopName =
                            stop.name,

                        points =
                            POINTS_PER_TRIP,
                        reason = "Abordaje validado en paradero ${stop.name} (+$POINTS_PER_TRIP pts)",
                        evidence = "boarded_at_${stop.id}_${System.currentTimeMillis()}"
                    )


                    rewardManager
                        .markRewarded(
                            stop.id
                        )


                    TrackingEventManager
                        .boardingValidated(

                            stopName =
                                stop.name,

                            speedKmh =
                                result.speedKmh,

                            distanceMeters =
                                result.distanceMeters,

                            pointsAdded =
                                POINTS_PER_TRIP
                        )


                    smartBusNotificationManager
                        .showBoardingValidated(

                            stopName =
                                stop.name,

                            points =
                                POINTS_PER_TRIP,

                            totalPoints =
                                pointsRepository.getPoints()
                        )


                    // Sin cuenta, los puntos viven en este telefono: es
                    // lo mas fragil. Se avisa para que los ate a una
                    // sesion y no los pierda al cambiar de dispositivo.
                    val session =
                        SessionManager.get(this).session.value

                    if (session == null || session.isGuest) {

                        smartBusNotificationManager
                            .showSignInToSavePoints(
                                pointsRepository.getPoints()
                            )
                    }
                }
            }


            // =================================================
            // MOVIMIENTO MUY LENTO
            // =================================================

            is BoardingResult.MovingTooSlowly -> {

                TrackingEventManager.updateStatus(

                    status =
                        "Movimiento insuficiente",

                    stopName =
                        result.stop.name,

                    speedKmh =
                        result.speedKmh
                )
            }


            // =================================================
            // VELOCIDAD NO DISPONIBLE
            // =================================================

            is BoardingResult.SpeedUnavailable -> {

                TrackingEventManager.updateStatus(

                    status =
                        "Esperando datos de velocidad",

                    stopName =
                        result.stop.name
                )
            }


            // =================================================
            // GPS POCO PRECISO
            // =================================================

            is BoardingResult.GpsAccuracyLow -> {

                TrackingEventManager.updateStatus(

                    status =
                        "Esperando una señal GPS más precisa",

                    stopName =
                        result.stop.name
                )
            }


            // =================================================
            // SALIÓ DEMASIADO RÁPIDO
            // =================================================

            is BoardingResult.LeftTooQuickly -> {

                arrivalNotificationStopId =
                    null

                TrackingEventManager.updateStatus(

                    status =
                        "Saliste del paradero demasiado rápido",

                    stopName =
                        result.stop.name
                )
            }


            // =================================================
            // SESIÓN EXPIRADA
            // =================================================

            is BoardingResult.SessionExpired -> {

                arrivalNotificationStopId =
                    null

                TrackingEventManager.updateStatus(

                    status =
                        "Sesión de abordaje finalizada",

                    stopName =
                        result.stop.name
                )
            }


            // =================================================
            // YA VALIDADO
            // =================================================

            is BoardingResult.AlreadyValidated -> {

                TrackingEventManager.updateStatus(

                    status =
                        "Abordaje ya validado",

                    stopName =
                        result.stop.name
                )
            }


            // =================================================
            // NINGÚN PARADERO
            // =================================================

            BoardingResult.NoStop -> {

                arrivalNotificationStopId =
                    null

                TrackingEventManager.updateStatus(

                    status =
                        "Buscando paraderos..."
                )
            }
        }
    }


    // =========================================================
    // VERIFICACIÓN POR CO-PRESENCIA
    //
    // Devuelve true si el abordaje ya quedó confirmado por este
    // camino y el resto del procesamiento no debe continuar.
    // =========================================================

    private fun processCoPresence(location: Location, stops: List<BusStop>): Boolean {

        val buses = realBuses

        // Sin buses reales todavía no hay con quién comparar:
        // se deja pasar al detector de respaldo (velocidad).
        if (buses.isEmpty()) {
            return false
        }

        val result =
            tripCoPresenceDetector.process(
                passengerLocation = location,
                buses = buses
            )

        return when (result) {


            // --------------------------------------------------
            // VAN JUNTOS: PROGRESANDO
            // --------------------------------------------------

            is TripCoPresenceDetector.Result.InProgress -> {

                TrackingEventManager.updateStatus(
                    status = "Verificando que viajes en el bus...",
                    distanceMeters = result.distanceMeters,
                    speedKmh = result.speedKmh
                )

                false
            }


            // --------------------------------------------------
            // TRAYECTO JUNTOS CONFIRMADO
            // --------------------------------------------------

            is TripCoPresenceDetector.Result.Confirmed -> {
                val nearbyStop = stops.firstOrNull { stop ->
                    val dist = FloatArray(1)
                    Location.distanceBetween(location.latitude, location.longitude, stop.latitude, stop.longitude, dist)
                    dist[0] <= stop.radiusMeters + 120f
                }
                val stopToUse = nearbyStop ?: stops.firstOrNull { it.id == lastStopId }

                if (stopToUse != null) {
                    grantBoarding(
                        reason = "Abordaje verificado en paradero ${stopToUse.name}: viajaste junto al conductor durante ${result.togetherMs / 1000}s",
                        evidence = "cotrip_${result.busCode}_${System.currentTimeMillis()}",
                        speedKmh = result.speedKmh,
                        distanceMeters = result.distanceMeters,
                        stopName = stopToUse.name
                    )
                } else {
                    TrackingEventManager.updateStatus(
                        status = "Debes tomar el bus en un paradero oficial para recibir puntos",
                        distanceMeters = result.distanceMeters,
                        speedKmh = result.speedKmh
                    )
                }

                true
            }


            TripCoPresenceDetector.Result.AlreadyConfirmed -> true


            // --------------------------------------------------
            // ESTADOS QUE NO DETIENEN EL RESPALDO
            //
            // Si no hay bus cerca o el GPS está flojo, sigue el
            // detector de paradero + velocidad.
            // --------------------------------------------------

            is TripCoPresenceDetector.Result.NoBusNearby,
            is TripCoPresenceDetector.Result.LowAccuracy,
            is TripCoPresenceDetector.Result.WaitingSamples -> false


            // --------------------------------------------------
            // VELOCIDAD O RUMBO NO COINCIDE
            //
            // Está cerca de un bus pero todavía no viaja a bordo
            // (todavía no subió, camina, o va en sentido
            // contrario). Se informa y se deja pasar al detector
            // de respaldo, que exige sus propias condiciones
            // (paradero + movimiento sostenido) antes de premiar.
            // --------------------------------------------------

            is TripCoPresenceDetector.Result.SpeedMismatch -> {

                TrackingEventManager.updateStatus(
                    status = "No viajas en el bus: velocidad o dirección no coinciden",
                    distanceMeters = result.distanceMeters,
                    speedKmh = result.passengerSpeedKmh
                )

                false
            }


            // --------------------------------------------------
            // UBICACIÓN FALSA
            // --------------------------------------------------

            TripCoPresenceDetector.Result.MockLocation -> {

                TrackingEventManager.updateStatus(
                    status = "Ubicación no válida: no se otorgan puntos"
                )

                true
            }
        }
    }


    // =========================================================
    // OTORGAR PUNTOS
    //
    // Un solo lugar para dar puntos, así los dos caminos de
    // verificación comparten el mismo control anti-fraude
    // (tiempo mínimo entre recompensas).
    // =========================================================

    private fun grantBoarding(
        reason: String,
        evidence: String,
        speedKmh: Float,
        distanceMeters: Float,
        stopName: String
    ) {

        val stopId = lastStopId

        if (!rewardManager.canReward(stopId)) {
            return
        }

        pointsRepository.addBoardingPoints(
            stopId = stopId,
            stopName = stopName,
            points = POINTS_PER_TRIP,
            reason = reason,
            evidence = evidence
        )

        rewardManager.markRewarded(stopId)

        TrackingEventManager.boardingValidated(
            stopName = stopName,
            speedKmh = speedKmh,
            distanceMeters = distanceMeters,
            pointsAdded = POINTS_PER_TRIP
        )

        smartBusNotificationManager.showBoardingValidated(
            stopName = stopName,
            points = POINTS_PER_TRIP,
            totalPoints = pointsRepository.getPoints()
        )

        val session =
            SessionManager.get(this).session.value

        if (session == null || session.isGuest) {

            smartBusNotificationManager.showSignInToSavePoints(
                pointsRepository.getPoints()
            )
        }
    }


    // =========================================================
    // DETENER GPS
    // =========================================================

    private fun acquireWakeLock() {
        try {
            if (wakeLock == null) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
                wakeLock = powerManager?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "SmartBus:PassengerGpsWakeLock"
                )?.apply {
                    setReferenceCounted(false)
                }
            }
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(4 * 60 * 60 * 1000L)
            }
        } catch (_: Exception) {
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {
        }
    }

    private fun stopTracking() {

        if (
            ::fusedLocationClient.isInitialized &&
            ::locationCallback.isInitialized
        ) {

            fusedLocationClient
                .removeLocationUpdates(
                    locationCallback
                )
        }

        releaseWakeLock()
        tracking = false

        arrivalNotificationStopId =
            null

        boardingDetector.resetSession()
        tripCoPresenceDetector.resetSession()

        TrackingEventManager.stopTracking()
    }


    // =========================================================
    // CANAL DE NOTIFICACIONES
    // =========================================================

    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Ubicación SmartBus",
                    NotificationManager
                        .IMPORTANCE_LOW
                )


            channel.description =
                "Seguimiento GPS de SmartBus"


            val manager =
                getSystemService(
                    NotificationManager::class.java
                )


            manager.createNotificationChannel(
                channel
            )
        }
    }


    // =========================================================
    // NOTIFICACIÓN PERMANENTE
    // =========================================================

    private fun createPersistentNotification():
            Notification {

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(this, 1001, openIntent, flags)

        return NotificationCompat
            .Builder(
                this,
                CHANNEL_ID
            )
            .setContentTitle(
                "MistiGo activo en segundo plano"
            )
            .setContentText(
                "Detectando paraderos, viajes y puntos (+20 pts)"
            )
            .setSmallIcon(
                android.R.drawable
                    .ic_menu_mylocation
            )
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(
                NotificationCompat
                    .PRIORITY_LOW
            )
            .build()
    }


    // =========================================================
    // BIND
    // =========================================================

    override fun onBind(
        intent: Intent?
    ): IBinder? {

        return null
    }


    // =========================================================
    // DESTROY
    // =========================================================

    override fun onDestroy() {

        stopTracking()

        serviceScope.cancel()

        super.onDestroy()
    }
}