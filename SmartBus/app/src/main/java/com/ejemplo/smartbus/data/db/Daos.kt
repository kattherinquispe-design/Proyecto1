package com.ejemplo.smartbus.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BusStopDao {

    @Query("SELECT * FROM bus_stops WHERE isActive = 1 ORDER BY name")
    fun observeActive(): Flow<List<BusStopEntity>>

    @Query("SELECT * FROM bus_stops ORDER BY id")
    suspend fun getAll(): List<BusStopEntity>

    @Query("SELECT COUNT(*) FROM bus_stops")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<BusStopEntity>)
}

@Dao
interface BusRouteDao {

    @Query("SELECT * FROM bus_routes WHERE isActive = 1 ORDER BY code")
    fun observeActive(): Flow<List<BusRouteEntity>>

    @Query("SELECT * FROM bus_routes ORDER BY id")
    suspend fun getAll(): List<BusRouteEntity>

    @Query("SELECT COUNT(*) FROM bus_routes")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<BusRouteEntity>)
}

@Dao
interface RouteStopDao {

    @Query("SELECT * FROM route_stops ORDER BY routeId, sequence")
    fun observeAll(): Flow<List<RouteStopEntity>>

    @Query("SELECT * FROM route_stops ORDER BY routeId, sequence")
    suspend fun getAll(): List<RouteStopEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<RouteStopEntity>)
}

@Dao
interface RoutePointDao {

    @Query("SELECT * FROM route_points ORDER BY routeId, sequence")
    fun observeAll(): Flow<List<RoutePointEntity>>

    @Query("SELECT * FROM route_points ORDER BY routeId, sequence")
    suspend fun getAll(): List<RoutePointEntity>

    @Query("SELECT * FROM route_points WHERE routeId = :routeId ORDER BY sequence")
    suspend fun getForRoute(routeId: Int): List<RoutePointEntity>

    @Query("DELETE FROM route_points WHERE routeId = :routeId")
    suspend fun deleteForRoute(routeId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<RoutePointEntity>)
}

@Dao
interface VehicleDao {

    @Query("SELECT * FROM vehicles ORDER BY code")
    fun observeAll(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles WHERE routeId = :routeId ORDER BY code")
    fun observeByRoute(routeId: Int): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles ORDER BY id")
    suspend fun getAll(): List<VehicleEntity>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun getById(id: Int): VehicleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<VehicleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: VehicleEntity)

    /**
     * Actualiza la posicion de una unidad simulada.
     *
     * El filtro `driverName = ''` es la parte importante: si la unidad
     * tiene un conductor real asignado, el simulador NO la puede mover.
     * Sin esto, la posicion del conductor y la del simulador se
     * pisarian cada pocos segundos y el pasajero veria el vehiculo
     * saltando de un lado a otro en vez de en tiempo real.
     */
    @Query(
        """
        UPDATE vehicles
        SET latitude = :latitude,
            longitude = :longitude,
            speedKmh = :speedKmh,
            headingDegrees = :headingDegrees,
            gpsAccuracy = :gpsAccuracy,
            lastUpdate = :lastUpdate
        WHERE id = :vehicleId AND driverName = ''
        """
    )
    suspend fun updatePosition(
        vehicleId: Int,
        latitude: Double,
        longitude: Double,
        speedKmh: Float,
        headingDegrees: Float?,
        gpsAccuracy: Float?,
        lastUpdate: Long
    )

    /**
     * Suelta la unidad: el conductor ya no la conduce, asi que deja de
     * asociarse a el y el simulador vuelve a poder moverla. Es lo que
     * hace que, al terminar el servicio o al desviarse demasiado, el
     * pasajero deje de ver al conductor.
     */
    @Query(
        """
        UPDATE vehicles
        SET driverName = '',
            plate = NULL,
            plateCount = 0,
            isMock = 1,
            status = 'En ruta'
        WHERE id = :vehicleId
        """
    )
    suspend fun clearLivePosition(vehicleId: Int)

    /**
     * Escribe la posicion real de una unidad que tiene conductor
     * asignado.
     *
     * A diferencia de [updatePosition], esta tambien deja el
     *Vehiculo como no simulado y guarda los datos que el pasajero
     * necesita ver: ultimos 4 de la placa, nombre del conductor y
     * capacidad.
     */
    @Query(
        """
        UPDATE vehicles
        SET latitude = :latitude,
            longitude = :longitude,
            speedKmh = :speedKmh,
            headingDegrees = :headingDegrees,
            gpsAccuracy = :gpsAccuracy,
            isMock = :isMock,
            plate = :plate,
            plateCount = :plateCount,
            driverName = :driverName,
            capacity = :capacity,
            occupancy = :occupancy,
            status = :status,
            lastUpdate = :lastUpdate
        WHERE id = :vehicleId
        """
    )
    suspend fun upsertWithLivePosition(
        vehicleId: Int,
        latitude: Double,
        longitude: Double,
        speedKmh: Float,
        headingDegrees: Float?,
        gpsAccuracy: Float?,
        isMock: Boolean,
        plate: String?,
        plateCount: Int,
        driverName: String,
        capacity: Int,
        occupancy: Int?,
        status: String,
        lastUpdate: Long
    )
}

@Dao
interface VehiclePositionDao {

    @Query(
        """
        SELECT * FROM vehicle_positions
        WHERE vehicleId = :vehicleId AND recordedAt >= :since
        ORDER BY recordedAt ASC
        """
    )
    fun observeRecent(vehicleId: Int, since: Long): Flow<List<VehiclePositionEntity>>

