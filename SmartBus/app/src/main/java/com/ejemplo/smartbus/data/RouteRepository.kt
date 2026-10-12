package com.ejemplo.smartbus.data

import com.ejemplo.smartbus.data.model.BusRoute
import com.ejemplo.smartbus.data.model.GeoPoint


object RouteRepository {

    val routes: List<BusRoute> = listOf(

        // =====================================================
        // RUTA 101
        // =====================================================

        BusRoute(
            id = 1,
            code = "101",
            name = "Ciudad Blanca - Paucarpata",
            origin = "Ciudad Blanca",
            destination = "Paucarpata",

            stopIds = listOf(
                1,
                3,
                2
            ),

            points = listOf(

                GeoPoint(
                    latitude = -16.4090,
                    longitude = -71.5375
                ),

                GeoPoint(
                    latitude = -16.4055,
                    longitude = -71.5370
                ),

                GeoPoint(
                    latitude = -16.3989,
                    longitude = -71.5369
                ),

                GeoPoint(
                    latitude = -16.4070,
                    longitude = -71.5200
                ),

                GeoPoint(
                    latitude = -16.4230,
                    longitude = -71.5000
                )
            )
        ),

        // =====================================================
        // RUTA 202
        // =====================================================

        BusRoute(
            id = 2,
            code = "202",
            name = "Paucarpata - Centro",
            origin = "Paucarpata",
            destination = "Centro",

            stopIds = listOf(
                2,
                3
            ),

            points = listOf(

                GeoPoint(
                    latitude = -16.4230,
                    longitude = -71.5000
                ),

                GeoPoint(
                    latitude = -16.4170,
                    longitude = -71.5090
                ),

                GeoPoint(
                    latitude = -16.4070,
                    longitude = -71.5200
                ),

                GeoPoint(
                    latitude = -16.3989,
                    longitude = -71.5369
                )
            )
        )
    )
}