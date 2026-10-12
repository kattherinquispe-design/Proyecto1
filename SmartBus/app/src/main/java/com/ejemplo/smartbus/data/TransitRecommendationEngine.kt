package com.ejemplo.smartbus.data

import android.location.Location
import com.ejemplo.smartbus.data.model.Bus
import com.ejemplo.smartbus.data.model.BusRoute
import com.ejemplo.smartbus.data.model.BusStop
import com.ejemplo.smartbus.data.model.TransitRecommendation
import java.text.Normalizer
import kotlin.math.ceil


object TransitRecommendationEngine {

    private const val DEFAULT_BUS_SPEED_KMH = 12f

    private const val WALKING_SPEED_METERS_PER_MINUTE = 75f


    // =========================================================
    // BUSCAR OPCIONES
    // =========================================================

    fun recommend(
        query: String,
        userLocation: Location?,
        routes: List<BusRoute>,
        stops: List<BusStop>,
        buses: List<Bus>
    ): List<TransitRecommendation> {

        val normalizedQuery =
            normalize(query)


        if (normalizedQuery.isBlank()) {
            return emptyList()
        }


        return routes.mapNotNull { route ->

            val routeStops =
                route.stopIds.mapNotNull { stopId ->

                    stops.firstOrNull {
                        it.id == stopId
                    }
                }


            if (routeStops.size < 2) {
                return@mapNotNull null
            }


            // -------------------------------------------------
            // ENCONTRAR DESTINO EN LA RUTA
            // -------------------------------------------------

            val destinationIndex =
                findDestinationIndex(
                    route = route,
                    routeStops = routeStops,
                    query = normalizedQuery
                )


            if (destinationIndex <= 0) {
                return@mapNotNull null
            }


            val destinationStop =
                routeStops[destinationIndex]


            // -------------------------------------------------
            // PARADEROS DONDE SE PUEDE ABORDAR
            // -------------------------------------------------

            val boardingStops =
                routeStops.take(destinationIndex)


            if (boardingStops.isEmpty()) {
                return@mapNotNull null
            }


            // -------------------------------------------------
            // PARADERO MÁS CONVENIENTE DESDE LA UBICACIÓN
            // -------------------------------------------------

            val boardingStop =
                if (userLocation != null) {

                    boardingStops.minByOrNull { stop ->

                        distanceToStop(
                            userLocation,
                            stop
                        )
                    }

                } else {

                    boardingStops.firstOrNull()
                }


                    ?: return@mapNotNull null


            val boardingIndex =
                routeStops.indexOfFirst {
                    it.id == boardingStop.id
                }


            // -------------------------------------------------
            // BUSES DE LA RUTA
            // -------------------------------------------------

            val routeBuses =
                buses.filter {
                    it.routeId == route.id
                }


            // -------------------------------------------------
            // BUS MÁS CERCANO AL PARADERO
            // -------------------------------------------------

            val closestBus =
                routeBuses.minByOrNull { bus ->

                    distanceBusToStop(
                        bus,
                        boardingStop
                    )
                }


            // -------------------------------------------------
            // DISTANCIA DEL USUARIO AL PARADERO
            // -------------------------------------------------

            val walkingDistance =
                userLocation?.let {

                    distanceToStop(
                        it,
                        boardingStop
                    )
                }


            // -------------------------------------------------
            // DISTANCIA DEL BUS AL PARADERO
            // -------------------------------------------------

            val busDistance =
                closestBus?.let {

                    distanceBusToStop(
                        it,
                        boardingStop
                    )
                }


            // -------------------------------------------------
            // VELOCIDAD DEL BUS
            // -------------------------------------------------

            val busSpeed =
                closestBus
                    ?.speedKmh
                    ?.takeIf { speed ->
                        speed > 0f
                    }
                    ?: DEFAULT_BUS_SPEED_KMH


            // -------------------------------------------------
            // ETA BUS → PARADERO
            // -------------------------------------------------

            val minutesToBoarding =
                closestBus?.let {

                    calculateMinutes(
                        busDistance ?: 0f,
                        busSpeed
                    )
                }


            // -------------------------------------------------
            // RECORRIDO DEL PARADERO → DESTINO
            // -------------------------------------------------

            val routeDistance =
                calculateRouteDistance(
                    routeStops,
                    boardingIndex,
                    destinationIndex
                )


            val minutesOnRoute =
                calculateMinutes(
                    routeDistance,
                    busSpeed
                )


            // -------------------------------------------------
            // CAMINATA AL PARADERO
            // -------------------------------------------------

            val walkingMinutes =
                walkingDistance?.let {

                    ceil(
                        it /
                                WALKING_SPEED_METERS_PER_MINUTE
                    )
                        .toInt()
                }


            // -------------------------------------------------
            // TIEMPO TOTAL
            // -------------------------------------------------

            val totalMinutes =
                when {

                    minutesToBoarding != null -> {

                        minutesToBoarding +
                                minutesOnRoute
                    }

                    else -> {

                        walkingMinutes?.plus(
                            minutesOnRoute
                        ) ?: minutesOnRoute
                    }
                }


            TransitRecommendation(

                route =
                    route,

                boardingStop =
                    boardingStop,

                destinationStop =
                    destinationStop,

                bus =
                    closestBus,

                walkingDistanceMeters =
                    walkingDistance,

                busDistanceMeters =
                    busDistance,

                minutesToBoarding =
                    minutesToBoarding,

                minutesOnRoute =
                    minutesOnRoute,

                totalMinutes =
                    totalMinutes,

                transfers =
                    0
            )
        }
            .sortedWith(
                compareBy<TransitRecommendation> {

                    it.totalMinutes
                        ?: Int.MAX_VALUE

                }.thenBy {

                    it.walkingDistanceMeters
                        ?: Float.MAX_VALUE
                }
            )
    }


