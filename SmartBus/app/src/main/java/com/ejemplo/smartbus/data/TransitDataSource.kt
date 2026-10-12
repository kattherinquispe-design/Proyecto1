package com.ejemplo.smartbus.data

import com.ejemplo.smartbus.data.model.Bus
import com.ejemplo.smartbus.data.model.BusRoute
import com.ejemplo.smartbus.data.model.BusStop
import kotlinx.coroutines.flow.StateFlow

interface TransitDataSource {

    fun getRoutes(): List<BusRoute>

    fun getStops(): List<BusStop>

    val liveBuses: StateFlow<List<Bus>>
}


object DemoTransitDataSource : TransitDataSource {

    override fun getRoutes(): List<BusRoute> {

        return RouteRepository.routes
    }


    override fun getStops(): List<BusStop> {

        return BusStopRepository.stops
    }


    override val liveBuses: StateFlow<List<Bus>>
        get() = BusLiveRepository.buses
}