    @Query(
        """
        SELECT * FROM vehicle_positions
        WHERE vehicleId = :vehicleId AND recordedAt >= :since
        ORDER BY recordedAt ASC
        """
    )
    suspend fun getRecent(vehicleId: Int, since: Long): List<VehiclePositionEntity>

    @Query("SELECT COUNT(*) FROM vehicle_positions WHERE vehicleId = :vehicleId")
    suspend fun countForVehicle(vehicleId: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: VehiclePositionEntity)

    @Query("DELETE FROM vehicle_positions WHERE recordedAt < :before")
    suspend fun deleteOlderThan(before: Long)
}

// ============================================================
// CONDUCTOR
// ============================================================

@Dao
interface DriverProfileDao {

    @Query("SELECT * FROM driver_profiles WHERE accountId = :accountId")
    fun observe(accountId: String): Flow<DriverProfileEntity?>

    @Query("SELECT * FROM driver_profiles WHERE accountId = :accountId")
    suspend fun get(accountId: String): DriverProfileEntity?

    @Query("SELECT COUNT(*) FROM driver_profiles WHERE documentId = :documentId")
    suspend fun countByDocumentId(documentId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: DriverProfileEntity)

    @Query("DELETE FROM driver_profiles WHERE accountId = :accountId")
    suspend fun delete(accountId: String)
}

@Dao
interface DriverVehicleDao {

    @Query("SELECT * FROM driver_vehicles WHERE accountId = :accountId ORDER BY isPrimary DESC, id")
    fun observeForAccount(accountId: String): Flow<List<DriverVehicleEntity>>

    @Query("SELECT * FROM driver_vehicles WHERE accountId = :accountId ORDER BY isPrimary DESC, id")
    suspend fun getForAccount(accountId: String): List<DriverVehicleEntity>

    @Query("SELECT * FROM driver_vehicles WHERE id = :id")
    fun observeById(id: Long): Flow<DriverVehicleEntity?>

    @Query("SELECT * FROM driver_vehicles WHERE id = :id")
    suspend fun getById(id: Long): DriverVehicleEntity?

    @Query("SELECT COUNT(*) FROM driver_vehicles WHERE plate = :plate")
    suspend fun countByPlate(plate: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(vehicle: DriverVehicleEntity): Long

    @Query("UPDATE driver_vehicles SET isPrimary = 0 WHERE accountId = :accountId")
    suspend fun clearPrimary(accountId: String)

    @Query("DELETE FROM driver_vehicles WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface DriverDocumentDao {

    @Query("SELECT * FROM driver_documents WHERE accountId = :accountId")
    fun observeForAccount(accountId: String): Flow<List<DriverDocumentEntity>>

    @Query("SELECT * FROM driver_documents WHERE accountId = :accountId")
    suspend fun getForAccount(accountId: String): List<DriverDocumentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(document: DriverDocumentEntity): Long

    @Query("DELETE FROM driver_documents WHERE accountId = :accountId AND type = :type")
    suspend fun delete(accountId: String, type: String)

    @Query("SELECT COUNT(*) FROM driver_documents WHERE status = 'APPROVED' AND accountId = :accountId")
    suspend fun countApproved(accountId: String): Int
}

@Dao
interface DriverShiftDao {

    @Query("SELECT * FROM driver_shifts WHERE accountId = :accountId")
    fun observe(accountId: String): Flow<DriverShiftEntity?>

    @Query("SELECT * FROM driver_shifts WHERE accountId = :accountId")
    suspend fun get(accountId: String): DriverShiftEntity?

    /**
     * Conductores que estan online y con GPS reciente. Es lo que
     * consulta el pasajero para saber a quien puede ver.
     */
    @Query(
        """
        SELECT * FROM driver_shifts
        WHERE isOnline = 1
        AND lastLocationAt IS NOT NULL
        AND lastLocationAt > :minLocationAt
        ORDER BY lastLocationAt DESC
        """
    )
    fun observeOnlineWithFreshGps(minLocationAt: Long): Flow<List<DriverShiftEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(shift: DriverShiftEntity)

    @Query(
        """
        UPDATE driver_shifts
        SET lastLatitude = :latitude,
            lastLongitude = :longitude,
            lastSpeedKmh = :speedKmh,
            lastHeadingDegrees = :headingDegrees,
            gpsAccuracyMeters = :gpsAccuracyMeters,
            lastLocationAt = :recordedAt
        WHERE accountId = :accountId
        """
    )
    suspend fun updateLocation(
        accountId: String,
        latitude: Double,
        longitude: Double,
        speedKmh: Float,
        headingDegrees: Float?,
        gpsAccuracyMeters: Float?,
        recordedAt: Long
    )
}

@Dao
interface DriverTripDao {

    @Query("SELECT * FROM driver_trips WHERE accountId = :accountId ORDER BY startedAt DESC")
    fun observeForAccount(accountId: String): Flow<List<DriverTripEntity>>

    @Query("SELECT * FROM driver_trips WHERE accountId = :accountId AND endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActive(accountId: String): DriverTripEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(trip: DriverTripEntity): Long

    @Query("UPDATE driver_trips SET endedAt = :endedAt, distanceMeters = :distanceMeters, fare = :fare WHERE id = :id")
    suspend fun close(id: Long, endedAt: Long, distanceMeters: Float, fare: Double)
}