    // =========================================================
    // ENCONTRAR DESTINO
    // =========================================================

    private fun findDestinationIndex(
        route: BusRoute,
        routeStops: List<BusStop>,
        query: String
    ): Int {

        // -----------------------------------------------------
        // COINCIDENCIA CON UN PARADERO
        // -----------------------------------------------------

        val stopIndex =
            routeStops.indexOfFirst { stop ->

                normalize(stop.name)
                    .contains(query)
            }


        if (stopIndex >= 0) {
            return stopIndex
        }


        // -----------------------------------------------------
        // DESTINO DE LA RUTA
        // -----------------------------------------------------

        val normalizedDestination =
            normalize(
                route.destination
            )


        if (
            normalizedDestination.contains(
                query
            ) ||
            query.contains(
                normalizedDestination
            )
        ) {

            val destinationStopIndex =
                routeStops.indexOfLast {

                    normalize(it.name)
                        .contains(
                            normalizedDestination
                        ) ||
                            normalizedDestination
                                .contains(
                                    normalize(
                                        it.name.removePrefix(
                                            "Paradero "
                                        )
                                    )
                                )
                }


            if (destinationStopIndex >= 0) {
                return destinationStopIndex
            }


            return routeStops.lastIndex
        }


        // -----------------------------------------------------
        // CÓDIGO DE RUTA
        // -----------------------------------------------------

        if (
            normalize(route.code) ==
            query
        ) {

            return routeStops.lastIndex
        }


        return -1
    }


    // =========================================================
    // DISTANCIA USUARIO → PARADERO
    // =========================================================

    private fun distanceToStop(
        userLocation: Location,
        stop: BusStop
    ): Float {

        val result =
            FloatArray(1)


        Location.distanceBetween(

            userLocation.latitude,
            userLocation.longitude,

            stop.latitude,
            stop.longitude,

            result
        )


        return result[0]
    }


    // =========================================================
    // DISTANCIA BUS → PARADERO
    // =========================================================

    private fun distanceBusToStop(
        bus: Bus,
        stop: BusStop
    ): Float {

        val result =
            FloatArray(1)


        Location.distanceBetween(

            bus.latitude,
            bus.longitude,

            stop.latitude,
            stop.longitude,

            result
        )


        return result[0]
    }


    // =========================================================
    // DISTANCIA DEL RECORRIDO
    // =========================================================

    private fun calculateRouteDistance(
        stops: List<BusStop>,
        startIndex: Int,
        endIndex: Int
    ): Float {

        if (
            startIndex < 0 ||
            endIndex >= stops.size ||
            startIndex >= endIndex
        ) {

            return 0f
        }


        var totalDistance =
            0f


        for (
        index in startIndex until endIndex
        ) {

            val start =
                stops[index]


            val end =
                stops[index + 1]


            val result =
                FloatArray(1)


            Location.distanceBetween(

                start.latitude,
                start.longitude,

                end.latitude,
                end.longitude,

                result
            )


            totalDistance +=
                result[0]
        }


        return totalDistance
    }


    // =========================================================
    // CALCULAR MINUTOS
    // =========================================================

    private fun calculateMinutes(
        distanceMeters: Float,
        speedKmh: Float
    ): Int {

        if (distanceMeters <= 0f) {
            return 0
        }


        if (speedKmh <= 0f) {
            return 0
        }


        val distanceKm =
            distanceMeters / 1000f


        val hours =
            distanceKm / speedKmh


        val minutes =
            hours * 60f


        return ceil(
            minutes.toDouble()
        )
            .toInt()
            .coerceAtLeast(1)
    }


    // =========================================================
    // NORMALIZAR TEXTO
    // =========================================================

    private fun normalize(
        value: String
    ): String {

        return Normalizer
            .normalize(
                value.trim().lowercase(),
                Normalizer.Form.NFD
            )
            .replace(
                "\\p{InCombiningDiacriticalMarks}+".toRegex(),
                ""
            )
    }
}