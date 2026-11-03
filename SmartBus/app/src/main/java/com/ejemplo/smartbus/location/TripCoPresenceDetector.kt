package com.ejemplo.smartbus.location

import android.location.Location
import android.os.Build
import com.ejemplo.smartbus.data.distanceInMeters
import com.ejemplo.smartbus.data.model.Bus
import kotlin.math.abs

/**
 * Valida el abordaje comprobando que el pasajero y el conductor
 * viajaron JUNTOS en el mismo trayecto.
 *
 * Es la comprobacion mas fuerte que se puede hacer solo con el
 * celular de los dos: no basta con estar cerca un instante, hay que
 * mantenerse pegado al vehiculo mientras este se mueve, a la misma
 * velocidad y en la misma direccion, durante un tramo real.
 *
 * Para saltarselo habria que conducir pegado a un bus, igualando su
 * velocidad y su rumbo, durante mas de un minuto y varios cientos de
 * metros: no es algo que un peaton o un app de GPS falso pueda imitar.
 *
 * Caminar rapido no sirve (va a 5 km/h y un bus va a 30), ir en auto
 * propio tampoco (la velocidad y el rumbo no coinciden sostenidamente)
 * y estar parado en la parada tampoco (no hay trayecto).
 *
 * La velocidad por si sola queda como respaldo en [BusBoardingDetector].
 */
class TripCoPresenceDetector {

    companion object {

        /**
         * Distancia maxima entre el pasajero y el bus para considerar
         * que van dentro del mismo vehiculo. Cubre el error normal del
         * GPS en ciudad, pero no un vehiculo que circula en la via
         * paralela ni a un costado.
         */
        const val MAX_TOGETHER_DISTANCE_M = 40f

        /**
         * Diferencia maxima de velocidad. Si viajas dentro del bus,
         * tu velocidad es la del bus: la diferencia es solo ruido del
         * GPS. Un peaton o un auto distinto jamas la sostiene.
         */
        const val MAX_SPEED_DIFFERENCE_KMH = 12f

        /**
         * Diferencia maxima de rumbo. Ambos deben ir hacia donde va la
         * ruta, no en sentido contrario ni de costado.
         */
        const val MAX_HEADING_DIFFERENCE_DEG = 60f

        /** Tiempo minimo viajando juntos para dar el abordaje. */
        const val MIN_TOGETHER_TIME_MS = 30_000L

        /** Distancia minima recorrida juntos. Cubre varios paraderos. */
        const val MIN_TOGETHER_DISTANCE_M = 300f

        /** Lecturas consecutivas dentro del vehiculo. */
        const val MIN_SAMPLES = 6

        /** Si el paso se corta, se pierde la acumulacion. */
        const val MAX_SAMPLE_GAP_MS = 20_000L

        /** Precision GPS maxima aceptada en el pasajero. */
        const val MAX_PASSENGER_ACCURACY_M = 60f

        /** Ventana para buscar el bus mas cercano. */
        const val SEARCH_RADIUS_M = 120f
    }


    // =========================================================
    // ESTADO
    // =========================================================

    private var togetherSinceMs: Long = 0L

    private var lastSampleMs: Long = 0L

    private var consecutiveSamples: Int = 0

    private var togetherDistanceM: Float = 0f

    private var lastPassenger: Location? = null

    /** Para derivar el rumbo y la velocidad reales del bus. */
    private var lastBusLatitude: Double? = null

    private var lastBusLongitude: Double? = null

    private var lastBusSampleMs: Long = 0L

    private var busHeadingDegrees: Float? = null

    private var validated: Boolean = false

    private var validatedBusCode: String = ""


    // =========================================================
    // RESULTADO
    // =========================================================

    sealed class Result {

        /** Pasajero y conductor viajaron juntos: abordaje confirmado. */
        data class Confirmed(
            val busCode: String,
            val distanceMeters: Float,
            val speedKmh: Float,
            val togetherMs: Long,
            val togetherDistanceMeters: Float
        ) : Result()

        /** Van juntos pero todavia no se acumula lo suficiente. */
        data class InProgress(
            val busCode: String,
            val distanceMeters: Float,
            val speedKmh: Float,
            val elapsedMs: Long,
            val coveredMeters: Float,
            val requiredMeters: Float
        ) : Result()

        /** Ya se confirmo antes en esta sesion. */
        data object AlreadyConfirmed : Result()

        /** El pasajero no esta cerca de ningun bus. */
        data class NoBusNearby(
            val nearestDistanceMeters: Float
        ) : Result()

        /** Cerca del bus pero la velocidad no coincide (no va a bordo). */
        data class SpeedMismatch(
            val distanceMeters: Float,
            val passengerSpeedKmh: Float,
            val busSpeedKmh: Float
        ) : Result()

