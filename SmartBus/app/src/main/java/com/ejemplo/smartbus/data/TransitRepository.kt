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
        vehicleDao.observeAll().map { list -> list.map { it.toModel() } }

    override fun observeVehiclesForRoute(routeId: Int): Flow<List<Bus>> =
        vehicleDao.observeByRoute(routeId).map { list -> list.map { it.toModel() } }

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
        vehicleDao.getAll().map { it.toModel() }

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
        if (stopDao.count() > 0 && routeDao.count() > 0) {
            return
        }

        val stops = BusStopRepository.stops
        val routes = RouteRepository.routes
        val buses = BusRepository.buses

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

        vehicleDao.upsertAll(buses.map { it.toEntity() })

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
        capacity = capacity
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
        isMock = true
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
 * Busca que rutas sirven a un destino concreto.
 *
 * Busca la parada mas cercana al destino y devuelve todas las rutas que
 * pasan por ella, con los buses que hay ahora mismo en cada ruta.
 */
fun findRoutesForDestination(
    destinationLat: Double,
    destinationLng: Double,
    stops: List<BusStop>,
    routes: List<BusRoute>,
    buses: List<Bus>,
    searchRadiusMeters: Float = 1200f
): List<RouteCandidate> {

    val nearbyStops =
        stops
            .map { it to distanceInMeters(destinationLat, destinationLng, it.latitude, it.longitude) }
            .filter { it.second <= searchRadiusMeters }
            .sortedBy { it.second }

    if (nearbyStops.isEmpty()) {
        return emptyList()
    }

    val boardingStopId = nearbyStops.first().first.id
    val boardingStop = nearbyStops.first().first

    return routes
        .filter { boardingStopId in it.stopIds }
        .map { route ->

            val routeBuses =
                buses.filter { it.routeId == route.id }

            val closest =
                routeBuses
                    .map {
                        it to distanceInMeters(
                            it.latitude,
                            it.longitude,
                            boardingStop.latitude,
                            boardingStop.longitude
                        )
                    }
                    .minByOrNull { it.second }

            RouteCandidate(
                route = route,
                boardingStop = boardingStop,
                distanceToStopMeters = nearbyStops.first().second,
                buses = routeBuses,
                closestBus = closest?.first,
                closestBusDistanceMeters = closest?.second
            )
        }
        .sortedBy { it.closestBusDistanceMeters ?: Float.MAX_VALUE }
}