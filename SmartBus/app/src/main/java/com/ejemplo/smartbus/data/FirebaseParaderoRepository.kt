package com.ejemplo.smartbus.data

import android.content.Context
import android.location.Location
import android.util.Log
import com.ejemplo.smartbus.data.model.BusStop
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs

/**
 * Repositorio de paraderos conectado a Firebase Firestore (colección "paraderos").
 * Permite obtener los paraderos reales y calcular la distancia exacta hacia la
 * ubicación GPS en tiempo real del pasajero.
 */
object FirebaseParaderoRepository {

    private const val TAG = "MistiGoParaderos"
    private const val COLLECTION_PARADEROS = "paraderos"

    private val _stops = MutableStateFlow<List<BusStop>>(BusStopRepository.stops)
    val stops: StateFlow<List<BusStop>> = _stops.asStateFlow()

    private var hasLoaded = false

    private suspend fun <T> Task<T>.awaitTask(): T =
        suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { result ->
                if (continuation.isActive) continuation.resume(result)
            }
            addOnFailureListener { exception ->
                if (continuation.isActive) continuation.resumeWithException(exception)
            }
        }

    /**
     * Carga los paraderos desde Firebase Firestore.
     */
    suspend fun loadStops(context: Context? = null): List<BusStop> =
        withContext(Dispatchers.IO) {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val snapshot: QuerySnapshot = firestore
                    .collection(COLLECTION_PARADEROS)
                    .get()
                    .awaitTask()

                if (!snapshot.isEmpty) {
                    val loaded = snapshot.documents.mapNotNull { doc ->
                        val nombre = doc.getString("nombre") ?: doc.getString("name") ?: return@mapNotNull null
                        val lat = doc.getDouble("latitud") ?: doc.getDouble("latitude") ?: 0.0
                        val lon = doc.getDouble("longitud") ?: doc.getDouble("longitude") ?: 0.0
                        val radio = doc.getDouble("radioMetros")?.toFloat() ?: 50f
                        if (lat == 0.0 && lon == 0.0) return@mapNotNull null

                        BusStop(
                            id = abs(doc.id.hashCode()),
                            name = nombre,
                            latitude = lat,
                            longitude = lon,
                            radiusMeters = radio,
                            isActive = true
                        )
                    }

                    if (loaded.isNotEmpty()) {
                        _stops.value = loaded
                        hasLoaded = true
                        Log.i(TAG, "Cargados ${loaded.size} paraderos desde Firebase Firestore.")
                        return@withContext loaded
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo leer paraderos de Firestore: ${e.message}")
            }

            return@withContext _stops.value
        }

    /**
     * Obtiene los paraderos más cercanos a una coordenada de usuario dada,
     * ordenados estrictamente por distancia ascendente.
     */
    fun getStopsNear(
        userLat: Double?,
        userLng: Double?,
        limit: Int = 5
    ): List<Pair<BusStop, Float>> {
        val current = _stops.value
        if (userLat == null || userLng == null || current.isEmpty()) {
            return current.take(limit).map { it to 0f }
        }

        val results = FloatArray(1)
        return current.map { stop ->
            Location.distanceBetween(userLat, userLng, stop.latitude, stop.longitude, results)
            stop to results[0]
        }
            .sortedBy { it.second }
            .take(limit)
    }
}