        /** GPS del pasajero demasiado impreciso para verificar. */
        data class LowAccuracy(
            val accuracyMeters: Float
        ) : Result()

        /** Ubicacion falsa en el pasajero: no se premia. */
        data object MockLocation : Result()

        /** Todavia no hay lecturas suficientes. */
        data object WaitingSamples : Result()
    }


    // =========================================================
    // PROCESAR
    // =========================================================

    fun process(
        passengerLocation: Location,
        buses: List<Bus>
    ): Result {

        val now = System.currentTimeMillis()

        // -----------------------------------------------------
        // UBICACION FALSA
        //
        // Un emulador o una app de GPS falso mueve al pasajero
        // donde quiera. Si eso diera puntos, el sistema no
        // serviria: se rechaza igual que en el conductor.
        // -----------------------------------------------------

        if (isMockLocation(passengerLocation)) {
            return Result.MockLocation
        }

        // -----------------------------------------------------
        // PRECISION
        // -----------------------------------------------------

        if (passengerLocation.hasAccuracy() &&
            passengerLocation.accuracy > MAX_PASSENGER_ACCURACY_M
        ) {
            return Result.LowAccuracy(passengerLocation.accuracy)
        }

        // -----------------------------------------------------
        // BUS MAS CERCANO
        // -----------------------------------------------------

        val nearest = findNearestBus(passengerLocation, buses)

        if (nearest == null) {
            resetAccumulation(now)
            return Result.NoBusNearby(Float.MAX_VALUE)
        }

        val (bus, distanceMeters) = nearest

        // Se salio del vehiculo: se pierde lo acumulado.
        if (distanceMeters > MAX_TOGETHER_DISTANCE_M) {
            resetAccumulation(now)
            return Result.NoBusNearby(distanceMeters)
        }

        // -----------------------------------------------------
        // VELOCIDAD
        // -----------------------------------------------------

        val passengerSpeedKmh = speedKmhOf(passengerLocation)

        val busSpeedKmh = effectiveBusSpeed(bus, now)

        if (abs(passengerSpeedKmh - busSpeedKmh) > MAX_SPEED_DIFFERENCE_KMH) {
            resetAccumulation(now)
            return Result.SpeedMismatch(
                distanceMeters = distanceMeters,
                passengerSpeedKmh = passengerSpeedKmh,
                busSpeedKmh = busSpeedKmh
            )
        }

        // -----------------------------------------------------
        // RUMBO
        //
        // Solo se exige si el pasajero realmente informa rumbo:
        // hay dispositivos que no lo dan y no debe castigarse.
        // -----------------------------------------------------

        val passengerHeading = headingOf(passengerLocation)

        val busHeading = trackBusHeading(bus, now)

        if (passengerHeading != null && busHeading != null) {

            val difference = headingDifference(passengerHeading, busHeading)

            if (difference > MAX_HEADING_DIFFERENCE_DEG) {
                resetAccumulation(now)
                return Result.SpeedMismatch(
                    distanceMeters = distanceMeters,
                    passengerSpeedKmh = passengerSpeedKmh,
                    busSpeedKmh = busSpeedKmh
                )
            }
        }

        // -----------------------------------------------------
        // ACUMULACION DEL TRAYECTO
        // -----------------------------------------------------

        // Primera lectura junta o hubo una pausa larga: se empieza
        // de cero para no sumar un viaje que en realidad son dos.
        if (togetherSinceMs == 0L ||
            (lastSampleMs > 0L && now - lastSampleMs > MAX_SAMPLE_GAP_MS)
        ) {
            togetherSinceMs = now
            togetherDistanceM = 0f
            consecutiveSamples = 0
        }

        val passengerBefore = lastPassenger

        if (passengerBefore != null) {
            togetherDistanceM += distanceInMeters(
                passengerBefore.latitude,
                passengerBefore.longitude,
                passengerLocation.latitude,
                passengerLocation.longitude
            )
        }

        lastPassenger = passengerLocation
        lastSampleMs = now
        consecutiveSamples++

        val elapsedMs = now - togetherSinceMs

        // -----------------------------------------------------
        // TODAVIA NO ES SUFICIENTE
        // -----------------------------------------------------

        if (consecutiveSamples < MIN_SAMPLES ||
            elapsedMs < MIN_TOGETHER_TIME_MS ||
            togetherDistanceM < MIN_TOGETHER_DISTANCE_M
        ) {
            return Result.InProgress(
                busCode = bus.code,
                distanceMeters = distanceMeters,
                speedKmh = passengerSpeedKmh,
                elapsedMs = elapsedMs,
                coveredMeters = togetherDistanceM,
                requiredMeters = MIN_TOGETHER_DISTANCE_M
            )
        }

        // -----------------------------------------------------
        // TRAYECTO JUNTOS CONFIRMADO
        // -----------------------------------------------------

        if (!validated) {
            validated = true
            validatedBusCode = bus.code
        }

        return Result.Confirmed(
            busCode = validatedBusCode.ifEmpty { bus.code },
            distanceMeters = distanceMeters,
            speedKmh = passengerSpeedKmh,
            togetherMs = elapsedMs,
            togetherDistanceMeters = togetherDistanceM
        )
    }


