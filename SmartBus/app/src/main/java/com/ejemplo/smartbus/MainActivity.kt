package com.ejemplo.smartbus

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.ejemplo.smartbus.data.PointsEventManager
import com.ejemplo.smartbus.data.SettingsRepository
import com.ejemplo.smartbus.data.SmartBusNotificationManager
import com.ejemplo.smartbus.location.TrackingManager
import com.ejemplo.smartbus.navigation.SmartBusNavigation
import com.ejemplo.smartbus.ui.theme.AppPalette
import com.ejemplo.smartbus.ui.theme.SmartBusTheme

/**
 * Actividad unica de la app.
 *
 * Antes pedia la ubicacion aqui, antes de dibujar nada, y solo
 * componia la interfaz cuando el usuario respondia. Eso hacia que
 * el mapa apareciera sin permiso ya denegado y que el dialogo se
 * pidiera dos veces (una aqui y otra en SmartBusMapScreen).
 *
 * Ahora la interfaz se monta siempre de inmediato. Los permisos los
 * pide SmartBusMapScreen en el momento en que el usuario abre el mapa,
 * que es donde el dialogo tiene sentido y donde ya estan los botones
 * para reintentar o ir a Ajustes.
 */
class MainActivity : ComponentActivity() {

    private var isContentSet = false

    /**
     * Las notificaciones solo hacen falta si el seguimiento GPS queda
     * activo, asi que se piden en ese momento y no al arrancar.
     */
    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                startLocationTracking()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.decorView.setBackgroundColor(android.graphics.Color.parseColor("#F4F4F6"))
        super.onCreate(savedInstanceState)

        handleNavigationIntent(intent)

        try {
            applySettings()
        } catch (e: Exception) {
            Log.e("SmartBus", "Error al aplicar configuraciones iniciales: ${e.message}", e)
        }

        setContent {
            SmartBusTheme {
                SmartBusNavigation()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNavigationIntent(intent)
    }

    private fun handleNavigationIntent(intent: Intent?) {
        val target = intent?.getStringExtra(SmartBusNotificationManager.EXTRA_NAVIGATE_TO)
        if (!target.isNullOrBlank()) {
            PointsEventManager.requestNavigation(target)
            intent.removeExtra(SmartBusNotificationManager.EXTRA_NAVIGATE_TO)
        }
    }

    /**
     * Aplica las preferencias guardadas en Configuracion. El
     * seguimiento en segundo plano solo arranca si el permiso ya esta
     * concedido: si no, el mapa lo pide y se reintenta desde ahi.
     */
    private fun applySettings() {

        val settings = SettingsRepository(this)

        AppPalette.setAccent(settings.accentColorId)

        if (settings.autoTracking) {
            ensureNotificationPermissionThenTrack()
        }
    }

    private fun ensureNotificationPermissionThenTrack() {

        if (!hasLocationPermission()) {
            return
        }

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
            return
        }

        startLocationTracking()
    }

    private fun hasLocationPermission(): Boolean {

        val fine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }

    private fun startLocationTracking() {

        if (!hasLocationPermission()) {
            return
        }

        try {
            TrackingManager(this).startTracking()
        } catch (exception: Exception) {
            Log.w(
                "SmartBus",
                "No se pudo iniciar el seguimiento GPS: ${exception.message}"
            )
        }
    }

    private fun stopTracking() {

        try {
            TrackingManager(this).stopTracking()
        } catch (exception: Exception) {
            Log.w(
                "SmartBus",
                "No se pudo detener el seguimiento GPS: ${exception.message}"
            )
        }
    }
}