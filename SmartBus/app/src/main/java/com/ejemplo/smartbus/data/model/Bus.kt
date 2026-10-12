package com.ejemplo.smartbus.data.model

data class Bus(
    val id: Int,
    val code: String,
    val routeId: Int,
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Float,
    val status: String = "En ruta",
    val direction: String = "",
    val lastUpdate: Long = System.currentTimeMillis(),
    val occupancy: Int? = null,
    val isOutOfRoute: Boolean = false,
    val gpsAccuracy: Float? = null,
    /**
     * Ultimos 4 de la placa. Es como el pasajero reconoce el
     * vehiculo: la placa completa no se muestra en pantalla.
     */
    val plateCount: Int = 0,
    /** Nombre del conductor si esta unidad tiene uno asignado. */
    val driverName: String = "",
    /** Puestos del vehiculo, 0 si no se sabe. */
    val capacity: Int = 0,
    /**
     * true si la posicion viene del simulador y no de un conductor
     * real. La interfaz lo dice, para no vender una demo como si
     * fuera un vehiculo en servicio de verdad.
     */
    val isMock: Boolean = true
)