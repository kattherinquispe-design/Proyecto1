package com.ejemplo.smartbus.data

import android.location.Location
import com.ejemplo.smartbus.data.model.Bus
import com.ejemplo.smartbus.data.model.BusStop
import kotlin.math.ceil

object BusEtaCalculator {

    fun calculateDistance(
        bus: Bus,
        stop: BusStop
    ): Float {
        val result = FloatArray(1)
        Location.distanceBetween(
            bus.latitude,
            bus.longitude,
            stop.latitude,
            stop.longitude,
            result
        )
        return result[0]
    }

    fun calculateDistanceToLocation(
        bus: Bus,
        lat: Double,
        lng: Double
    ): Float {
        val result = FloatArray(1)
        Location.distanceBetween(
            bus.latitude,
            bus.longitude,
            lat,
            lng,
            result
        )
        return result[0]
    }

    /**
     * Calcula el tiempo estimado de llegada en minutos.
     * Si el bus esta detenido en un semaforo o paradero, usa velocidad urbana promedio (22 km/h).
     */
    fun calculateEtaMinutes(
        bus: Bus,
        stop: BusStop
    ): Int {
        val distanceMeters = calculateDistance(bus, stop)
        if (distanceMeters <= 35f) {
            return 0
        }

        val effectiveSpeed = if (bus.speedKmh > 5f) bus.speedKmh else 22f
        val distanceKm = distanceMeters / 1000f
        val hours = distanceKm / effectiveSpeed
        val minutes = hours * 60f

        return ceil(minutes.toDouble()).toInt().coerceAtLeast(1)
    }

    fun calculateEtaMinutesToLocation(
        bus: Bus,
        lat: Double,
        lng: Double
    ): Int {
        val distanceMeters = calculateDistanceToLocation(bus, lat, lng)
        if (distanceMeters <= 35f) {
            return 0
        }

        val effectiveSpeed = if (bus.speedKmh > 5f) bus.speedKmh else 22f
        val distanceKm = distanceMeters / 1000f
        val hours = distanceKm / effectiveSpeed
        val minutes = hours * 60f

        return ceil(minutes.toDouble()).toInt().coerceAtLeast(1)
    }

    fun findClosestBus(
        buses: List<Bus>,
        stop: BusStop
    ): Bus? {
        if (buses.isEmpty()) return null
        return buses.minByOrNull { calculateDistance(it, stop) }
    }

    fun findClosestBusForRoute(
        buses: List<Bus>,
        routeId: Int,
        stop: BusStop
    ): Bus? {
        val routeBuses = buses.filter { it.routeId == routeId }
        return findClosestBus(routeBuses, stop)
    }
}
