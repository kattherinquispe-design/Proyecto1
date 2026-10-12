package com.ejemplo.smartbus.data.model

import android.location.Location
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * Una ruta que sirve a un destino concreto, con los buses que la
 * recorren ahora mismo y el mas cercano a la parada donde te subes.
 */
data class RouteCandidate(
    val route: BusRoute,
    val boardingStop: BusStop,
    val distanceToStopMeters: Float,
    val buses: List<Bus>,
    val closestBus: Bus? = null,
    val closestBusDistanceMeters: Float? = null
) {

    /**
     * Minutos estimados de llegada del bus mas cercano.
     *
     * La distancia se mide siguiendo el trazado de la ruta, no en
     * linea recta: asi el tiempo refleja las calles por donde pasa el
     * vehiculo y no "vuela" por encima de las manzanas. Si no hay
     * trazado suficiente se cae a la linea recta.
     */
    val etaMinutes: Int?
        get() {
            val bus = closestBus ?: return null

            val meters = pathDistanceToStopMeters()
                ?: closestBusDistanceMeters
                ?: return null

            if (meters <= 40f) {
                return 0
            }

            val speed = bus.speedKmh.takeIf { it > 1f } ?: DEFAULT_BUS_SPEED_KMH
            val minutes = (meters / 1000f) / speed * 60f

            return ceil(minutes).toInt()
        }

    /**
     * Distancia que le falta al bus para llegar a la parada, medida a
     * lo largo del trazado. Devuelve null si la ruta no tiene puntos
     * suficientes para calcularla.
     */
    private fun pathDistanceToStopMeters(): Float? {

        val points = route.points

        if (points.size < 2) {
            return null
        }

        val bus = closestBus ?: return null

        val busOffset = offsetAlongPath(
            points = points,
            latitude = bus.latitude,
            longitude = bus.longitude
        ) ?: return null

        val stopOffset = offsetAlongPath(
            points = points,
            latitude = boardingStop.latitude,
            longitude = boardingStop.longitude
        ) ?: return null

        val total = totalPathMeters(points)

        if (total <= 0f) {
            return null
        }

        var distance = stopOffset - busOffset

        // Si el bus ya paso la parada, le toca la vuelta completa.
        if (distance < 0f) {
            distance += total
        }

        return distance
    }

    /**
     * Metro de recorrido donde cae un punto sobre el trazado: se busca
     * el segmento mas cercano y se mide hasta la proyeccion del punto.
     */
    private fun offsetAlongPath(
        points: List<GeoPoint>,
        latitude: Double,
        longitude: Double
    ): Float? {

        if (points.size < 2) {
            return null
        }

        val metersPerDegLat = 111_320.0
        val metersPerDegLng = 111_320.0 * cos(Math.toRadians(latitude))

        var accumulated = 0f
        var bestDistance = Float.MAX_VALUE
        var bestOffset = 0f

        for (index in 0 until points.size - 1) {

            val a = points[index]
            val b = points[index + 1]

            val segmentMeters = distanceMeters(
                a.latitude, a.longitude,
                b.latitude, b.longitude
            )

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

            if (distance < bestDistance) {
                bestDistance = distance
                bestOffset = accumulated + (segmentMeters * t).toFloat()
            }

            accumulated += segmentMeters
        }

        return bestOffset
    }

    private fun totalPathMeters(points: List<GeoPoint>): Float {

        var total = 0f

        for (index in 0 until points.size - 1) {

            val a = points[index]
            val b = points[index + 1]

            total += distanceMeters(
                a.latitude, a.longitude,
                b.latitude, b.longitude
            )
        }

        return total
    }

    private fun distanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Float {

        val result = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, result)
        return result[0]
    }

    companion object {
        const val DEFAULT_BUS_SPEED_KMH = 18f
    }
}
