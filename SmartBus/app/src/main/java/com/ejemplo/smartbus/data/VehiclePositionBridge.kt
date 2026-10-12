package com.ejemplo.smartbus.data

import android.content.Context
import android.util.Log
import com.ejemplo.smartbus.data.db.VehiclePositionEntity
import com.ejemplo.smartbus.data.model.Bus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Puente entre una fuente de posiciones de vehiculos y la base de datos.
 *
 * La app lee siempre los vehiculos desde Room, nunca desde la fuente
 * directa. Asi el mapa, el ETA y el historial usan los mismos datos.
 *
 * Aqui todavia no hay API del operador, asi que las posiciones siguen
 * viniendo de BusLiveRepository, que es un simulador. Eso esta
 * aislado detras de [useSimulatedFeed]: cuando exista el feed real del
 * operador, se implementa [VehiclePositionSource] contra el y se
 * apaga el simulador cambiando una sola linea.
 */
interface VehiclePositionSource {

    val name: String

    /** Posiciones actuales de los vehiculos. */
    suspend fun currentPositions(): List<Bus>

    /** Flujo de posiciones, para empujarlas a Room en cuanto llegan. */
    val recentPositions: Flow<List<Bus>>
}

/**
 * Fuente simulada: mueve los buses por el trazado de su ruta.
 *
 * NO es un feed real. Sirve para poder trabajar la UI mientras la API
 * del operador todavia no existe.
 */
class SimulatedVehicleSource : VehiclePositionSource {

    override val name: String = "simulador"

    override val recentPositions: Flow<List<Bus>> =
        BusLiveRepository.buses

    override suspend fun currentPositions(): List<Bus> =
        BusLiveRepository.buses.value
}

/**
 * Mantiene Room al dia con las posiciones de los vehiculos.
 */
class VehiclePositionBridge(
    private val transitRepository: TransitRepository,
    private val source: VehiclePositionSource,
    private val useSimulatedFeed: Boolean = true
) {

    private var lastSeenUpdate: Map<Int, Long> = emptyMap()

    /**
     * Copia las posiciones actuales a Room. Solo escribe si algo cambio,
     * para no llenar el historial con filas repetidas.
     */
    suspend fun syncOnce() {

        if (!useSimulatedFeed) {
            return
        }

        val buses = source.currentPositions()

        if (buses.isEmpty()) {
            return
        }

        val changed = buses.filter { bus ->
            lastSeenUpdate[bus.id] != bus.lastUpdate
        }

        if (changed.isEmpty()) {
            return
        }

        push(changed)
    }

    /**
     * Observa la fuente y escribe en Room cada vez que llega una
     * posicion nueva.
     */
    suspend fun observe() {

        source.recentPositions.collectLatest { buses ->

            if (buses.isEmpty()) {
                return@collectLatest
            }

            val changed = buses.filter { bus ->
                lastSeenUpdate[bus.id] != bus.lastUpdate
            }

            lastSeenUpdate = buses.associate { it.id to it.lastUpdate }

            if (changed.isNotEmpty()) {
                push(changed)
            }
        }
    }

    private suspend fun push(buses: List<Bus>) {

        try {
            transitRepository.syncVehiclePositions(
                buses.map { bus ->
                    VehiclePositionEntity(
                        vehicleId = bus.id,
                        latitude = bus.latitude,
                        longitude = bus.longitude,
                        speedKmh = bus.speedKmh,
                        recordedAt = bus.lastUpdate
                    )
                }
            )
        } catch (exception: Exception) {
            Log.w(
                "SmartBusTransit",
                "No se pudo sincronizar la posicion de los buses: " +
                    exception.message
            )
        }
    }
}

/**
 * Arranque del puente desde la Activity.
 */
object VehiclePositionSync {

    /**
     * Cuando exista la API del operador, se pasa aqui un
     * [VehiclePositionSource] real y se apaga el simulador.
     */
    fun start(
        context: Context,
        transitRepository: TransitRepository,
        scope: CoroutineScope
    ) {

        val bridge = VehiclePositionBridge(
            transitRepository = transitRepository,
            source = SimulatedVehicleSource(),
            useSimulatedFeed = true
        )

        scope.launch {
            bridge.observe()
        }
    }
}