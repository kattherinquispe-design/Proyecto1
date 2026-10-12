package com.ejemplo.smartbus.data

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Preferencias de la pantalla de Configuracion.
 * Los valores se guardan en SharedPreferences y se exponen como
 * estado de Compose para que los cambios se reflejen al instante.
 */
class SettingsRepository(context: Context) {

    private val preferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        const val PREFS_NAME = "smartbus_settings"

        const val UNITS_METRIC = "metric"
        const val UNITS_IMPERIAL = "imperial"

        private const val KEY_AUTO_TRACKING = "auto_tracking"
        private const val KEY_NOTIFICATIONS = "notifications_enabled"
        private const val KEY_BOARDING_ALERTS = "boarding_alerts"
        private const val KEY_HIGH_ACCURACY = "high_accuracy_location"
        private const val KEY_DISTANCE_UNITS = "distance_units"
        private const val KEY_AUTO_START_ON_BOARD = "auto_start_on_board"
    }

    private val autoTrackingState = mutableStateOf(
        preferences.getBoolean(KEY_AUTO_TRACKING, true)
    )
    val autoTracking: Boolean get() = autoTrackingState.value

    private val notificationsState = mutableStateOf(
        preferences.getBoolean(KEY_NOTIFICATIONS, true)
    )
    val notificationsEnabled: Boolean get() = notificationsState.value

    private val boardingAlertsState = mutableStateOf(
        preferences.getBoolean(KEY_BOARDING_ALERTS, true)
    )
    val boardingAlerts: Boolean get() = boardingAlertsState.value

    private val highAccuracyState = mutableStateOf(
        preferences.getBoolean(KEY_HIGH_ACCURACY, true)
    )
    val highAccuracyLocation: Boolean get() = highAccuracyState.value

    private val autoStartOnBoardState = mutableStateOf(
        preferences.getBoolean(KEY_AUTO_START_ON_BOARD, false)
    )
    val autoStartOnBoard: Boolean get() = autoStartOnBoardState.value

    private val distanceUnitsState = mutableStateOf(
        preferences.getString(KEY_DISTANCE_UNITS, UNITS_METRIC) ?: UNITS_METRIC
    )
    val distanceUnits: String get() = distanceUnitsState.value

    fun setAutoTracking(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AUTO_TRACKING, enabled).apply()
        autoTrackingState.value = enabled
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
        notificationsState.value = enabled
    }

    fun setBoardingAlerts(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_BOARDING_ALERTS, enabled).apply()
        boardingAlertsState.value = enabled
    }

    fun setHighAccuracyLocation(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_HIGH_ACCURACY, enabled).apply()
        highAccuracyState.value = enabled
    }

    fun setAutoStartOnBoard(enabled: Boolean) {
        preferences.edit()
            .putBoolean(KEY_AUTO_START_ON_BOARD, enabled)
            .apply()
        autoStartOnBoardState.value = enabled
    }

    fun setDistanceUnits(units: String) {
        preferences.edit().putString(KEY_DISTANCE_UNITS, units).apply()
        distanceUnitsState.value = units
    }

    fun resetToDefaults() {
        preferences.edit().clear().apply()
        autoTrackingState.value = true
        notificationsState.value = true
        boardingAlertsState.value = true
        highAccuracyState.value = true
        autoStartOnBoardState.value = false
        distanceUnitsState.value = UNITS_METRIC
    }

    /**
     * Convierte una distancia en metros al formato elegido por el usuario.
     */
    fun formatDistance(meters: Float): String =
        if (distanceUnits == UNITS_IMPERIAL) {
            val miles = meters / 1609.344f
            if (miles < 0.19f) {
                "${meters.roundToInt()} m"
            } else {
                String.format(Locale.getDefault(), "%.1f mi", miles)
            }
        } else {
            if (meters < 950f) {
                "${meters.roundToInt()} m"
            } else {
                String.format(Locale.getDefault(), "%.1f km", meters / 1000f)
            }
        }

    /**
     * Estado observable de la preferencia de seguimiento automatico.
     * Permite a MainActivity observar los cambios sin recomponer.
     */
    fun observeAutoTracking(): State<Boolean> = autoTrackingState
}