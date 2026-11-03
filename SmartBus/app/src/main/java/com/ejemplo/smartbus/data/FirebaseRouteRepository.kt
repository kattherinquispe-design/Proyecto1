package com.ejemplo.smartbus.data

import android.location.Location
import android.util.Log
import com.ejemplo.smartbus.data.model.BusRoute
import com.ejemplo.smartbus.data.model.GeoPoint
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs

/**
 * Repositorio de rutas conectadas en tiempo real a Firebase Firestore.
 * Lee la colección "rutas" donde se encuentran los recorridos reales de MistiGo,
 * organizando cada ruta con su trazado de SUBIDA (Ida) y BAJADA (Vuelta / Retorno).
 */
object FirebaseRouteRepository {

    private const val TAG = "MistiGoFirebase"
    private const val COLLECTION_RUTAS = "rutas"

    private val _routes = MutableStateFlow<List<BusRoute>>(emptyList())
    val routes: StateFlow<List<BusRoute>> = _routes.asStateFlow()

    private var hasLoaded = false

    /**
     * Espera asíncrona segura para tareas de Google Play Services / Firebase.
     */
    private suspend fun <T> Task<T>.awaitTask(): T =
        suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { result ->
                if (continuation.isActive) {
                    continuation.resume(result)
                }
            }
            addOnFailureListener { exception ->
                if (continuation.isActive) {
                    continuation.resumeWithException(exception)
                }
            }
        }

    /**
     * Carga todas las rutas desde Firebase Firestore de la colección "rutas".
     */
    suspend fun loadRoutes(forceRefresh: Boolean = false): List<BusRoute> =
        withContext(Dispatchers.IO) {
            if (hasLoaded && !forceRefresh && _routes.value.isNotEmpty()) {
                return@withContext _routes.value
            }

            try {
                val firestore = FirebaseFirestore.getInstance()
                Log.d(TAG, "Conectando a Firebase Firestore coleccion '$COLLECTION_RUTAS'...")

                val snapshot: QuerySnapshot = firestore
                    .collection(COLLECTION_RUTAS)
                    .get()
                    .awaitTask()

                Log.d(TAG, "Firebase respondió con ${snapshot.documents.size} documentos de rutas.")

                val consolidated = parseAndConsolidateRoutes(snapshot)
                if (consolidated.isNotEmpty()) {
                    _routes.value = consolidated
                    hasLoaded = true
                    Log.i(TAG, "Rutas consolidadas con éxito: ${consolidated.size} rutas reales cargadas.")
                    return@withContext consolidated
                } else {
                    Log.w(TAG, "No se encontraron rutas validas en Firestore, usando fallback local.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al conectar con Firebase Firestore: ${e.message}", e)
            }

            // Fallback a las rutas de muestra si no hay conexion
            if (_routes.value.isEmpty()) {
                val fallback = RouteRepository.routes.map { r ->
                    r.copy(
                        subidaPoints = r.points,
                        bajadaPoints = r.points.reversed()
                    )
                }
                _routes.value = fallback
            }

            return@withContext _routes.value
        }

    /**
     * Busca rutas por texto (código de ruta, nombre, origen o destino).
     */
    suspend fun searchRoutes(query: String): List<BusRoute> {
        val currentRoutes = if (_routes.value.isEmpty()) loadRoutes() else _routes.value
        val cleanQuery = query.trim().lowercase()

        if (cleanQuery.isBlank()) {
            return currentRoutes
        }

        return currentRoutes.filter { route ->
            route.code.lowercase().contains(cleanQuery) ||
                route.name.lowercase().contains(cleanQuery) ||
                route.origin.lowercase().contains(cleanQuery) ||
                route.destination.lowercase().contains(cleanQuery)
        }
    }

    /**
     * Encuentra rutas cuyo recorrido pase cerca de unas coordenadas dadas.
     */
    fun findRoutesNear(
        lat: Double,
        lng: Double,
        radiusMeters: Float = 1500f
    ): List<BusRoute> {
        val current = _routes.value
        if (current.isEmpty()) return emptyList()

        val results = FloatArray(1)
        return current.filter { route ->
            val allPoints = (route.subidaPoints + route.bajadaPoints).ifEmpty { route.points }
            allPoints.any { pt ->
                Location.distanceBetween(lat, lng, pt.latitude, pt.longitude, results)
                results[0] <= radiusMeters
            }
        }
    }

    /**
     * Parsea la cadena JSON de coordenadas en formato [[lng, lat, alt], ...]
     */
    fun parseCoordinates(rawJson: String?): List<GeoPoint> {
        if (rawJson.isNullOrBlank()) return emptyList()
        val points = ArrayList<GeoPoint>()
        try {
            val jsonArray = JSONArray(rawJson)
            for (i in 0 until jsonArray.length()) {
                val pointArray = jsonArray.optJSONArray(i) ?: continue
                val lng = pointArray.optDouble(0)
                val lat = pointArray.optDouble(1)
                if (!lat.isNaN() && !lng.isNaN() && (lat != 0.0 || lng != 0.0)) {
                    points.add(GeoPoint(latitude = lat, longitude = lng))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando coordenadas: ${e.message}")
        }
        return points
    }

    private class RawRouteDoc(
        val docId: String,
        val rawName: String,
        val description: String,
        val points: List<GeoPoint>,
        val isBajada: Boolean
    )

    private fun parseAndConsolidateRoutes(snapshot: QuerySnapshot): List<BusRoute> {
        val rawDocs = mutableListOf<RawRouteDoc>()

        for (doc in snapshot.documents) {
            val rawName = doc.getString("nombreRuta") ?: ""
            val props = doc.get("propiedades") as? Map<*, *>
            val description = (props?.get("description") as? String) ?: ""
            val coordStr = doc.getString("coordenadas")
            val points = parseCoordinates(coordStr)

            if (points.size < 2) continue

            val isBajada = rawName.contains("vuelta", ignoreCase = true) ||
                rawName.contains("retorno", ignoreCase = true) ||
                rawName.contains("bajada", ignoreCase = true) ||
                description.contains("vuelta", ignoreCase = true) ||
                description.contains("retorno", ignoreCase = true) ||
                description.contains("bajada", ignoreCase = true)

            rawDocs.add(
                RawRouteDoc(
                    docId = doc.id,
                    rawName = rawName,
                    description = description,
                    points = points,
                    isBajada = isBajada
                )
            )
        }

        // Agrupamos por nombre base limpio de la ruta
        val groups = rawDocs.groupBy { cleanRouteBase(it.rawName, it.description) }
        val consolidated = mutableListOf<BusRoute>()

        var index = 1
        for ((baseName, docs) in groups) {
            if (baseName.isBlank() || baseName.equals("Ruta Urbana", ignoreCase = true) && docs.size > 2) {
                // Si es un documento genérico sin nombre específico, generamos rutas con sus descripciones
                for (doc in docs) {
                    val (orig, dest) = extractOriginDestination(doc.rawName, doc.description)
                    val code = "R-${index}"
                    val subida = if (!doc.isBajada) doc.points else doc.points.reversed()
                    val bajada = if (doc.isBajada) doc.points else doc.points.reversed()
                    consolidated.add(
                        BusRoute(
                            id = Math.abs(doc.docId.hashCode()),
                            code = code,
                            name = "$orig - $dest",
                            origin = orig,
                            destination = dest,
                            points = subida,
                            subidaPoints = subida,
                            bajadaPoints = bajada,
                            frequencyMinutes = 10,
                            isActive = true,
                            tripType = if (doc.isBajada) "BAJADA" else "SUBIDA"
                        )
                    )
                    index++
                }
                continue
            }

            // Seleccionar los mejores trazados para Subida y Bajada
            val subidaDocs = docs.filter { !it.isBajada }.sortedByDescending { it.points.size }
            val bajadaDocs = docs.filter { it.isBajada }.sortedByDescending { it.points.size }

            val bestSubidaPoints = subidaDocs.firstOrNull()?.points ?: emptyList()
            val bestBajadaPoints = bajadaDocs.firstOrNull()?.points ?: emptyList()

            val finalSubida = bestSubidaPoints.ifEmpty { bestBajadaPoints.reversed() }
            val finalBajada = bestBajadaPoints.ifEmpty { bestSubidaPoints.reversed() }

            val representativeDoc = docs.first()
            val (origin, destination) = extractOriginDestination(representativeDoc.rawName, representativeDoc.description)
            val code = extractRouteCode(baseName, index)

            val routeId = Math.abs(baseName.hashCode())
            consolidated.add(
                BusRoute(
                    id = if (routeId != 0) routeId else index,
                    code = code,
                    name = baseName,
                    origin = origin,
                    destination = destination,
                    points = finalSubida,
                    subidaPoints = finalSubida,
                    bajadaPoints = finalBajada,
                    frequencyMinutes = 10,
                    isActive = true,
                    tripType = "SUBIDA"
                )
            )
            index++
        }

        return consolidated.sortedBy { it.code }
    }

    private fun cleanRouteBase(rawName: String, desc: String): String {
        val trimmed = rawName.trim()
        if (trimmed.equals("IDA", ignoreCase = true) ||
            trimmed.equals("VUELTA", ignoreCase = true) ||
            trimmed.equals("RETORNO", ignoreCase = true) ||
            trimmed.isBlank()
        ) {
            if (desc.isNotBlank()) {
                val desdeHasta = desc.replace("(?i)desde:".toRegex(), "").split("(?i)hasta:".toRegex())
                if (desdeHasta.size == 2) {
                    val o = desdeHasta[0].trim().replace("\n", " ").replace("\\s+".toRegex(), " ")
                    val d = desdeHasta[1].trim().replace("\n", " ").replace("\\s+".toRegex(), " ")
                    return "$o - $d"
                }
                return desc.lines().firstOrNull()?.trim() ?: "Ruta Urbana"
            }
            return "Ruta Urbana"
        }

        return trimmed
            .replace("(?i)\\s*\\(?\\s*(ida|vuelta|retorno|subida|bajada)\\s*\\)?\\s*".toRegex(), " ")
            .replace("[-\\s]+".toRegex(), " ")
            .trim()
    }

    private fun extractRouteCode(baseName: String, fallbackIndex: Int): String {
        val match = "(?i)ruta\\s+([a-z0-9]+)".toRegex().find(baseName)
        if (match != null) {
            return match.groupValues[1].uppercase()
        }
        val firstWord = baseName.split(" ").firstOrNull()
        if (!firstWord.isNullOrBlank() && firstWord.length <= 6) {
            return firstWord.uppercase()
        }
        return "R$fallbackIndex"
    }

    private fun extractOriginDestination(rawName: String, desc: String): Pair<String, String> {
        if (desc.isNotBlank()) {
            val desdeHasta = desc.replace("(?i)desde:".toRegex(), "").split("(?i)hasta:".toRegex())
            if (desdeHasta.size >= 2) {
                val orig = desdeHasta[0].trim().replace("\n", " ").take(30)
                val dest = desdeHasta[1].trim().replace("\n", " ").take(30)
                return Pair(orig, dest)
            }
            val dashSplit = desc.split("-")
            if (dashSplit.size >= 2) {
                return Pair(dashSplit[0].trim().take(30), dashSplit[1].trim().take(30))
            }
        }
        val nameSplit = rawName.split("-")
        if (nameSplit.size >= 2) {
            return Pair(nameSplit[0].trim().take(30), nameSplit[1].trim().take(30))
        }
        return Pair("Terminal Origen", "Terminal Destino")
    }
}
