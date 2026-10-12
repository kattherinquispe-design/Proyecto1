package com.ejemplo.smartbus.data.model

data class Report(
    val id: Long,
    val userId: String = "",
    val type: String,
    val description: String,
    val busId: Int? = null,
    val routeId: Int? = null,
    val stopId: Int? = null,
    val stopName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val photoUri: String? = null,
    val evidenceUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = ReportStatus.ENVIADO,
    val rating: Int? = null
)

object ReportStatus {
    const val ENVIADO = "enviado"
    const val RECIBIDO = "recibido"
    const val ATENDIDO = "atendido"
    const val CERRADO = "cerrado"
}

object ReportType {
    const val RETRASO = "Retraso"
    const val DESVIO_RUTA = "Desvío de ruta"
    const val BUS_LLENO = "Bus lleno"
    const val PROBLEMA_SERVICIO = "Problema del servicio"
    const val BUS_NO_PASO = "Bus que no pasó"
    const val PARADERO_INCORRECTO = "Paradero incorrecto"
    const val RUTA_INCORRECTA = "Ruta incorrecta"
    const val GPS_INCORRECTO = "GPS incorrecto"
    const val CAMBIO_RUTA = "Cambios de ruta"
    const val OTRO = "Otro"
}