package com.ejemplo.smartbus.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

object PointsEventManager {

    private val _pointsEvents =
        MutableSharedFlow<Int>(
            replay = 0,
            extraBufferCapacity = 10
        )

    val pointsEvents: SharedFlow<Int> =
        _pointsEvents

    fun notifyPointsAdded(points: Int) {
        _pointsEvents.tryEmit(points)
    }
}