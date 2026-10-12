package com.ejemplo.smartbus.location

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ejemplo.smartbus.data.SessionManager
import com.ejemplo.smartbus.data.driver.DriverRepository
import com.ejemplo.smartbus.data.model.GeoPoint
import com.ejemplo.smartbus.data.model.UserRole
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
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * Comparte la ubicacion del conductor mientras esta online.
 *
 * Es un servicio en primer plano porque el GPS tiene que seguir
 * funcionando con la pantalla apagada y la app en segundo plano: sin
 * eso Android corta el proceso y el pasajero dejaria de ver el vehiculo.
 *
 * Diferencias con el seguimiento del pasajero:
 *  - Este manda la posicion a la base de datos (el pasajero la ve).
 *  - El del pasajero sirve para detectar abordajes y dar puntos.
 */
class DriverLocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private lateinit var driverRepository: DriverRepository

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    private var accountId: String? = null
    private var tracking = false

    /**
     * Trazado de la ruta asignada. Se carga al empezar y sirve para
     * vigilar que el conductor no se salga del recorrido.
     */
    @Volatile
    private var routePoints: List<GeoPoint> = emptyList()

    /** Momento en que empezo el desvio actual, 0 si va por la ruta. */
    private var deviationStartedAt = 0L

    /** Evita disparar dos veces la parada por desvio. */
    private var deviationHandled = false

    override fun onCreate() {

        super.onCreate()

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        driverRepository = DriverRepository(this)

        createNotificationChannel()
        createLocationCallback()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        when (intent?.action) {

            ACTION_START -> {
                accountId = intent.getStringExtra(EXTRA_ACCOUNT_ID)
                startTracking()
            }

            ACTION_STOP -> {
                if (accountId == null) {
                    accountId = intent?.getStringExtra(EXTRA_ACCOUNT_ID)
                        ?: SessionManager.get(this).session.value?.storageKey
                }
                shutdown()
            }
        }

        // Si el sistema mata el servicio, se reinicia solo con la
        // ultima cuenta conocida para no perder el viaje.
        return START_STICKY
    }

    // =====================================================
    // GPS
    // =====================================================

    private fun startTracking() {

        if (tracking) {
            return
        }

        val session = SessionManager.get(this).session.value

        if (session == null || session.role != UserRole.DRIVER) {
            Log.w(TAG, "No se inicia el GPS: la sesion no es de conductor")
            stopSelf()
            return
        }

        if (accountId == null) {
            accountId = session.storageKey
        }

        if (!hasLocationPermission()) {
            Log.w(TAG, "No se inicia el GPS: falta permiso de ubicacion")
            stopSelf()
            return
        }

        val notification = createPersistentNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        tracking = true
        deviationStartedAt = 0L
        deviationHandled = false

        // La ruta asignada se lee en segundo plano para no bloquear el
        // arranque del servicio. Sin ruta no se puede vigilar el desvio.
        val currentAccount = accountId
        if (currentAccount != null) {
            scope.launch {
                routePoints = try {
                    driverRepository.getAssignedRoutePoints(currentAccount)
                } catch (exception: Exception) {
                    Log.w(TAG, "No se pudo leer la ruta: ${exception.message}")
                    emptyList()
                }
            }
        }

        val request =
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
                .setMinUpdateIntervalMillis(MIN_UPDATE_INTERVAL_MS)
                .setMinUpdateDistanceMeters(MIN_UPDATE_DISTANCE_M)
                .setWaitForAccurateLocation(false)
                .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                mainLooper
            )
        } catch (exception: SecurityException) {
            Log.w(TAG, "GPS del conductor rechazado: ${exception.message}")
            tracking = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun createLocationCallback() {

        locationCallback =
            object : LocationCallback() {

                override fun onLocationResult(result: LocationResult) {

                    for (location in result.locations) {
                        publish(location)
                    }
                }
            }
    }

    /**
     * Escribe la posicion en la base. Solo si el fix es bueno: una
     * ubicacion de 500 m de error dibujaria el vehiculo donde no esta.
     */
    private fun publish(location: Location) {

        val id = accountId ?: return

        if (location.hasAccuracy() && location.accuracy > MAX_ACCEPTED_ACCURACY_M) {
            return
        }

        // Ubicacion falsa: una app de GPS simulado, un emulador o una
        // extension del navegador pueden mover el vehiculo sin que
        // exista. Si el conductor hace eso, el pasajero esta en
        // peligro, asi que la posicion no se publica.
        if (isMockLocation(location)) {

            Log.w(TAG, "Se descarto una ubicacion simulada")

            return
        }

        // Vigilancia del recorrido: si el conductor se aleja de su ruta
        // mas de lo permitido y no vuelve en el tiempo maximo, se deja
        // de publicar su ubicacion. Asi el pasajero deja de ver un
        // vehiculo "en ruta" que en realidad esta yendo a otro lado.
        if (checkRouteDeviation(location)) {
            return
        }

        val session = SessionManager.get(this).session.value

        if (session == null || session.role != UserRole.DRIVER) {
            stopTracking()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        scope.launch {
            try {
                driverRepository.publishLocation(
                    accountId = id,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    speedKmh = if (location.hasSpeed()) {
                        location.speed * 3.6f
                    } else {
                        0f
                    },
                    headingDegrees = if (location.hasBearing()) {
                        location.bearing
                    } else {
                        null
                    },
                    gpsAccuracyMeters = if (location.hasAccuracy()) {
                        location.accuracy
                    } else {
                        null
                    }
                )
            } catch (exception: Exception) {
                Log.w(
                    TAG,
                    "No se pudo publicar la posicion: ${exception.message}"
                )
            }
        }
    }

    /**
     * True si la posicion viene de un proveedor simulado.
     *
     * FusedLocationProvider avisa con isFromMockProvider, y desde
     * Android 12 el propio Location trae isMock. Se miran los dos
     * porque no todos los dispositivos rellenan el segundo.
     */
    private fun isMockLocation(location: Location): Boolean {

        if (location.isFromMockProvider) {
            return true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return location.isMock
        }

        return false
    }

    /**
     * Comprueba si el conductor sigue dentro de su ruta.
     *
     * Devuelve true si el servicio ya se detuvo por desvio (y entonces
     * la posicion no debe publicarse). Mientras esta dentro de la
     * tolerancia, reinicia el contador.
     */
    private fun checkRouteDeviation(location: Location): Boolean {

        if (deviationHandled) {
            return true
        }

        val points = routePoints

        // Sin ruta cargada no hay nada que vigilar.
        if (points.size < 2) {
            return false
        }

        val distance = distanceToRouteMeters(
            location.latitude,
            location.longitude,
            points
        )

        if (distance <= DEVIATION_TOLERANCE_M) {
            deviationStartedAt = 0L
            return false
        }

        val now = System.currentTimeMillis()

        if (deviationStartedAt == 0L) {
            deviationStartedAt = now
            Log.w(TAG, "Desvio detectado: ${distance.toInt()} m de la ruta")
            return false
        }

        if (now - deviationStartedAt < DEVIATION_TIMEOUT_MS) {
            return false
        }

        deviationHandled = true
        handleDeviationStop()
        return true
    }

    /**
     * Cierra el servicio por desvio prolongado: el conductor deja de
     * trabajar y su vehiculo desaparece del mapa del pasajero.
     */
    private fun handleDeviationStop() {

        Log.w(
            TAG,
            "Desvio de mas de ${DEVIATION_TIMEOUT_MS / 60_000} min: " +
                "se deja de compartir la ubicacion"
        )

        shutdown()
    }

    /**
     * Apaga el servicio con orden.
     *
     * Primero cierra el turno (para que el vehiculo deje de estar
     * asociado al conductor) y solo despues se retira. Se hace en una
     * corrutina para no bloquear el hilo principal, pero sin matar el
     * servicio antes de terminar.
     */
    private fun shutdown() {

        val id = accountId

        if (id == null) {
            stopTracking()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        scope.launch {
            try {
                driverRepository.goOffline(id)
            } catch (exception: Exception) {
                Log.w(TAG, "No se pudo cerrar el turno: ${exception.message}")
            } finally {
                stopTracking()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    /**
     * Distancia minima, en metros, del punto a cualquiera de los
     * segmentos de la ruta.
     *
     * Se proyecta sobre un plano local (equirectangular): para tramos
     * de una ciudad el error es de centimetros y evita cargar una
     * libreria de geometria solo para esto.
     */
    private fun distanceToRouteMeters(
        latitude: Double,
        longitude: Double,
        points: List<GeoPoint>
    ): Float {

        if (points.size < 2) {
            return 0f
        }

        val metersPerDegLat = 111_320.0
        val metersPerDegLng =
            111_320.0 * cos(Math.toRadians(latitude))

        var best = Float.MAX_VALUE

        for (index in 0 until points.size - 1) {

            val a = points[index]
            val b = points[index + 1]

            val ax = (a.longitude - longitude) * metersPerDegLng
            val ay = (a.latitude - latitude) * metersPerDegLat
            val bx = (b.longitude - longitude) * metersPerDegLng
            val by = (b.latitude - latitude) * metersPerDegLat

            val dx = bx - ax
            val dy = by - ay
            val lengthSquared = dx * dx + dy * dy

            val t = if (lengthSquared <= 0.0) {
                0.0
            } else {
                ((-ax * dx - ay * dy) / lengthSquared).coerceIn(0.0, 1.0)
            }

            val px = ax + t * dx
            val py = ay + t * dy

            val distance = sqrt(px * px + py * py).toFloat()

            if (distance < best) {
                best = distance
            }
        }

        return best
    }

    private fun stopTracking() {

        if (::fusedLocationClient.isInitialized && ::locationCallback.isInitialized) {            try {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            } catch (exception: SecurityException) {
                Log.w(TAG, "No se pudo quitar el GPS: ${exception.message}")
            }
        }

        tracking = false
    }

    private fun hasLocationPermission(): Boolean {

        val fine = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }

    // =====================================================
    // NOTIFICACION
    // =====================================================

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Servicio de conductor",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Comparte tu ubicacion mientras estas en servicio"
            setShowBadge(false)
        }

        getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(channel)
    }

    private fun createPersistentNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Estas en servicio")
            .setContentText("El pasajero puede ver donde vas")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopTracking()
        scope.cancel()
        super.onDestroy()
    }

    companion object {

        private const val TAG = "SmartBusDriver"

        private const val CHANNEL_ID = "smartbus_driver_channel"
        private const val NOTIFICATION_ID = 2001

        private const val UPDATE_INTERVAL_MS = 5_000L
        private const val MIN_UPDATE_INTERVAL_MS = 2_000L
        private const val MIN_UPDATE_DISTANCE_M = 8f

        /** Un fix peor que esto no se muestra: seria mentir. */
        private const val MAX_ACCEPTED_ACCURACY_M = 100f

        /**
         * Que tan lejos de la ruta se tolera antes de considerar que el
         * conductor se desvio. Cubre el error normal del GPS y las
         * vias paralelas, pero no un viaje a otro distrito.
         */
        private const val DEVIATION_TOLERANCE_M = 350f

        /**
         * Tiempo que puede estar fuera de ruta antes de dejar de
         * compartir la ubicacion. Veinte minutos permiten una vuelta
         * larga o un desvio puntual sin castigar al conductor.
         */
        private const val DEVIATION_TIMEOUT_MS = 20 * 60 * 1000L

        const val ACTION_START = "SMARTBUS_DRIVER_START"
        const val ACTION_STOP = "SMARTBUS_DRIVER_STOP"
        const val EXTRA_ACCOUNT_ID = "account_id"

        fun start(context: Context, accountId: String) {

            val intent = Intent(context, DriverLocationService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_ACCOUNT_ID, accountId)
            }

            try {
                // ContextCompat es obligatorio: startForegroundService
                // como metodo de Context solo existe desde API 26 y
                // el proyecto sirve desde API 24.
                ContextCompat.startForegroundService(context, intent)
            } catch (exception: Exception) {
                Log.w(TAG, "No se pudo arrancar el servicio: ${exception.message}")
            }
        }

        fun stop(context: Context) {

            val intent = Intent(context, DriverLocationService::class.java).apply {
                action = ACTION_STOP
            }

            try {
                context.startService(intent)
            } catch (exception: Exception) {
                Log.w(TAG, "No se pudo parar el servicio: ${exception.message}")
            }
        }
    }
}