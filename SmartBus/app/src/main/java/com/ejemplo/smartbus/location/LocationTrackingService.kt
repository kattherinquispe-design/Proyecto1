package com.ejemplo.smartbus.location

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.ejemplo.smartbus.data.BoardingRewardManager
import com.ejemplo.smartbus.data.BusStopRepository
import com.ejemplo.smartbus.data.PointsEventManager
import com.ejemplo.smartbus.data.PointsRepository
import com.ejemplo.smartbus.data.SessionManager
import com.ejemplo.smartbus.data.SmartBusNotificationManager
import com.ejemplo.smartbus.data.TrackingEventManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority


class LocationTrackingService : Service() {

    // =========================================================
    // GPS
    // =========================================================

    private lateinit var fusedLocationClient:
            FusedLocationProviderClient

    private lateinit var locationCallback:
            LocationCallback


    // =========================================================
    // DETECTOR
    // =========================================================

    private val boardingDetector =
        BusBoardingDetector()


    // =========================================================
    // REPOSITORIOS
    // =========================================================

    private lateinit var pointsRepository:
            PointsRepository

    private lateinit var rewardManager:
            BoardingRewardManager

    private lateinit var smartBusNotificationManager:
            SmartBusNotificationManager


    // =========================================================
    // CONTROL
    // =========================================================

    private var tracking = false

    private var arrivalNotificationStopId: Int? = null


    companion object {

        private const val CHANNEL_ID =
            "smartbus_location_channel"

        private const val NOTIFICATION_ID =
            1001

        const val ACTION_START =
            "SMARTBUS_START_LOCATION"

        const val ACTION_STOP =
            "SMARTBUS_STOP_LOCATION"
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

        createNotificationChannel()

        createLocationCallback()
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
        }

        return START_STICKY
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

        val result =
            boardingDetector.processLocation(
                location = location,
                stops = BusStopRepository.stops,
                buses = com.ejemplo.smartbus.data.BusLiveRepository.buses.value
            )


        when (result) {


            // =================================================
            // ENTRÓ AL PARADERO
            // =================================================

            is BoardingResult.AtStop -> {

                val stop =
                    result.stop

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
                            50,
                        reason = "Abordaje validado (GPS + movimiento + bus en zona)",
                        evidence = "boarded_at_${stop.id}_${System.currentTimeMillis()}"
                    )


                    PointsEventManager
                        .notifyPointsAdded(50)


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
                                50
                        )


                    smartBusNotificationManager
                        .showBoardingValidated(

                            stopName =
                                stop.name,

                            points =
                                50
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
    // DETENER GPS
    // =========================================================

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

        tracking = false

        arrivalNotificationStopId =
            null

        boardingDetector.resetSession()

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

        return NotificationCompat
            .Builder(
                this,
                CHANNEL_ID
            )
            .setContentTitle(
                "SmartBus activo"
            )
            .setContentText(
                "Detectando tu recorrido y abordaje"
            )
            .setSmallIcon(
                android.R.drawable
                    .ic_menu_mylocation
            )
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

        super.onDestroy()
    }
}