    // =========================================================
    // UTILIDADES
    // =========================================================

    private fun findNearestBus(
        passengerLocation: Location,
        buses: List<Bus>
    ): Pair<Bus, Float>? {

        var bestBus: Bus? = null
        var bestDistance = Float.MAX_VALUE

        for (bus in buses) {

            // Un bus simulado no es un vehiculo en servicio: no se
            // puede confirmar un abordaje contra la demo.
            if (bus.isMock) {
                continue
            }

            val distance = distanceInMeters(
                passengerLocation.latitude,
                passengerLocation.longitude,
                bus.latitude,
                bus.longitude
            )

            if (distance < bestDistance) {
                bestDistance = distance
                bestBus = bus
            }
        }

        val bus = bestBus ?: return null

        if (bestDistance > SEARCH_RADIUS_M) {
            return null
        }

        return bus to bestDistance
    }

    /**
     * Velocidad real del bus.
     *
     * Usa la que informa el conductor si es utilizable; si no, la que
     * sale de moverse entre dos lecturas, que sirve aunque el feed no
     * escriba velocidad.
     */
    private fun effectiveBusSpeed(bus: Bus, now: Long): Float {

        if (bus.speedKmh > 0f) {
            return bus.speedKmh
        }

        val previousLat = lastBusLatitude
        val previousLng = lastBusLongitude
        val previousMs = lastBusSampleMs

        if (previousLat == null || previousLng == null || previousMs == 0L) {
            return 0f
        }

        val elapsedSeconds = (now - previousMs) / 1000.0

        if (elapsedSeconds <= 0.5) {
            return 0f
        }

        val meters = distanceInMeters(
            previousLat,
            previousLng,
            bus.latitude,
            bus.longitude
        )

        return (meters / elapsedSeconds).toFloat() * 3.6f
    }

    /**
     * Rumbo del bus derivado de sus propias lecturas.
     *
     * El modelo no lo trae, pero para exigir que ambos vayan en la
     * misma direccion basta con mirar hacia donde se mueve.
     */
    private fun trackBusHeading(bus: Bus, now: Long): Float? {

        val previousLat = lastBusLatitude
        val previousLng = lastBusLongitude
        val previousMs = lastBusSampleMs

        lastBusLatitude = bus.latitude
        lastBusLongitude = bus.longitude
        lastBusSampleMs = now

        if (previousLat == null || previousLng == null || previousMs == 0L) {
            return busHeadingDegrees
        }

        if (now - previousMs > MAX_SAMPLE_GAP_MS) {
            return busHeadingDegrees
        }

        val lat1 = Math.toRadians(previousLat)
        val lat2 = Math.toRadians(bus.latitude)

        val deltaLng = Math.toRadians(bus.longitude - previousLng)

        val y = kotlin.math.sin(deltaLng) * kotlin.math.cos(lat2)
        val x = kotlin.math.cos(lat1) * kotlin.math.sin(lat2) -
                kotlin.math.sin(lat1) * kotlin.math.cos(lat2) *
                kotlin.math.cos(deltaLng)

        val bearing = Math.toDegrees(kotlin.math.atan2(y, x))
            .toFloat()

        busHeadingDegrees = (bearing + 360f) % 360f

        return busHeadingDegrees
    }

    private fun resetAccumulation(now: Long) {
        togetherSinceMs = 0L
        togetherDistanceM = 0f
        consecutiveSamples = 0
        lastSampleMs = 0L
        lastPassenger = null
    }

    private fun speedKmhOf(location: Location): Float =
        if (location.hasSpeed()) location.speed * 3.6f else 0f

    private fun headingOf(location: Location): Float? =
        if (location.hasBearing()) location.bearing else null

    private fun headingDifference(a: Float, b: Float): Float {
        val raw = abs(((a - b + 540f) % 360f) - 180f)
        return raw
    }

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
     * Marca la sesion como ya confirmada para no repetir el premio
     * en cada lectura siguiente.
     */
    fun markConfirmed() {
        validated = true
    }

    fun isConfirmed(): Boolean = validated

    /** Reinicia todo al terminar el seguimiento. */
    fun resetSession() {
        togetherSinceMs = 0L
        lastSampleMs = 0L
        consecutiveSamples = 0
        togetherDistanceM = 0f
        lastPassenger = null
        lastBusLatitude = null
        lastBusLongitude = null
        lastBusSampleMs = 0L
        busHeadingDegrees = null
        validated = false
        validatedBusCode = ""
    }
}
