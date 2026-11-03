package com.ejemplo.smartbus.data

import android.util.Log
import com.ejemplo.smartbus.data.model.Bus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Repositorio de buses y conductores en vivo (100% real, sin simulacion).
 * Escucha en tiempo real la coleccion 'vehiculos_en_vivo' de Firebase Firestore
 * donde los conductores transmiten su GPS y ruta, y los distribuye a los pasajeros.
 */
object BusLiveRepository {

    private var realDrivers: List<Bus> = emptyList()

    private val _buses = MutableStateFlow<List<Bus>>(emptyList())
    val buses: StateFlow<List<Bus>> = _buses

    init {
        startFirestoreListener()
    }

    private fun startFirestoreListener() {
        try {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("vehiculos_en_vivo")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w("BusLiveRepository", "Error escuchando vehiculos_en_vivo: ${error.message}")
                        return@addSnapshotListener
                    }

                    val list = snapshot?.documents?.mapNotNull { doc ->
                        val isMock = doc.getBoolean("isMock") ?: false
                        if (isMock) return@mapNotNull null

                        val lat = doc.getDouble("latitude") ?: doc.getDouble("latitud") ?: return@mapNotNull null
                        val lng = doc.getDouble("longitude") ?: doc.getDouble("longitud") ?: return@mapNotNull null
                        val routeId = doc.getLong("routeId")?.toInt() ?: 0
                        val speed = (doc.getDouble("speedKmh") ?: 0.0).toFloat()
                        val plate = doc.getString("plate").orEmpty()
                        val driverName = doc.getString("driverName").orEmpty()
                        val status = doc.getString("status") ?: "EN_SERVICIO"
                        val tripType = doc.getString("tripType") ?: "SUBIDA"
                        val lastUpdate = doc.getLong("lastUpdate") ?: System.currentTimeMillis()
                        val capacity = doc.getLong("capacity")?.toInt() ?: 0

                        Bus(
                            id = Math.abs(doc.id.hashCode()),
                            code = if (plate.isNotBlank()) "Bus ${plate.uppercase()}" else "Bus Conductor",
                            routeId = routeId,
                            latitude = lat,
                            longitude = lng,
                            speedKmh = speed,
                            tripType = tripType,
                            driverName = driverName,
                            plateCount = plate.filter { it.isDigit() }.takeLast(4).toIntOrNull() ?: 0,
                            capacity = capacity,
                            isMock = false,
                            status = status,
                            lastUpdate = lastUpdate
                        )
                    } ?: emptyList()

                    realDrivers = list
                    _buses.value = realDrivers
                }
        } catch (e: Exception) {
            Log.w("BusLiveRepository", "Error al iniciar Firestore listener: ${e.message}")
        }
    }

    fun advanceSimulation() {
        // Sin simulacion: todos los buses provienen de conductores reales.
    }

    fun getBusById(busId: Int): Bus? =
        _buses.value.firstOrNull { it.id == busId }
}
