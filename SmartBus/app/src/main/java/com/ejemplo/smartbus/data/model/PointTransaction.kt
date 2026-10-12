package com.ejemplo.smartbus.data.model

data class PointTransaction(
    val id: Long,
    val points: Int,
    val stopId: Int = 0,
    val stopName: String = "",
    val routeId: Int? = null,
    val busId: Int? = null,
    val reason: String = "",
    val evidence: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)