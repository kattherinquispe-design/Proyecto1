package com.ejemplo.smartbus.location

import android.location.Location
import android.os.Build
import com.ejemplo.smartbus.data.model.Bus
import com.ejemplo.smartbus.data.model.BusStop


class BusBoardingDetector {

    companion object {

        // Tiempo mínimo en el paradero
        const val MIN_TIME_AT_STOP_MS =
            15_000L

        // Velocidad mínima
        const val MIN_BUS_SPEED_KMH =
            8f

        // Distancia mínima desde el paradero
        const val MIN_DISTANCE_AFTER_STOP_METERS =
            100f

        // Tiempo máximo de una sesión
        const val MAX_SESSION_TIME_MS =
            5 * 60 * 1000L

        // Distancia máxima de una sesión
        const val MAX_SESSION_DISTANCE_METERS =
            1500f

        // Lecturas consecutivas necesarias
        const val MIN_MOVING_SAMPLES =
            3

        // Precisión GPS máxima permitida
        const val MAX_GPS_ACCURACY_METERS =
            75f

        // Distancia a la que un bus se considera en el paradero
        const val BUS_NEAR_STOP_METERS =
            250f

        // Máximo tiempo entre lecturas
        const val MAX_LOCATION_GAP_MS =
            15_000L

        // Intervalo mínimo para calcular velocidad por distancia
        const val MIN_DERIVE_SPEED_GAP_S =
            0.75f

        // Intervalo máximo para calcular velocidad por distancia
        const val MAX_DERIVE_SPEED_GAP_S =
            25f
    }


    // =========================================================
    // ESTADO
    // =========================================================

    private var currentStop:
            BusStop? = null

    private var enteredStopTime:
            Long = 0L

    private var locationWhenEntered:
            Location? = null

    private var lastLocation:
            Location? = null

    private var lastLocationTime:
            Long = 0L

    private var totalDistanceAfterStop:
            Float = 0f

    private var consecutiveMovingSamples:
            Int = 0

    private var boardingValidated:
            Boolean = false

    private var busSeenAtStop:
            Boolean = false


    // =========================================================
    // PROCESAR GPS
    // =========================================================

