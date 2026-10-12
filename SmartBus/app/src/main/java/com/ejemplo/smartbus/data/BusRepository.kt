package com.ejemplo.smartbus.data

import com.ejemplo.smartbus.data.model.Bus

object BusRepository {

    val buses: List<Bus> = listOf(

        // =====================================================
        // RUTA 101
        // =====================================================

        Bus(
            id = 1,
            code = "SB-101",
            routeId = 1,
            latitude = -16.4075,
            longitude = -71.5372,
            speedKmh = 14f,
            status = "En ruta",
            direction = "Ciudad Blanca → Paucarpata"
        ),

        Bus(
            id = 2,
            code = "SB-102",
            routeId = 1,
            latitude = -16.4060,
            longitude = -71.5230,
            speedKmh = 11f,
            status = "En ruta",
            direction = "Ciudad Blanca → Paucarpata"
        ),

        // =====================================================
        // RUTA 202
        // =====================================================

        Bus(
            id = 3,
            code = "SB-201",
            routeId = 2,
            latitude = -16.4140,
            longitude = -71.5120,
            speedKmh = 9f,
            status = "En ruta",
            direction = "Paucarpata → Centro"
        )
    )
}