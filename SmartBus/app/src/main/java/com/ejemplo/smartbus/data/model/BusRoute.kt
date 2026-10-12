package com.ejemplo.smartbus.data.model

data class BusRoute(
    val id: Int,
    val code: String,
    val name: String,
    val origin: String,
    val destination: String,
    val stopIds: List<Int> = emptyList(),
    val points: List<GeoPoint> = emptyList(),
    val frequencyMinutes: Int = 10,
    val isActive: Boolean = true,
    val hasDeviation: Boolean = false,
    val deviationInfo: String? = null
)