    fun processLocation(
        location: Location,
        stops: List<BusStop>,
        buses: List<com.ejemplo.smartbus.data.model.Bus> = emptyList()
    ): BoardingResult {

        val currentTime =
            System.currentTimeMillis()


        // =====================================================
        // PRECISIÓN GPS
        // =====================================================

        if (!isLocationAccurate(location)) {

            return if (currentStop != null) {

                BoardingResult.GpsAccuracyLow(

                    stop =
                        currentStop!!,

                    accuracyMeters =
                        location.accuracy
                )

            } else {

                BoardingResult.NoStop
            }
        }


        // =====================================================
        // BUSCAR PARADERO
        // =====================================================

        val stop =
            BusStopDetector.findCurrentStop(
                location,
                stops
            )


        // =====================================================
        // DENTRO DEL PARADERO
        // =====================================================

        if (stop != null) {

            // Un bus compatible en este instante queda registrado
            // como evidencia de que el usuario si abordo.
            if (isBusAtStop(location, stop, buses)) {
                busSeenAtStop = true
            }

            // ---------------------------------------------
            // NUEVO PARADERO
            // ---------------------------------------------

            if (
                currentStop?.id !=
                stop.id
            ) {

                currentStop =
                    stop

                enteredStopTime =
                    currentTime

                busSeenAtStop =
                    isBusAtStop(
                        location,
                        stop,
                        buses
                    )

                locationWhenEntered =
                    Location(location)

                lastLocation =
                    Location(location)

                lastLocationTime =
                    currentTime

                totalDistanceAfterStop =
                    0f

                consecutiveMovingSamples =
                    0

                boardingValidated =
                    false


                return BoardingResult.AtStop(

                    stop =
                        stop,

                    distanceMeters =
                        BusStopDetector
                            .calculateDistance(
                                location,
                                stop
                            )
                )
            }


            // ---------------------------------------------
            // SIGUE EN EL PARADERO
            // ---------------------------------------------

            return BoardingResult.WaitingAtStop(

                stop =
                    stop,

                timeAtStopMs =
                    currentTime -
                            enteredStopTime,

                distanceMeters =
                    BusStopDetector
                        .calculateDistance(
                            location,
                            stop
                        )
            )
        }


        // =====================================================
        // SALIÓ DEL PARADERO
        // =====================================================

        val previousStop =
            currentStop


        if (previousStop == null) {

            return BoardingResult.NoStop
        }


        // =====================================================
        // YA VALIDADO
        // =====================================================

        if (boardingValidated) {

            return BoardingResult.AlreadyValidated(
                previousStop
            )
        }


        // =====================================================
        // TIEMPO EN EL PARADERO
        // =====================================================

        val timeAtStop =
            currentTime -
                    enteredStopTime


        if (
            timeAtStop <
            MIN_TIME_AT_STOP_MS
        ) {

            resetSession()

            return BoardingResult.LeftTooQuickly(
                previousStop
            )
        }


        // =====================================================
        // SESIÓN EXPIRADA
        // =====================================================

        if (
            timeAtStop >
            MAX_SESSION_TIME_MS
        ) {

            resetSession()

            return BoardingResult.SessionExpired(
                previousStop
            )
        }


        // =====================================================
        // CRUZAR CON BUSES EN EL PARADERO / RUTA (ANTI-FRAUDE)
        // =====================================================
        val hasCompatibleBus = buses.isEmpty() ||
            isBusAtStop(location, previousStop, buses)

        // El abordaje es valido si un bus fue visto en el paradero
        // durante la espera, o si esta presente en el momento de salir.
        val busConfirmed = hasCompatibleBus || busSeenAtStop

        // =====================================================
        // VELOCIDAD (reportada o calculada entre lecturas)
        // =====================================================

        val reportedSpeedKmh =
            if (location.hasSpeed() && location.speed >= 0f) {
                location.speed * 3.6f
            } else {
                -1f
            }

        val derivedSpeedKmh =
            deriveSpeedKmh(
                location,
                currentTime
            )

        val speedKmh =
            maxOf(
                reportedSpeedKmh,
                derivedSpeedKmh
            )

        if (speedKmh < 0f) {

            // Sin dato de velocidad no se reinicia la sesion:
            // una lectura perdida no debe invalidar el abordaje.
            updateLastLocation(
                location,
                currentTime
            )

            return BoardingResult.SpeedUnavailable(
                previousStop
            )
        }

        // =====================================================
        // MOVIMIENTO INSUFICIENTE
        // =====================================================

        if (
            speedKmh <
            MIN_BUS_SPEED_KMH
        ) {

            consecutiveMovingSamples =
                0

            updateLastLocation(
                location,
                currentTime
            )

            return BoardingResult.MovingTooSlowly(

                stop =
                    previousStop,

                speedKmh =
                    speedKmh
            )
        }


        // =====================================================
        // DISTANCIA DESDE EL PARADERO
        // =====================================================

        val initialLocation =
            locationWhenEntered


        val distanceFromStop =
            if (
                initialLocation != null
            ) {

                initialLocation
                    .distanceTo(location)

            } else {

                0f
            }


        // =====================================================
        // DISTANCIA ENTRE LECTURAS
        // =====================================================

        val previousLocation =
            lastLocation


        if (
            previousLocation != null
        ) {

            val segmentDistance =
                previousLocation
                    .distanceTo(location)


            val segmentTime =
                currentTime -
                        lastLocationTime


            if (
                segmentTime <=
                MAX_LOCATION_GAP_MS &&
                segmentDistance <=
                100f
            ) {

                totalDistanceAfterStop +=
                    segmentDistance
            }
        }


        updateLastLocation(
            location,
            currentTime
        )


        // =====================================================
        // SESIÓN DEMASIADO LARGA
        // =====================================================

        if (
            distanceFromStop >
            MAX_SESSION_DISTANCE_METERS
        ) {

            resetSession()

            return BoardingResult.SessionExpired(
                previousStop
            )
        }


        // =====================================================
        // AÚN NO HAY DISTANCIA SUFICIENTE
        // =====================================================

        if (
            distanceFromStop <
            MIN_DISTANCE_AFTER_STOP_METERS
        ) {

            consecutiveMovingSamples++

            return BoardingResult.MovingButNotEnough(

                stop =
                    previousStop,

                speedKmh =
                    speedKmh,

                distanceMeters =
                    distanceFromStop,

                movingSamples =
                    consecutiveMovingSamples
            )
        }


        // =====================================================
        // SUMAR LECTURA DE MOVIMIENTO
        // =====================================================

        consecutiveMovingSamples++


        if (
            consecutiveMovingSamples <
            MIN_MOVING_SAMPLES
        ) {

            return BoardingResult.MovingForValidation(

                stop =
                    previousStop,

                speedKmh =
                    speedKmh,

                distanceMeters =
                    distanceFromStop,

                movingSamples =
                    consecutiveMovingSamples
            )
        }


        // =====================================================
        // VALIDACIÓN FINAL CON BUSES
        // =====================================================
        if (!busConfirmed && buses.isNotEmpty()) {
            consecutiveMovingSamples = (consecutiveMovingSamples - 1).coerceAtLeast(0)
            return BoardingResult.MovingButNotEnough(
                stop = previousStop,
                speedKmh = speedKmh,
                distanceMeters = distanceFromStop,
                movingSamples = consecutiveMovingSamples
            )
        }

        boardingValidated =
            true


        return BoardingResult.BoardingValidated(

            stop =
                previousStop,

            speedKmh =
                speedKmh,

            distanceMeters =
                distanceFromStop,

            totalDistanceMeters =
                totalDistanceAfterStop,

            movingSamples =
                consecutiveMovingSamples
        )
    }


