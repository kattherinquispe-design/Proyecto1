package com.ejemplo.smartbus.data

import android.content.Context
import com.ejemplo.smartbus.data.model.PointTransaction
import org.json.JSONArray
import org.json.JSONObject

class PointsRepository(context: Context) {

    private val appContext = context.applicationContext

    private var preferences =
        appContext.getSharedPreferences(
            preferencesName(null),
            Context.MODE_PRIVATE
        )

    /**
     * Los puntos son por cuenta. Antes eran globales: dos personas que
     * usaban el telefono compartian el mismo saldo.
     */
    fun useAccount(accountId: String?) {
        preferences =
            appContext.getSharedPreferences(
                preferencesName(accountId),
                Context.MODE_PRIVATE
            )
    }

    private fun preferencesName(accountId: String?): String =
        if (accountId.isNullOrBlank()) {
            "smartbus_points"
        } else {
            "smartbus_points_$accountId"
        }

    companion object {
        private const val KEY_POINTS = "total_points"
        private const val KEY_HISTORY = "points_history"
        private const val KEY_REDEEMED = "redeemed_rewards"
    }

    /**
     * Devuelve el codigo de canje de una recompensa ya utilizada.
     * Sirve para impedir que un mismo canje se reutilice.
     */
    fun getRedemptionCode(rewardId: Int): String? {
        val json = preferences.getString(KEY_REDEEMED, null) ?: return null
        return try {
            val obj = JSONObject(json)
            if (obj.has(rewardId.toString())) obj.getString(rewardId.toString()) else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * True si la recompensa ya fue canjeada alguna vez.
     */
    fun isRewardRedeemed(rewardId: Int): Boolean = getRedemptionCode(rewardId) != null

    /**
     * Marca una recompensa como canjeada para bloquear su reutilizacion.
     */
    private fun markRewardRedeemed(rewardId: Int, code: String) {
        val obj = try {
            JSONObject(preferences.getString(KEY_REDEEMED, null) ?: "{}")
        } catch (_: Exception) {
            JSONObject()
        }
        obj.put(rewardId.toString(), code)
        preferences.edit().putString(KEY_REDEEMED, obj.toString()).apply()
    }

    /**
     * Devuelve los ids de recompensas ya canjeadas.
     */
    fun getRedeemedRewardIds(): Set<Int> {
        val json = preferences.getString(KEY_REDEEMED, null) ?: return emptySet()
        return try {
            val obj = JSONObject(json)
            val ids = mutableSetOf<Int>()
            obj.keys().forEach { key -> ids.add(key.toIntOrNull() ?: 0) }
            ids.filter { it != 0 }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    /**
     * Devuelve el detalle de las recompensas ya canjeadas:
     * id, nombre, costo y codigo de canje.
     */
    fun getRedeemedRewards(): List<RedeemedReward> {
        val json = preferences.getString(KEY_REDEEMED, null) ?: return emptyList()
        return try {
            val obj = JSONObject(json)
            val result = mutableListOf<RedeemedReward>()
            obj.keys().forEach { key ->
                val id = key.toIntOrNull() ?: return@forEach
                if (id == 0) return@forEach
                val reward = RewardsCatalog.byId(id)
                result.add(
                    RedeemedReward(
                        id = id,
                        name = reward?.name ?: "Recompensa $id",
                        points = reward?.points ?: 0,
                        code = obj.optString(key)
                    )
                )
            }
            result.sortedBy { it.id }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Genera un codigo de canje unico, verificando que no exista otro igual.
     */
    fun generateRedemptionCode(rewardId: Int): String {
        var code: String
        do {
            code = "SB-${rewardId}-${System.currentTimeMillis().toString().takeLast(6)}"
        } while (isCodeUsed(code))
        return code
    }

    private fun isCodeUsed(code: String): Boolean {
        val json = preferences.getString(KEY_REDEEMED, null) ?: return false
        return try {
            val obj = JSONObject(json)
            obj.keys().forEach { key ->
                if (obj.optString(key) == code) return true
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Canjea una recompensa. Los puntos se descuentan una sola vez:
     * si la recompensa ya fue utilizada devuelve false y no descuenta nada.
     */
    fun redeemRewardOnce(rewardId: Int, rewardName: String, points: Int): String? {
        if (isRewardRedeemed(rewardId)) return null
        if (getPoints() < points) return null

        val code = generateRedemptionCode(rewardId)
        val charged = redeemPoints(
            points = points,
            rewardName = rewardName,
            redemptionCode = code
        )

        if (!charged) return null

        markRewardRedeemed(rewardId, code)
        return code
    }

    /**
     * Devuelve el total actual de puntos.
     */
    fun getPoints(): Int {
        return preferences.getInt(KEY_POINTS, 0)
    }

    /**
     * Agrega puntos después de validar un abordaje.
     */
    fun addBoardingPoints(
        stopId: Int = 0,
        stopName: String = "",
        routeId: Int? = null,
        busId: Int? = null,
        points: Int = 50,
        reason: String = "Abordaje validado",
        evidence: String? = null
    ) {

        val currentPoints = getPoints()

        val newTotal = currentPoints + points

        preferences
            .edit()
            .putInt(KEY_POINTS, newTotal)
            .apply()

        addTransaction(
            points = points,
            stopId = stopId,
            stopName = stopName,
            routeId = routeId,
            busId = busId,
            reason = reason,
            evidence = evidence
        )

        PointsEventManager.notifyPointsAdded(newTotal)
    }

    /**
     * Resta puntos cuando el usuario canjea una recompensa.
     */
    fun redeemPoints(
        points: Int,
        rewardName: String,
        rewardId: Int = 0,
        redemptionCode: String? = null
    ): Boolean {

        val currentPoints = getPoints()

        if (currentPoints < points) {
            return false
        }

        val newTotal = currentPoints - points

        preferences
            .edit()
            .putInt(KEY_POINTS, newTotal)
            .apply()

        addTransaction(
            points = -points,
            stopId = 0,
            stopName = rewardName,
            reason = if (redemptionCode.isNullOrEmpty()) "Canje de recompensa" else "Canje de recompensa (Código: $redemptionCode)",
            evidence = redemptionCode
        )

        return true
    }

    /**
     * Guarda una operación en el historial.
     */
    private fun addTransaction(
        points: Int,
        stopId: Int = 0,
        stopName: String = "",
        routeId: Int? = null,
        busId: Int? = null,
        reason: String = "",
        evidence: String? = null
    ) {

        val history = getHistory().toMutableList()

        val transaction = PointTransaction(
            id = System.currentTimeMillis(),
            points = points,
            stopId = stopId,
            stopName = stopName,
            routeId = routeId,
            busId = busId,
            reason = reason,
            evidence = evidence,
            timestamp = System.currentTimeMillis()
        )

        history.add(0, transaction)

        val jsonArray = JSONArray()

        history.forEach { item ->

            val jsonObject = JSONObject()

            jsonObject.put("id", item.id)
            jsonObject.put("points", item.points)
            jsonObject.put("stopId", item.stopId)
            jsonObject.put("stopName", item.stopName)
            if (item.routeId != null) jsonObject.put("routeId", item.routeId) else jsonObject.put("routeId", JSONObject.NULL)
            if (item.busId != null) jsonObject.put("busId", item.busId) else jsonObject.put("busId", JSONObject.NULL)
            jsonObject.put("reason", item.reason)
            if (item.evidence != null) jsonObject.put("evidence", item.evidence) else jsonObject.put("evidence", JSONObject.NULL)
            jsonObject.put("timestamp", item.timestamp)

            jsonArray.put(jsonObject)
        }

        preferences
            .edit()
            .putString(
                KEY_HISTORY,
                jsonArray.toString()
            )
            .apply()
    }

    /**
     * Devuelve todo el historial.
     */
    fun getHistory(): List<PointTransaction> {

        val jsonString =
            preferences.getString(
                KEY_HISTORY,
                null
            )
                ?: return emptyList()

        return try {

            val jsonArray =
                JSONArray(jsonString)

            val history =
                mutableListOf<PointTransaction>()

            for (i in 0 until jsonArray.length()) {

                val item =
                    jsonArray.getJSONObject(i)

                history.add(
                    PointTransaction(
                        id = item.getLong("id"),
                        points = item.getInt("points"),
                        stopId = item.optInt("stopId", 0),
                        stopName = item.optString("stopName", ""),
                        routeId = if (item.has("routeId") && !item.isNull("routeId")) item.getInt("routeId") else null,
                        busId = if (item.has("busId") && !item.isNull("busId")) item.getInt("busId") else null,
                        reason = item.optString("reason", ""),
                        evidence = if (item.has("evidence") && !item.isNull("evidence")) item.getString("evidence") else null,
                        timestamp = item.getLong("timestamp")
                    )
                )
            }

            history

        } catch (exception: Exception) {

            emptyList()
        }
    }

    /**
     * Elimina todos los puntos.
     * Útil durante las pruebas.
     */
    fun clearPoints() {

        preferences
            .edit()
            .clear()
            .apply()
    }
}

/**
 * Detalle de una recompensa ya canjeada.
 */
data class RedeemedReward(
    val id: Int,
    val name: String,
    val points: Int,
    val code: String
)
