package com.ejemplo.smartbus.data.model

data class BusStop(
    val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = 80f,
    val isActive: Boolean = true,
    val hasAccessibility: Boolean = false,
    val routes: List<Int> = emptyList()
)