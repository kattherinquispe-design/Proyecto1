package com.ejemplo.smartbus.data

import com.ejemplo.smartbus.data.model.BusStop

object BusStopRepository {

    val stops = listOf(

        BusStop(
            id = 1,
            name = "Paradero Ciudad Blanca",
            latitude = -16.4090,
            longitude = -71.5375,
            radiusMeters = 50f
        ),

        BusStop(
            id = 2,
            name = "Paradero Paucarpata",
            latitude = -16.4230,
            longitude = -71.5000,
            radiusMeters = 50f
        ),

        BusStop(
            id = 3,
            name = "Paradero Centro",
            latitude = -16.3989,
            longitude = -71.5369,
            radiusMeters = 50f
        )
    )
}