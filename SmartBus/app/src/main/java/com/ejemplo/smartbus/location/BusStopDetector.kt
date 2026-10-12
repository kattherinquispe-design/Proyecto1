package com.ejemplo.smartbus.location

import android.location.Location
import com.ejemplo.smartbus.data.model.BusStop

object BusStopDetector {

    // Velocidad mínima que consideraremos compatible
    // con el desplazamiento de un bus.
    // 8 km/h = 2.22 m/s
    private const val MIN_BUS_SPEED_KMH = 8f

    // Tiempo mínimo que debe permanecer dentro del paradero
    // antes de poder considerar un posible abordaje.
    private const val MIN_TIME_AT_STOP_MS = 15_000L

    // Distancia mínima que debe recorrer después de salir
    // del paradero para validar el desplazamiento.
    private const val MIN_DISTANCE_AFTER_STOP_METERS = 100f

    /**
     * Calcula la distancia entre el usuario y un paradero.
     */
    fun calculateDistance(
        userLocation: Location,
        busStop: BusStop
    ): Float {

        val result = FloatArray(1)

        Location.distanceBetween(
            userLocation.latitude,
            userLocation.longitude,
            busStop.latitude,
            busStop.longitude,
            result
        )

        return result[0]
    }

    /**
     * Busca el paradero más cercano.
     */
    fun findNearestStop(
        userLocation: Location,
        stops: List<BusStop>
    ): BusStop? {

        if (stops.isEmpty()) {
            return null
        }

        return stops.minByOrNull { stop ->
            calculateDistance(userLocation, stop)
        }
    }

    /**
     * Comprueba si el usuario está dentro
     * del radio del paradero.
     */
    fun isInsideStop(
        userLocation: Location,
        busStop: BusStop
    ): Boolean {

        val distance = calculateDistance(
            userLocation,
            busStop
        )

        return distance <= busStop.radiusMeters
    }

    /**
     * Busca si el usuario está actualmente
     * dentro de algún paradero.
     */
    fun findCurrentStop(
        userLocation: Location,
        stops: List<BusStop>
    ): BusStop? {

        return stops.firstOrNull { stop ->
            isInsideStop(
                userLocation,
                stop
            )
        }
    }

    /**
     * Convierte la velocidad de m/s a km/h.
     */
    fun metersPerSecondToKmH(
        speedMetersPerSecond: Float
    ): Float {

        return speedMetersPerSecond * 3.6f
    }

    /**
     * Comprueba si la velocidad actual
     * es suficiente para considerar que
     * el usuario está desplazándose en un vehículo.
     */
    fun isVehicleSpeed(
        location: Location
    ): Boolean {

        if (!location.hasSpeed()) {
            return false
        }

        val speedKmh = metersPerSecondToKmH(
            location.speed
        )

        return speedKmh >= MIN_BUS_SPEED_KMH
    }

    /**
     * Tiempo mínimo que debe permanecer
     * en el paradero.
     */
    fun getMinimumTimeAtStop(): Long {
        return MIN_TIME_AT_STOP_MS
    }

    /**
     * Distancia mínima que debe recorrer
     * después de abandonar el paradero.
     */
    fun getMinimumDistanceAfterStop(): Float {
        return MIN_DISTANCE_AFTER_STOP_METERS
    }
}