    // =====================================================
    // CRUZE CON BUS EN EL PARADERO
    // =====================================================

    private fun isBusAtStop(
        location: Location,
        stop: BusStop,
        buses: List<Bus>
    ): Boolean {

        val userToStop = FloatArray(1)
        Location.distanceBetween(
            location.latitude,
            location.longitude,
            stop.latitude,
            stop.longitude,
            userToStop
        )

        return buses.any { bus ->
            val busToStop = FloatArray(1)
            Location.distanceBetween(
                bus.latitude,
                bus.longitude,
                stop.latitude,
                stop.longitude,
                busToStop
            )
            busToStop[0] <= stop.radiusMeters + BUS_NEAR_STOP_METERS ||
                userToStop[0] <= stop.radiusMeters + 80f
        }
    }


    // =========================================================
    // VELOCIDAD CALCULADA ENTRE LECTURAS
    // =========================================================

    private fun deriveSpeedKmh(
        location: Location,
        currentTime: Long
    ): Float {

        val previous =
            lastLocation ?: return -1f

        val elapsedSeconds =
            (currentTime - lastLocationTime) / 1000f

        if (
            elapsedSeconds <
            MIN_DERIVE_SPEED_GAP_S ||
            elapsedSeconds >
            MAX_DERIVE_SPEED_GAP_S
        ) {

            return -1f
        }

        val meters =
            previous.distanceTo(location)

        return (meters / elapsedSeconds) * 3.6f
    }


    // =========================================================
    // ACTUALIZAR ÚLTIMA UBICACIÓN
    // =========================================================

    private fun updateLastLocation(
        location: Location,
        currentTime: Long
    ) {

        lastLocation =
            Location(location)

        lastLocationTime =
            currentTime
    }


    // =========================================================
    // PRECISIÓN GPS
    // =========================================================

    private fun isLocationAccurate(
        location: Location
    ): Boolean {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            if (!location.hasAccuracy()) {
                return false
            }
        }

        return location.accuracy <=
                MAX_GPS_ACCURACY_METERS
    }


    // =========================================================
    // REINICIAR
    // =========================================================

    fun resetSession() {

        currentStop =
            null

        enteredStopTime =
            0L

        locationWhenEntered =
            null

        lastLocation =
            null

        lastLocationTime =
            0L

        totalDistanceAfterStop =
            0f

        consecutiveMovingSamples =
            0

        boardingValidated =
            false

        busSeenAtStop =
            false
    }


    // =========================================================
    // CONSULTAS
    // =========================================================

    fun getCurrentStop():
            BusStop? {

        return currentStop
    }


    fun getTimeAtStop():
            Long {

        if (
            enteredStopTime == 0L
        ) {

            return 0L
        }

        return System.currentTimeMillis() -
                enteredStopTime
    }


    fun getTotalDistanceAfterStop():
            Float {

        return totalDistanceAfterStop
    }


    fun getMovingSamples():
            Int {

        return consecutiveMovingSamples
    }
}


// =============================================================
// RESULTADOS
// =============================================================

sealed class BoardingResult {


    data class AtStop(
        val stop: BusStop,
        val distanceMeters: Float
    ) : BoardingResult()


    data class WaitingAtStop(
        val stop: BusStop,
        val timeAtStopMs: Long,
        val distanceMeters: Float
    ) : BoardingResult()


    data class MovingButNotEnough(
        val stop: BusStop,
        val speedKmh: Float,
        val distanceMeters: Float,
        val movingSamples: Int
    ) : BoardingResult()


    data class MovingForValidation(
        val stop: BusStop,
        val speedKmh: Float,
        val distanceMeters: Float,
        val movingSamples: Int
    ) : BoardingResult()


    data class BoardingValidated(
        val stop: BusStop,
        val speedKmh: Float,
        val distanceMeters: Float,
        val totalDistanceMeters: Float,
        val movingSamples: Int
    ) : BoardingResult()


    data class LeftTooQuickly(
        val stop: BusStop
    ) : BoardingResult()


    data class MovingTooSlowly(
        val stop: BusStop,
        val speedKmh: Float
    ) : BoardingResult()


    data class SpeedUnavailable(
        val stop: BusStop
    ) : BoardingResult()


    data class GpsAccuracyLow(
        val stop: BusStop,
        val accuracyMeters: Float
    ) : BoardingResult()


    data class AlreadyValidated(
        val stop: BusStop
    ) : BoardingResult()


    data class SessionExpired(
        val stop: BusStop
    ) : BoardingResult()


    data object NoStop : BoardingResult()
}