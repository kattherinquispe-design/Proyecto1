package com.ejemplo.smartbus.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


data class TrackingUiState(
    val status: String = "Seguimiento detenido",
    val stopName: String = "",
    val distanceMeters: Float = 0f,
    val speedKmh: Float = 0f,
    val trackingActive: Boolean = false,
    val pointsAdded: Int = 0
)


object TrackingEventManager {

    private val _trackingState =
        MutableStateFlow(
            TrackingUiState()
        )

    val trackingState: StateFlow<TrackingUiState> =
        _trackingState


    fun startTracking() {

        _trackingState.value =
            TrackingUiState(
                status = "Buscando paraderos...",
                trackingActive = true
            )
    }


    fun stopTracking() {

        _trackingState.value =
            TrackingUiState(
                status = "Seguimiento detenido",
                trackingActive = false
            )
    }


    fun updateAtStop(
        stopName: String,
        distanceMeters: Float
    ) {

        _trackingState.value =
            TrackingUiState(
                status = "Estás en el paradero",
                stopName = stopName,
                distanceMeters = distanceMeters,
                trackingActive = true
            )
    }


    fun updateWaitingAtStop(
        stopName: String,
        distanceMeters: Float,
        timeAtStopMs: Long
    ) {

        _trackingState.value =
            TrackingUiState(
                status = "Esperando abordaje...",
                stopName = stopName,
                distanceMeters = distanceMeters,
                trackingActive = true
            )
    }


    fun updateMoving(
        stopName: String,
        speedKmh: Float,
        distanceMeters: Float
    ) {

        _trackingState.value =
            TrackingUiState(
                status = "Verificando abordaje...",
                stopName = stopName,
                distanceMeters = distanceMeters,
                speedKmh = speedKmh,
                trackingActive = true
            )
    }


    fun boardingValidated(
        stopName: String,
        speedKmh: Float,
        distanceMeters: Float,
        pointsAdded: Int
    ) {

        _trackingState.value =
            TrackingUiState(
                status = "Abordaje validado",
                stopName = stopName,
                distanceMeters = distanceMeters,
                speedKmh = speedKmh,
                trackingActive = true,
                pointsAdded = pointsAdded
            )
    }


    fun updateStatus(
        status: String,
        stopName: String = "",
        distanceMeters: Float = 0f,
        speedKmh: Float = 0f
    ) {

        _trackingState.value =
            TrackingUiState(
                status = status,
                stopName = stopName,
                distanceMeters = distanceMeters,
                speedKmh = speedKmh,
                trackingActive = true
            )
    }
}