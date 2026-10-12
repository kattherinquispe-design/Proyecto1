package com.ejemplo.smartbus.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BusStopEntity::class,
        BusRouteEntity::class,
        RouteStopEntity::class,
        RoutePointEntity::class,
        VehicleEntity::class,
        VehiclePositionEntity::class,
        DriverProfileEntity::class,
        DriverVehicleEntity::class,
        DriverDocumentEntity::class,
        DriverShiftEntity::class,
        DriverTripEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class SmartBusDatabase : RoomDatabase() {

    abstract fun busStopDao(): BusStopDao
    abstract fun busRouteDao(): BusRouteDao
    abstract fun routeStopDao(): RouteStopDao
    abstract fun routePointDao(): RoutePointDao
    abstract fun vehicleDao(): VehicleDao
    abstract fun vehiclePositionDao(): VehiclePositionDao

    abstract fun driverProfileDao(): DriverProfileDao
    abstract fun driverVehicleDao(): DriverVehicleDao
    abstract fun driverDocumentDao(): DriverDocumentDao
    abstract fun driverShiftDao(): DriverShiftDao
    abstract fun driverTripDao(): DriverTripDao

    companion object {

        private const val NAME = "smartbus.db"

        @Volatile
        private var instance: SmartBusDatabase? = null

        fun get(context: Context): SmartBusDatabase =
            instance ?: synchronized(this) {
                instance ?: Room
                    .databaseBuilder(
                        context.applicationContext,
                        SmartBusDatabase::class.java,
                        NAME
                    )
                    // Mientras el esquema sea el de desarrollo se
                    // reconstruye la base al cambiarlo. Antes de salir
                    // hay que escribir migraciones de verdad: esto borra
                    // las posiciones guardadas al publicar una version
                    // con el esquema cambiado.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}