package com.ejemplo.smartbus.data

import android.content.Context
import android.location.Location
import com.ejemplo.smartbus.data.db.BusRouteEntity
import com.ejemplo.smartbus.data.db.BusStopEntity
import com.ejemplo.smartbus.data.db.RoutePointEntity
import com.ejemplo.smartbus.data.db.RouteStopEntity
import com.ejemplo.smartbus.data.db.SmartBusDatabase
import com.ejemplo.smartbus.data.db.VehicleEntity
import com.ejemplo.smartbus.data.db.VehiclePositionEntity
import com.ejemplo.smartbus.data.model.Bus
import com.ejemplo.smartbus.data.model.BusRoute
import com.ejemplo.smartbus.data.model.BusStop
import com.ejemplo.smartbus.data.model.GeoPoint
import com.ejemplo.smartbus.data.model.RouteCandidate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Fuente de datos del transporte.
 *
 * La app solo habla con esta interfaz. Hoy la implementa Room (datos
 * locales); cuando conectes tu base de datos real, implementa la misma
 * interfaz contra Firebase o tu API y la UI no cambia ni una linea.
 */
interface TransitRepository {

    fun observeStops(): Flow<List<BusStop>>
    fun observeRoutes(): Flow<List<BusRoute>>
    fun observeVehicles(): Flow<List<Bus>>
    fun observeVehiclesForRoute(routeId: Int): Flow<List<Bus>>

    suspend fun getStopsOnce(): List<BusStop>
    suspend fun getRoutesOnce(): List<BusRoute>
    suspend fun getVehiclesOnce(): List<Bus>

    /** Carga los datos iniciales si la base esta vacia. */
    suspend fun seedIfEmpty()

    /**
     * Punto de entrada para la geolocalizacion real de los buses.
     * Cuando tu base de datos envie la posicion de cada bus, solo
     * tienes que llamar a esto: el mapa, el ETA y las paradas se
     * actualizan solos.
     */
    suspend fun updateVehiclePosition(
        vehicleId: Int,
        latitude: Double,
        longitude: Double,
        speedKmh: Float = 0f,
        headingDegrees: Float? = null,
        gpsAccuracy: Float? = null,
        recordedAt: Long = System.currentTimeMillis()
    )

    /** Guarda varias posiciones de golpe (por ejemplo, al sincronizar). */
    suspend fun syncVehiclePositions(positions: List<VehiclePositionEntity>)

    suspend fun getVehicleTrail(vehicleId: Int, since: Long): List<GeoPoint>
}

