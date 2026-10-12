package com.ejemplo.smartbus.data.model

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Float? = null,
    val timestamp: Long = System.currentTimeMillis()
)