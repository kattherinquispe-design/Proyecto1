package com.ejemplo.smartbus.data.model

data class RouteSchedule(
    val routeId: Int,
    val startTimeMinutes: Int = 0,
    val endTimeMinutes: Int = 1440,
    val frequencyMinutes: Int = 10,
    val status: String = "normal",
    val active: Boolean = true
)