class RoomTransitRepository(
    context: Context
) : TransitRepository {

    private val db = SmartBusDatabase.get(context)

    private val stopDao = db.busStopDao()
    private val routeDao = db.busRouteDao()
    private val routeStopDao = db.routeStopDao()
    private val routePointDao = db.routePointDao()
    private val vehicleDao = db.vehicleDao()
    private val positionDao = db.vehiclePositionDao()

    // =========================================================
    // OBSERVACION
    // =========================================================

    override fun observeStops(): Flow<List<BusStop>> =
        stopDao.observeActive().map { list -> list.map { it.toModel() } }

    override fun observeRoutes(): Flow<List<BusRoute>> =
        combine(
            routeDao.observeActive(),
            routeStopDao.observeAll(),
            routePointDao.observeAll()
        ) { routes, routeStops, routePoints ->

            routes.map { route ->

                val stopsForRoute =
                    routeStops
                        .filter { it.routeId == route.id }
                        .sortedBy { it.sequence }
                        .map { it.stopId }

                val pointsForRoute =
                    routePoints
                        .filter { it.routeId == route.id }
                        .sortedBy { it.sequence }
                        .map {
                            GeoPoint(
                                latitude = it.latitude,
                                longitude = it.longitude
                            )
                        }

                route.toModel(
                    stopIds = stopsForRoute,
                    points = pointsForRoute
                )
            }
        }

    override fun observeVehicles(): Flow<List<Bus>> =
        vehicleDao.observeAll().map { list ->
            list.filter { !it.isMock && it.driverName.isNotBlank() }.map { it.toModel() }
        }

    override fun observeVehiclesForRoute(routeId: Int): Flow<List<Bus>> =
        vehicleDao.observeByRoute(routeId).map { list ->
            list.filter { !it.isMock && it.driverName.isNotBlank() }.map { it.toModel() }
        }

    override suspend fun getStopsOnce(): List<BusStop> =
        stopDao.getAll().map { it.toModel() }

    override suspend fun getRoutesOnce(): List<BusRoute> {

        val routeStops = routeStopDao.getAll()
        val routePoints = routePointDao.getAll()

        return routeDao.getAll().map { route ->
            route.toModel(
                stopIds = routeStops
                    .filter { it.routeId == route.id }
                    .sortedBy { it.sequence }
                    .map { it.stopId },
                points = routePoints
                    .filter { it.routeId == route.id }
                    .sortedBy { it.sequence }
                    .map { GeoPoint(it.latitude, it.longitude) }
            )
        }
    }

    override suspend fun getVehiclesOnce(): List<Bus> =
        vehicleDao.getAll().filter { !it.isMock && it.driverName.isNotBlank() }.map { it.toModel() }

    // =========================================================
    // DATOS REALES
    // =========================================================

    override suspend fun updateVehiclePosition(
        vehicleId: Int,
        latitude: Double,
        longitude: Double,
        speedKmh: Float,
        headingDegrees: Float?,
        gpsAccuracy: Float?,
        recordedAt: Long
    ) {
        vehicleDao.updatePosition(
            vehicleId = vehicleId,
            latitude = latitude,
            longitude = longitude,
            speedKmh = speedKmh,
            headingDegrees = headingDegrees,
            gpsAccuracy = gpsAccuracy,
            lastUpdate = recordedAt
        )

        positionDao.insert(
            VehiclePositionEntity(
                vehicleId = vehicleId,
                latitude = latitude,
                longitude = longitude,
                speedKmh = speedKmh,
                recordedAt = recordedAt
            )
        )
    }

    override suspend fun syncVehiclePositions(positions: List<VehiclePositionEntity>) {
        positions.forEach { position ->
            vehicleDao.updatePosition(
                vehicleId = position.vehicleId,
                latitude = position.latitude,
                longitude = position.longitude,
                speedKmh = position.speedKmh,
                headingDegrees = null,
                gpsAccuracy = null,
                lastUpdate = position.recordedAt
            )
            positionDao.insert(position)
        }
    }

    override suspend fun getVehicleTrail(
        vehicleId: Int,
        since: Long
    ): List<GeoPoint> =
        positionDao.getRecent(vehicleId, since).map {
            GeoPoint(
                latitude = it.latitude,
                longitude = it.longitude,
                speedKmh = it.speedKmh,
                timestamp = it.recordedAt
            )
        }

    // =========================================================
    // SEMILLA
    // =========================================================

    override suspend fun seedIfEmpty() {
        try {
            FirebaseRouteRepository.loadRoutes()
        } catch (_: Exception) {
        }

        // Siempre eliminar cualquier vehiculo simulado antiguo de la base local
        try {
            vehicleDao.deleteSimulatedVehicles()
        } catch (_: Exception) {
        }

        if (stopDao.count() > 0 && routeDao.count() > 0) {
            return
        }

        val stops = BusStopRepository.stops
        val routes = RouteRepository.routes

        stopDao.upsertAll(stops.map { it.toEntity() })

        routeDao.upsertAll(routes.map { it.toEntity() })

        routeStopDao.upsertAll(
            routes.flatMap { route ->
                route.stopIds.mapIndexed { index, stopId ->
                    RouteStopEntity(
                        routeId = route.id,
                        stopId = stopId,
                        sequence = index,
                        distanceFromOriginMeters = index * 900f
                    )
                }
            }
        )

        routePointDao.upsertAll(
            routes.flatMap { route ->
                route.points.mapIndexed { index, point ->
                    RoutePointEntity(
                        routeId = route.id,
                        sequence = index,
                        latitude = point.latitude,
                        longitude = point.longitude
                    )
                }
            }
        )

        pruneEmptyTrail()
    }

    private suspend fun pruneEmptyTrail() {
        val cutoff = System.currentTimeMillis() - TRAIL_RETENTION_MS
        positionDao.deleteOlderThan(cutoff)
    }

    companion object {
        private const val TRAIL_RETENTION_MS = 2 * 60 * 60 * 1000L

        @Volatile
        private var instance: RoomTransitRepository? = null

        fun get(context: Context): RoomTransitRepository =
            instance ?: synchronized(this) {
                instance ?: RoomTransitRepository(context).also { instance = it }
            }
    }
}

// =========================================================
// CONVERSIONES
// =========================================================

private fun BusStopEntity.toModel() =
    BusStop(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        radiusMeters = radiusMeters,
        isActive = isActive,
        hasAccessibility = hasAccessibility
    )

private fun BusStop.toEntity() =
    BusStopEntity(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        radiusMeters = radiusMeters,
        isActive = isActive,
        hasAccessibility = hasAccessibility
    )

private fun BusRouteEntity.toModel(
    stopIds: List<Int>,
    points: List<GeoPoint>
) =
    BusRoute(
        id = id,
        code = code,
        name = name,
        origin = origin,
        destination = destination,
        stopIds = stopIds,
        points = points,
        frequencyMinutes = frequencyMinutes,
        isActive = isActive,
        hasDeviation = hasDeviation,
        deviationInfo = deviationInfo
    )

private fun BusRoute.toEntity() =
    BusRouteEntity(
        id = id,
        code = code,
        name = name,
        origin = origin,
        destination = destination,
        frequencyMinutes = frequencyMinutes,
        isActive = isActive,
        hasDeviation = hasDeviation,
        deviationInfo = deviationInfo
    )

private fun VehicleEntity.toModel() =
    Bus(
        id = id,
        code = code,
        routeId = routeId,
        latitude = latitude,
        longitude = longitude,
        speedKmh = speedKmh,
        status = status,
        direction = direction,
        lastUpdate = lastUpdate,
        occupancy = occupancy,
        isOutOfRoute = isOutOfRoute,
        gpsAccuracy = gpsAccuracy,
        isMock = isMock,
        plateCount = plateCount,
        driverName = driverName,
        capacity = capacity,
        tripType = tripType
    )

