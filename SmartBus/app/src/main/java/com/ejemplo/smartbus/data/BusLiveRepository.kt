package com.ejemplo.smartbus.data

import android.util.Log
import com.ejemplo.smartbus.data.model.Bus
import com.ejemplo.smartbus.data.model.GeoPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object BusLiveRepository {

    private const val SIMULATION_INTERVAL_MS = 3_000L
    private const val SIMULATION_STEP = 0.03f

    private val _buses =
        MutableStateFlow(
            BusRepository.buses
        )

    val buses: StateFlow<List<Bus>> =
        _buses

    private val progressByBus =
        mutableMapOf<Int, Float>()

    private val simulationScope =
        CoroutineScope(
            SupervisorJob() +
                Dispatchers.Default
        )


    init {

        BusRepository.buses.forEach { bus ->

            progressByBus[bus.id] = 0f
        }

        startSimulation()
    }


    // =====================================================
    // SIMULACION DE FLOTA EN TIEMPO REAL
    // =====================================================

    private fun startSimulation() {

        simulationScope.launch {

            while (isActive) {

                delay(SIMULATION_INTERVAL_MS)

                try {

                    advanceSimulation()

                } catch (exception: Exception) {

                    Log.e(
                        "SmartBusFleet",
                        "Error en la simulacion de flota: ${exception.message}"
                    )
                }
            }
        }
    }


    fun advanceSimulation() {

        val currentBuses =
            _buses.value.toMutableList()


        val updatedBuses =
            currentBuses.map { bus ->

                val route =
                    RouteRepository.routes
                        .firstOrNull {
                            it.id == bus.routeId
                        }


                if (
                    route == null ||
                    route.points.size < 2
                ) {
                    return@map bus
                }


                var progress =
                    progressByBus[bus.id]
                        ?: 0f


                progress += SIMULATION_STEP


                if (
                    progress >=
                    route.points.size - 1
                ) {

                    progress = 0f
                }


                progressByBus[bus.id] =
                    progress


                val segment =
                    progress.toInt()


                val fraction =
                    (progress - segment).coerceIn(0f, 1f)


                val start =
                    route.points[segment]


                val end =
                    route.points[
                        segment + 1
                    ]


                val latitude =
                    interpolate(
                        start.latitude,
                        end.latitude,
                        fraction
                    )


                val longitude =
                    interpolate(
                        start.longitude,
                        end.longitude,
                        fraction
                    )


                bus.copy(
                    latitude = latitude,
                    longitude = longitude
                )
            }


        val now = System.currentTimeMillis()

        _buses.value =
            updatedBuses.map { bus ->
                // Sin marcar la hora, la base de datos no distingue esta
                // posicion de la anterior y deja de guardar el recorrido.
                bus.copy(lastUpdate = now)
            }
    }


    private fun interpolate(
        start: Double,
        end: Double,
        fraction: Float
    ): Double {

        return start +
                (end - start) *
                fraction
    }


    fun getBusById(
        busId: Int
    ): Bus? {

        return _buses.value
            .firstOrNull {
                it.id == busId
            }
    }
}