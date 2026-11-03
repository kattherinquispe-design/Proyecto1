package com.ejemplo.smartbus.data

import com.ejemplo.smartbus.data.model.Bus

/**
 * Sin buses simulados: la aplicacion muestra unicamente unidades reales
 * transmitiendo en vivo desde Conductores registrados.
 */
object BusRepository {
    val buses: List<Bus> = emptyList()
}