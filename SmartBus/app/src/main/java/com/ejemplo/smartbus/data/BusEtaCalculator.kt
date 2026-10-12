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


    fun calculateEtaMinutes(
        bus: Bus,
        stop: BusStop
    ): Int? {

        if (bus.speedKmh <= 0f) {
            return null
        }


        val distanceMeters =
            calculateDistance(
                bus,
                stop
            )


        if (distanceMeters <= 30f) {
            return 0
        }


        val distanceKm =
            distanceMeters / 1000f


        val hours =
            distanceKm / bus.speedKmh.coerceAtLeast(1f)


        val minutes =
            hours * 60f


        return ceil(
            minutes
                .toDouble()
        ).toInt()
            .coerceAtLeast(1)
    }


    fun findClosestBus(
        buses: List<Bus>,
        stop: BusStop
    ): Bus? {

        if (buses.isEmpty()) {
            return null
        }


        return buses.minByOrNull {

            calculateDistance(
                it,
                stop
            )
        }
    }


    fun findClosestBusForRoute(
        buses: List<Bus>,
        routeId: Int,
        stop: BusStop
    ): Bus? {

        val routeBuses =
            buses.filter {
                it.routeId == routeId
            }


        return findClosestBus(
            routeBuses,
            stop
        )
    }
}