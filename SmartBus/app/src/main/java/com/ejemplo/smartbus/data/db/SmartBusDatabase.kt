package com.ejemplo.smartbus.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
    version = 5,
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

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE driver_profiles ADD COLUMN email TEXT NOT NULL DEFAULT ''")
            }
        }

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
                    .addMigrations(MIGRATION_4_5)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}