private fun Bus.toEntity() =
    VehicleEntity(
        id = id,
        code = code,
        plate = null,
        routeId = routeId,
        latitude = latitude,
        longitude = longitude,
        speedKmh = speedKmh,
        headingDegrees = null,
        status = status,
        direction = direction,
        occupancy = occupancy,
        gpsAccuracy = gpsAccuracy,
        isOutOfRoute = isOutOfRoute,
        lastUpdate = lastUpdate,
        // El seed viene del simulador, no de un conductor real.
        // La app lo marca para no presentarlo como vehiculo en servicio.
        isMock = true,
        tripType = tripType
    )

// =========================================================
// HELPERS DE DISTANCIA
// =========================================================

fun distanceInMeters(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double
): Float {
    val results = FloatArray(1)
    Location.distanceBetween(lat1, lon1, lat2, lon2, results)
    return results[0]
}

/**
 * Busca las rutas que sirven a un destino concreto en un radio de 400 metros
 * (o las más cercanas), calculando la distancia tanto hacia el destino
 * como desde la ubicación en tiempo real del pasajero hacia la ruta.
 */
fun findRoutesForDestination(
    destinationLat: Double,
    destinationLng: Double,
    stops: List<BusStop>,
    routes: List<BusRoute>,
    buses: List<Bus>,
    userLat: Double? = null,
    userLng: Double? = null,
    searchRadiusMeters: Float = 400f
): List<RouteCandidate> {

    val candidates = mutableListOf<RouteCandidate>()

    fun evaluateRoutes(radius: Float) {
        for (route in routes) {
            val allPoints = (route.subidaPoints + route.bajadaPoints).ifEmpty { route.points }
            if (allPoints.isEmpty()) continue

            // 1. Distancia desde el destino final hacia los puntos de la ruta
            var minDestDistance = Float.MAX_VALUE
            var closestDestPt: GeoPoint? = null

            for (pt in allPoints) {
                val d = distanceInMeters(destinationLat, destinationLng, pt.latitude, pt.longitude)
                if (d < minDestDistance) {
                    minDestDistance = d
                    closestDestPt = pt
                }
            }

            if (minDestDistance <= radius && closestDestPt != null) {
                // 2. Distancia desde la ubicación del pasajero en tiempo real hacia la ruta
                var minUserDistance: Float? = null
                var closestUserPt: GeoPoint? = null

                if (userLat != null && userLng != null) {
                    var uDist = Float.MAX_VALUE
                    for (pt in allPoints) {
                        val d = distanceInMeters(userLat, userLng, pt.latitude, pt.longitude)
                        if (d < uDist) {
                            uDist = d
                            closestUserPt = pt
                        }
                    }
                    minUserDistance = uDist
                }

                val routeBuses = buses.filter { it.routeId == route.id }
                val boardingRefPt = closestUserPt ?: closestDestPt
                val closestBus = routeBuses.minByOrNull {
                    distanceInMeters(it.latitude, it.longitude, boardingRefPt.latitude, boardingRefPt.longitude)
                }
                val closestBusDist = closestBus?.let {
                    distanceInMeters(it.latitude, it.longitude, boardingRefPt.latitude, boardingRefPt.longitude)
                }

                // Buscar paradero formal cercano o crear punto de abordaje
                val matchingStop = stops.minByOrNull {
                    distanceInMeters(boardingRefPt.latitude, boardingRefPt.longitude, it.latitude, it.longitude)
                }?.takeIf {
                    distanceInMeters(boardingRefPt.latitude, boardingRefPt.longitude, it.latitude, it.longitude) <= 300f
                } ?: BusStop(
                    id = route.id,
                    name = "Punto de abordaje • ${route.name}",
                    latitude = boardingRefPt.latitude,
                    longitude = boardingRefPt.longitude
                )

                candidates.add(
                    RouteCandidate(
                        route = route,
                        boardingStop = matchingStop,
                        distanceToStopMeters = minUserDistance ?: minDestDistance,
                        buses = routeBuses,
                        closestBus = closestBus,
                        closestBusDistanceMeters = closestBusDist,
                        distanceToUserMeters = minUserDistance,
                        distanceToDestinationMeters = minDestDistance
                    )
                )
            }
        }
    }

    // Buscamos estrictamente en el radio de 400 m solicitado
    evaluateRoutes(searchRadiusMeters)

    // Si ninguna ruta pasa exactamente a 400 m, ampliamos ligeramente a 700 m como tolerancia
    if (candidates.isEmpty()) {
        evaluateRoutes(700f)
    }

    // Ordenar: primero las que dejan más cerca del destino (menor distancia al destino),
    // y luego por menor distancia para caminar desde la ubicación actual
    return candidates
        .distinctBy { it.route.id }
        .sortedWith(
            compareBy<RouteCandidate> { it.distanceToDestinationMeters ?: Float.MAX_VALUE }
                .thenBy { it.distanceToUserMeters ?: Float.MAX_VALUE }
        )
}