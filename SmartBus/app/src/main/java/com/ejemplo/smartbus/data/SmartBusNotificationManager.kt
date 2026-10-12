package com.ejemplo.smartbus.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat

class SmartBusNotificationManager(
    private val context: Context
) {

    companion object {

        private const val CHANNEL_ID =
            "smartbus_events"

        private const val CHANNEL_NAME =
            "Eventos SmartBus"

        private const val ARRIVAL_ID =
            2001

        private const val BOARDING_ID =
            2002

        private const val ETA_ID =
            2003

        private const val ALERT_ID =
            2004

        private const val SAVE_POINTS_ID =
            2005
    }

    init {
        createChannel()
    }

    private fun createChannel() {

        val manager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        val channel =
            NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager
                    .IMPORTANCE_HIGH
            )

        channel.description =
            "Notificaciones de SmartBus"

        manager.createNotificationChannel(
            channel
        )
    }

    private fun canNotify(): Boolean {

        return if (
            android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.TIRAMISU
        ) {

            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

        } else {

            true
        }
    }

    fun showArrivalAtStop(
        stopName: String
    ) {

        if (!canNotify()) return

        notify(
            id = ARRIVAL_ID,

            title =
                "Paradero relacionado Llegaste a tu paradero",

            message =
                "Estás en $stopName. SmartBus está verificando tu abordaje."
        )
    }

    fun showBoardingValidated(
        stopName: String,
        points: Int
    ) {

        if (!canNotify()) return

        notify(
            id = BOARDING_ID,

            title =
                "Abordaje validado",

            message =
                "Ganaste +$points puntos en $stopName."
        )
    }

    fun showPointsEarned(points: Int, stopName: String) {
        if (!canNotify()) return
        notify(BOARDING_ID, "¡Puntos obtenidos!", "+$points puntos en $stopName")
    }

    fun showPointsMilestone(points: Int) {
        if (!canNotify()) return
        notify(ALERT_ID + 10, "¡Felicidades!", "Has alcanzado $points puntos")
    }

    /**
     * Avisa a quien entra como invitado de que sus puntos viven solo en
     * este telefono. Al iniciar sesion quedan atados a su cuenta y no se
     * pierden si cambia de dispositivo o desinstala la app.
     */
    fun showSignInToSavePoints(points: Int) {

        if (!canNotify()) return

        notify(
            id = SAVE_POINTS_ID,

            title =
                "Guarda tus puntos",

            message =
                "Tienes $points puntos. Inicia sesion para guardarlos " +
                    "de forma segura y no perderlos."
        )
    }

    fun showRewardAvailable(rewardName: String) {
        if (!canNotify()) return
        notify(ALERT_ID + 11, "Recompensa disponible", "$rewardName te está esperando")
    }

    fun showBusEta(
        busCode: String,
        etaMinutes: Int
    ) {

        if (!canNotify()) return

        val message =
            if (etaMinutes <= 0) {

                "Ruta $busCode está llegando."

            } else {

                "Ruta $busCode llega aproximadamente en $etaMinutes min."
            }

        notify(
            id = ETA_ID,

            title =
                "Cada Tu bus",

            message =
                message
        )
    }

    fun showServiceAlert(
        title: String,
        message: String
    ) {

        if (!canNotify()) return

        notify(
            id = ALERT_ID,

            title =
                "Reportar problema $title",

            message =
                message
        )
    }

    private fun notify(
        id: Int,
        title: String,
        message: String
    ) {

        val notification =
            NotificationCompat
                .Builder(
                    context,
                    CHANNEL_ID
                )
                .setSmallIcon(
                    android.R.drawable
                        .ic_menu_mylocation
                )
                .setContentTitle(
                    title
                )
                .setContentText(
                    message
                )
                .setPriority(
                    NotificationCompat
                        .PRIORITY_HIGH
                )
                .setAutoCancel(true)
                .build()

        val manager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        manager.notify(
            id,
            notification
        )
    }
}