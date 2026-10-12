package com.ejemplo.smartbus.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Esquema de la base de datos del sistema de transporte.
 *
 * Estas tablas ya estan listas para recibir los datos reales:
 * cuando conectes tu base de datos (Firebase, REST o un GPS real),
 * solo tienes que escribir en ellas con [com.ejemplo.smartbus.data.TransitRepository]
 * y toda la app se actualiza sola porque todo se observa con Flow.
 */

@Entity(tableName = "bus_stops")
data class BusStopEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float,
    val isActive: Boolean,
    val hasAccessibility: Boolean
)

@Entity(
    tableName = "bus_routes",
    indices = [Index(value = ["code"], unique = true)]
)
data class BusRouteEntity(
    @PrimaryKey val id: Int,
    val code: String,
    val name: String,
    val origin: String,
    val destination: String,
    val frequencyMinutes: Int,
    val isActive: Boolean,
    val hasDeviation: Boolean,
    val deviationInfo: String?
)

/** Paradas que belongencen a cada ruta, en orden de recorrido. */
@Entity(
    tableName = "route_stops",
    primaryKeys = ["routeId", "stopId"],
    indices = [Index(value = ["routeId"]), Index(value = ["stopId"])]
)
data class RouteStopEntity(
    val routeId: Int,
    val stopId: Int,
    val sequence: Int,
    val distanceFromOriginMeters: Float
)

/** Trazado de la ruta: los puntos que se dibujan en el mapa. */
@Entity(
    tableName = "route_points",
    primaryKeys = ["routeId", "sequence"]
)
data class RoutePointEntity(
    val routeId: Int,
    val sequence: Int,
    val latitude: Double,
    val longitude: Double
)

/** Posicion actual e identacion de cada bus. */
@Entity(
    tableName = "vehicles",
    indices = [Index(value = ["routeId"])]
)
data class VehicleEntity(
    @PrimaryKey val id: Int,
    val code: String,
    val plate: String?,
    val routeId: Int,
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Float,
    val headingDegrees: Float?,
    val status: String,
    val direction: String,
    val occupancy: Int?,
    val gpsAccuracy: Float?,
    val isOutOfRoute: Boolean,
    val lastUpdate: Long,
    /**
     * Marca de origen de la posicion.
     *
     * false = la posicion viene del GPS del conductor registrado.
     * true = viene del simulador de demostracion.
     *
     * Importa porque un bus simulado no es un bus real: si se
     * presentara como conductor verificado, la app estaria
     * mintiendole al pasajero.
     */
    val isMock: Boolean = false,

    /**
     * Ultimos 4 digitos de la placa. En Peru los-conductores se
     * identifican por ahi, y asi el pasajero reconoce el vehiculo
     * sin exponer la placa completa en pantalla.
     */
    val plateCount: Int = 0,

    /** Nombre del conductor, vacio si el bus es de operador. */
    val driverName: String = "",

    /** Puestos del vehiculo, 0 si no se sabe. */
    val capacity: Int = 0
)

/**
 * Historial de posiciones. Sirve para dibujar el recorrido real del bus
 * y para calcular el tiempo de llegada con datos, no con suposiciones.
 */
@Entity(
    tableName = "vehicle_positions",
    indices = [Index(value = ["vehicleId"]), Index(value = ["recordedAt"])]
)
data class VehiclePositionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Int,
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Float,
    val recordedAt: Long
)

// ============================================================
// CONDUCTOR
// La idea es la misma que con los buses: los datos se escriben una
// sola vez, se observan con Flow y toda la app se actualiza sola.
// El accountId es el mismo storageKey de la sesion, asi el
// conductor y su pasajero quedan unidos sin tablas de union raras.
// ============================================================

/** Perfil del conductor: quien es y si esta verificado. */
@Entity(
    tableName = "driver_profiles",
    indices = [Index(value = ["documentId"], unique = true)]
)
data class DriverProfileEntity(
    @PrimaryKey val accountId: String,
    val fullName: String,
    val documentId: String,
    val documentType: String,
    val companyName: String,
    val companyCode: String,
    val birthDate: String?,
    val phone: String,
    val phoneVerified: Boolean,
    val licenseNumber: String,
    val licenseClass: String,
    val licenseExpiresAt: Long?,
    val emergencyContactName: String,
    val emergencyContactPhone: String,
    val status: String,
    val statusReason: String,
    val rating: Float,
    val totalTrips: Int,
    val createdAt: Long
)

/** Vehiculo del conductor. Un conductor puede tener varios. */
@Entity(
    tableName = "driver_vehicles",
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["plate"], unique = true)
    ]
)
data class DriverVehicleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: String,
    val plate: String,
    val brand: String,
    val model: String,
    val year: Int,
    val color: String,
    val plateCount: Int,
    val capacity: Int,
    val isPrimary: Boolean,
    val soatExpiresAt: Long?,
    val inspectionExpiresAt: Long?,
    val status: String,
    /**
     * Unidad de transporte a la que pertenece este vehiculo.
     *
     * Es el puente entre las dos caras de la app: el conductor
     * registra su auto con placa, y el operador lo asigna a una
     * unidad con ruta y codigo. Sin este campo el conductor
     * compartiria GPS pero el pasajero no tendria donde verlo.
     */
    val transitVehicleId: Int? = null,
    val createdAt: Long
)

/**
 * Documento subido por el conductor.
 *
 * El archivo se guarda en el almacenamiento interno de la app y aqui
 * solo queda la ruta. En produccion estos archivos van a un bucket con
 * cifrado y solo se muestran a los revisores autorizados.
 */
@Entity(
    tableName = "driver_documents",
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["accountId", "type"], unique = true)
    ]
)
data class DriverDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: String,
    val type: String,
    val filePath: String,
    val status: String,
    val note: String,
    val expiresAt: Long?,
    val uploadedAt: Long,
    val reviewedAt: Long?
)

/**
 * Turno actual: si el conductor esta online y donde esta.
 *
 * Se escribe desde el servicio en segundo plano, asi que es la tabla
 * que hace de puente entre el conductor y el pasajero que lo ve.
 */
@Entity(tableName = "driver_shifts")
data class DriverShiftEntity(
    @PrimaryKey val accountId: String,
    val vehicleId: Long?,
    val isOnline: Boolean,
    val isOnTrip: Boolean,
    val routeId: Int?,
    val startedAt: Long?,
    val lastLatitude: Double?,
    val lastLongitude: Double?,
    val lastSpeedKmh: Float,
    val lastHeadingDegrees: Float?,
    val gpsAccuracyMeters: Float?,
    val lastLocationAt: Long?
)

/** Viajes del conductor: ganancias y recorrido. */
@Entity(
    tableName = "driver_trips",
    indices = [Index(value = ["accountId"]), Index(value = ["startedAt"])]
)
data class DriverTripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: String,
    val vehicleId: Long?,
    val routeId: Int?,
    val originName: String,
    val destinationName: String,
    val startedAt: Long,
    val endedAt: Long?,
    val distanceMeters: Float,
    val fare: Double,
    val passengerName: String?
)