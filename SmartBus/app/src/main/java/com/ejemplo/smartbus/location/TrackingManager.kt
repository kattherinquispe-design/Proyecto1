package com.ejemplo.smartbus.location

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class TrackingManager(
    private val context: Context
) {

    fun startTracking() {

        val intent = Intent(
            context,
            LocationTrackingService::class.java
        ).apply {
            action = LocationTrackingService.ACTION_START
        }

        ContextCompat.startForegroundService(
            context,
            intent
        )
    }

    fun stopTracking() {

        val intent = Intent(
            context,
            LocationTrackingService::class.java
        ).apply {
            action = LocationTrackingService.ACTION_STOP
        }

        context.startService(intent)
    }
}