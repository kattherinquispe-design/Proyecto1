package com.ejemplo.smartbus.data.model

data class TransitRecommendation(
    val route: BusRoute,
    val boardingStop: BusStop,
    val destinationStop: BusStop,
    val bus: Bus?,
    val walkingDistanceMeters: Float? = null,
    val busDistanceMeters: Float? = null,
    val minutesToBoarding: Int? = null,
    val minutesOnRoute: Int? = null,
    val totalMinutes: Int? = null,
    val transfers: Int = 0,
    val etaMinutes: Int? = null,
    val isRecommended: Boolean = false
)