package com.ejemplo.smartbus.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PointsEventManager {

    private val _pointsEvents =
        MutableSharedFlow<Int>(
            replay = 1,
            extraBufferCapacity = 10
        )

    val pointsEvents: SharedFlow<Int> =
        _pointsEvents

    private val _pendingNavigationTarget = MutableStateFlow<String?>(null)
    val pendingNavigationTarget: StateFlow<String?> = _pendingNavigationTarget.asStateFlow()

    fun notifyPointsAdded(points: Int) {
        _pointsEvents.tryEmit(points)
    }

    fun requestNavigation(target: String) {
        _pendingNavigationTarget.value = target
    }

    fun consumeNavigation() {
        _pendingNavigationTarget.value = null
    }
}