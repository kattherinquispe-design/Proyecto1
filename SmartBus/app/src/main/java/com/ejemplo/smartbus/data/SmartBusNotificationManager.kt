package com.ejemplo.smartbus.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.ejemplo.smartbus.MainActivity

class SmartBusNotificationManager(
    private val context: Context
) {

    companion object {

        private const val CHANNEL_ID =
            "mistigo_alerts_sound_v3"

        private const val CHANNEL_NAME =
            "Alertas y Puntos MistiGo"

        const val EXTRA_NAVIGATE_TO =
            "navigate_to"

        const val TARGET_REWARDS =
            "REWARDS"

        const val TARGET_MAP =
            "MAP"

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

        private const val STOP_COOLDOWN_MS =
            90_000L

        @Volatile
        private var lastNotifiedStopName: String? = null

        @Volatile
        private var lastNotifiedStopAt: Long = 0L
    }

    init {
        createChannel()
    }

    private fun createChannel() {

        val manager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel =
            NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de paraderos cercanos y puntos ganados en MistiGo"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
                enableLights(true)
                lightColor = Color.parseColor("#2563EB")
                setSound(soundUri, audioAttributes)
            }

        manager.createNotificationChannel(channel)
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

    private fun buildContentIntent(
        requestCode: Int,
        target: String
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_TO, target)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun showArrivalAtStop(
        stopName: String
    ) {

        if (!canNotify()) return

        val now = System.currentTimeMillis()
        if (lastNotifiedStopName == stopName && now - lastNotifiedStopAt < STOP_COOLDOWN_MS) {
            return
        }
        lastNotifiedStopName = stopName
        lastNotifiedStopAt = now

        val title = "🚏 ¡Estás en el paradero $stopName!"
        val shortMsg = "📍 Entraste al radio del paradero. ¡Aborda tu bus y gana +20 puntos!"
        val bigMsg = "📍 Te encuentras dentro del radio del paradero $stopName.\n" +
            "🚌 Toma tu bus MistiGo desde aquí para ganar +20 puntos automáticamente en tu viaje."

        notify(
            id = ARRIVAL_ID,
            title = title,
            message = shortMsg,
            bigText = bigMsg,
            subText = "MistiGo • Paradero activo",
            accentColor = Color.parseColor("#2563EB"),
            target = TARGET_MAP
        )
    }

    fun showBoardingValidated(
        stopName: String,
        points: Int = 20,
        totalPoints: Int? = null
    ) {

        if (!canNotify()) return

        val balanceText = if (totalPoints != null) " Tu saldo actual es de $totalPoints puntos." else ""
        val title = "🎉 ¡Recibiste +$points puntos en MistiGo!"
        val shortMsg = "✅ Viaje en bus desde $stopName. Toca para ver tus puntos."
        val bigMsg = "✅ ¡Viaje validado desde el paradero $stopName!\n" +
            "⭐ Has ganado +$points puntos por viajar en bus.$balanceText\n" +
            "🎁 Toca esta notificación para abrir MistiGo y ver tus puntos."

        notify(
            id = BOARDING_ID,
            title = title,
            message = shortMsg,
            bigText = bigMsg,
            subText = "MistiGo • +$points Puntos",
            accentColor = Color.parseColor("#10B981"),
            target = TARGET_REWARDS,
            actionLabel = "Ver mis puntos 🎁"
        )
    }

    fun showPointsEarned(points: Int, stopName: String) {
        showBoardingValidated(stopName = stopName, points = points)
    }

    fun showPointsMilestone(points: Int) {
        if (!canNotify()) return
        notify(
            id = ALERT_ID + 10,
            title = "🏆 ¡Nuevo logro de puntos!",
            message = "Has alcanzado $points puntos en MistiGo. Toca para ver tus recompensas.",
            bigText = "🏆 ¡Felicidades! Ya acumulaste $points puntos en MistiGo.\n🎁 Toca aquí para ver tus puntos y canjear premios.",
            subText = "MistiGo • Recompensas",
            accentColor = Color.parseColor("#F59E0B"),
            target = TARGET_REWARDS,
            actionLabel = "Ver recompensas"
        )
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
            title = "💾 Guarda tus $points puntos",
            message = "Tienes $points puntos acumulados. Inicia sesión para respaldarlos en tu cuenta.",
            bigText = "Tienes $points puntos acumulados en MistiGo.\n🔒 Inicia sesión con tu correo o Google para guardarlos de forma segura y no perderlos.",
            subText = "MistiGo • Cuenta",
            accentColor = Color.parseColor("#2563EB"),
            target = TARGET_REWARDS
        )
    }

    fun showRewardAvailable(rewardName: String) {
        if (!canNotify()) return
        notify(
            id = ALERT_ID + 11,
            title = "🎁 ¡Recompensa disponible!",
            message = "$rewardName ya está disponible para canjear con tus puntos.",
            subText = "MistiGo • Canjes",
            accentColor = Color.parseColor("#10B981"),
            target = TARGET_REWARDS
        )
    }

    fun showBusEta(
        busCode: String,
        etaMinutes: Int
    ) {

        if (!canNotify()) return

        val message =
            if (etaMinutes <= 0) {
                "🚍 La unidad $busCode está llegando a tu paradero."
            } else {
                "🚍 La unidad $busCode llega aproximadamente en $etaMinutes min."
            }

        notify(
            id = ETA_ID,
            title = "🚌 Tu bus MistiGo se acerca",
            message = message,
            subText = "MistiGo • Tiempo real",
            accentColor = Color.parseColor("#2563EB"),
            target = TARGET_MAP
        )
    }

    fun showServiceAlert(
        title: String,
        message: String
    ) {

        if (!canNotify()) return

        notify(
            id = ALERT_ID,
            title = "⚠️ Alerta de servicio: $title",
            message = message,
            subText = "MistiGo • Avisos",
            accentColor = Color.parseColor("#EF4444"),
            target = TARGET_MAP
        )
    }

    private fun notify(
        id: Int,
        title: String,
        message: String,
        bigText: String = message,
        subText: String = "MistiGo",
        accentColor: Int = Color.parseColor("#2563EB"),
        target: String = TARGET_MAP,
        actionLabel: String? = null
    ) {

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val contentIntent = buildContentIntent(id, target)

        val builder =
            NotificationCompat
                .Builder(
                    context,
                    CHANNEL_ID
                )
                .setSmallIcon(
                    android.R.drawable.ic_menu_mylocation
                )
                .setContentTitle(title)
                .setContentText(message)
                .setSubText(subText)
                .setColor(accentColor)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(bigText)
                        .setBigContentTitle(title)
                        .setSummaryText(subText)
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSound(soundUri)
                .setVibrate(longArrayOf(0, 300, 150, 300))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)

        if (!actionLabel.isNullOrBlank()) {
            builder.addAction(
                android.R.drawable.ic_menu_view,
                actionLabel,
                contentIntent
            )
        }

        val manager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        manager.notify(
            id,
            builder.build()
        )
    }
}