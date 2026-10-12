package com.ejemplo.smartbus.location

import android.location.Location

/**
 * Calidad de una lectura de GPS.
 */
enum class FixQuality {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    STALE
}

/**
 * Resultado de evaluar una lectura de GPS.
 */
data class FixDecision(
    val accept: Boolean,
    val quality: FixQuality,
    val reason: String
)

/**
 * Decide que lecturas de GPS se pueden mostrar.
 *
 * Regla principal: la app NUNCA debe quedarse congelada en una
 * posicion falsa. Antes pasaba esto: se aceptaba la posicion cacheada
 * del sistema (que puede ser de hace horas y de otro barrio) y luego
 * el fix real se descartaba por estar "muy lejos", dejando al usuario
 * clavado en un lugar donde nunca estuvo.
 *
 * Aqui la posicion cacheada solo sirve para arrancar y caduca a los
 * 30 segundos. El filtro de saltos existe para suavizar el temblor
 * del GPS, no para rechazar la posicion real, y solo funciona dentro
 * de una ventana de 20 segundos.
 */
object LocationFixEvaluator {

    /** Precisiones mayores que esto son basura del proveedor. */
    const val MAX_USABLE_ACCURACY_METERS = 2_000f

    /** La posicion cacheada del sistema solo vale 30 segundos. */
    const val CACHED_FIX_MAX_AGE_MS = 30_000L

    /** Ventana en la que se suaviza el temblor del GPS. */
    const val JITTER_WINDOW_MS = 20_000L

    /** Salto maximo admitido dentro de la ventana de temblor. */
    const val JITTER_TOLERANCE_METERS = 120f

    /** Un fix peor que esto dentro de la ventana se considera ruido. */
    const val JITTER_MIN_ACCURACY_METERS = 100f

    fun evaluate(
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float,
        elapsedRealtimeMs: Long,
        fixElapsedRealtimeMs: Long,
        currentLatitude: Double? = null,
        currentLongitude: Double? = null,
        currentAcceptedAtElapsedMs: Long = 0L,
        isCachedHint: Boolean = false
    ): FixDecision {

        // 1. Lectura inutil: el proveedor no sabe ni donde estamos.
        if (accuracyMeters <= 0f) {
            return FixDecision(
                accept = false,
                quality = FixQuality.POOR,
                reason = "El GPS no devolvio precision"
            )
        }

        if (accuracyMeters > MAX_USABLE_ACCURACY_METERS) {
            return FixDecision(
                accept = false,
                quality = FixQuality.POOR,
                reason = "Precision inutil (${accuracyMeters.toInt()} m)"
            )
        }

        // 2. La posicion cacheada solo sirve para arrancar la pantalla.
        if (isCachedHint) {
            val age = if (fixElapsedRealtimeMs > 0L) {
                elapsedRealtimeMs - fixElapsedRealtimeMs
            } else {
                0L
            }

            if (age > CACHED_FIX_MAX_AGE_MS) {
                return FixDecision(
                    accept = false,
                    quality = FixQuality.STALE,
                    reason = "Ubicacion cacheada antigua, esperando GPS real"
                )
            }
        }

        // 3. Suavizado del temblor: solo dentro de una ventana corta y
        //    solo si la lectura nueva es ademas imprecisa.
        if (
            currentLatitude != null &&
            currentLongitude != null &&
            elapsedRealtimeMs - currentAcceptedAtElapsedMs < JITTER_WINDOW_MS
        ) {
            val jump = distanceMeters(
                currentLatitude,
                currentLongitude,
                latitude,
                longitude
            )

            if (
                jump > JITTER_TOLERANCE_METERS &&
                accuracyMeters > JITTER_MIN_ACCURACY_METERS
            ) {
                return FixDecision(
                    accept = false,
                    quality = FixQuality.POOR,
                    reason = "Lectura incompleta, se mantiene la anterior"
                )
            }
        }

        return FixDecision(
            accept = true,
            quality = qualityOf(accuracyMeters),
            reason = "ok"
        )
    }

    fun qualityOf(accuracyMeters: Float): FixQuality = when {
        accuracyMeters <= 8f -> FixQuality.EXCELLENT
        accuracyMeters <= 20f -> FixQuality.GOOD
        accuracyMeters <= 60f -> FixQuality.FAIR
        else -> FixQuality.POOR
    }

    fun labelOf(quality: FixQuality): String = when (quality) {
        FixQuality.EXCELLENT -> "GPS: Muy preciso"
        FixQuality.GOOD -> "GPS: Preciso"
        FixQuality.FAIR -> "GPS: Aceptable"
        FixQuality.POOR -> "GPS: Baja precision"
        FixQuality.STALE -> "GPS: Buscando senal"
    }

    fun detailOf(quality: FixQuality, accuracyMeters: Float): String =
        if (accuracyMeters > 0f && accuracyMeters < MAX_USABLE_ACCURACY_METERS) {
            "+-${accuracyMeters.toInt()} m"
        } else {
            "sin senal"
        }
}

private fun distanceMeters(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double
): Float {
    val results = FloatArray(1)
    Location.distanceBetween(lat1, lon1, lat2, lon2, results)
    return results[0]
}