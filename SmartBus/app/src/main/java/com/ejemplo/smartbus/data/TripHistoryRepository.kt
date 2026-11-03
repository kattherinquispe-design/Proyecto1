package com.ejemplo.smartbus.data

import android.content.Context
import android.content.SharedPreferences
import com.ejemplo.smartbus.data.model.BusRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class FrequentRoute(
    val routeId: Int,
    val code: String,
    val name: String,
    val origin: String,
    val destination: String,
    val tripCount: Int,
    val lastTakenAt: Long
)

/**
 * Repositorio de historial de viajes y buses tomados por el pasajero.
 * Mantiene un registro de las rutas utilizadas para mostrar en "Próximo bus"
 * los buses más frecuentes o recientemente usados.
 */
class TripHistoryRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("mistigo_trip_history", Context.MODE_PRIVATE)

    private val _frequentRoutes = MutableStateFlow<List<FrequentRoute>>(loadFromPrefs())
    val frequentRoutes: StateFlow<List<FrequentRoute>> = _frequentRoutes.asStateFlow()

    /**
     * Registra que el pasajero ha tomado o seleccionado una ruta de bus.
     */
    fun recordTrip(route: BusRoute) {
        val current = _frequentRoutes.value.toMutableList()
        val index = current.indexOfFirst { it.routeId == route.id || it.code.equals(route.code, ignoreCase = true) }

        if (index >= 0) {
            val existing = current[index]
            current[index] = existing.copy(
                tripCount = existing.tripCount + 1,
                lastTakenAt = System.currentTimeMillis()
            )
        } else {
            current.add(
                FrequentRoute(
                    routeId = route.id,
                    code = route.code,
                    name = route.name,
                    origin = route.origin,
                    destination = route.destination,
                    tripCount = 1,
                    lastTakenAt = System.currentTimeMillis()
                )
            )
        }

        // Ordenar por frecuencia descendente, y luego por el más reciente
        current.sortWith(
            compareByDescending<FrequentRoute> { it.tripCount }
                .thenByDescending { it.lastTakenAt }
        )

        _frequentRoutes.value = current
        saveToPrefs(current)
    }

    private fun saveToPrefs(list: List<FrequentRoute>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("routeId", item.routeId)
                put("code", item.code)
                put("name", item.name)
                put("origin", item.origin)
                put("destination", item.destination)
                put("tripCount", item.tripCount)
                put("lastTakenAt", item.lastTakenAt)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    private fun loadFromPrefs(): List<FrequentRoute> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        val list = mutableListOf<FrequentRoute>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    FrequentRoute(
                        routeId = obj.getInt("routeId"),
                        code = obj.getString("code"),
                        name = obj.getString("name"),
                        origin = obj.getString("origin"),
                        destination = obj.getString("destination"),
                        tripCount = obj.getInt("tripCount"),
                        lastTakenAt = obj.getLong("lastTakenAt")
                    )
                )
            }
        } catch (_: Exception) {
        }
        return list.sortedWith(
            compareByDescending<FrequentRoute> { it.tripCount }
                .thenByDescending { it.lastTakenAt }
        )
    }

    companion object {
        private const val KEY_HISTORY = "trips_json"

        @Volatile
        private var instance: TripHistoryRepository? = null

        fun get(context: Context): TripHistoryRepository =
            instance ?: synchronized(this) {
                instance ?: TripHistoryRepository(context).also { instance = it }
            }
    }
}
