package com.ejemplo.smartbus.data

import android.content.Context

class BoardingRewardManager(
    context: Context
) {

    companion object {

        private const val PREFS_NAME =
            "smartbus_boarding_rewards"

        private const val KEY_LAST_STOP_ID =
            "last_rewarded_stop_id"

        private const val KEY_LAST_REWARD_TIME =
            "last_reward_time"

        private const val MIN_TIME_BETWEEN_REWARDS =
            5 * 60 * 1000L
    }

    private val preferences =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    /**
     * Determina si se puede entregar
     * una nueva recompensa.
     */
    fun canReward(
        stopId: Int
    ): Boolean {

        val lastStopId =
            preferences.getInt(
                KEY_LAST_STOP_ID,
                -1
            )

        val lastRewardTime =
            preferences.getLong(
                KEY_LAST_REWARD_TIME,
                0L
            )

        val currentTime =
            System.currentTimeMillis()

        val elapsed =
            currentTime - lastRewardTime

        // Control anti-fraude: tiempo mínimo entre puntos
        if (elapsed < MIN_TIME_BETWEEN_REWARDS) {
            return false
        }

        // Evitar repetir mismo paradero en ventana corta
        if (
            lastStopId == stopId &&
            elapsed < MIN_TIME_BETWEEN_REWARDS * 2
        ) {

            return false
        }

        return true
    }

    /**
     * Registra que el abordaje
     * ya fue recompensado.
     */
    fun markRewarded(
        stopId: Int
    ) {

        preferences.edit()
            .putInt(
                KEY_LAST_STOP_ID,
                stopId
            )
            .putLong(
                KEY_LAST_REWARD_TIME,
                System.currentTimeMillis()
            )
            .apply()
    }
}