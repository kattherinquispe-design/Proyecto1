package com.ejemplo.smartbus.data

import android.content.Context
import android.content.SharedPreferences
import com.ejemplo.smartbus.data.model.PointTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import org.json.JSONArray
import org.json.JSONObject

class PointsRepository(context: Context) {

    private val appContext = context.applicationContext

    private var explicitAccountId: String? = null

    private var preferences: SharedPreferences =
        appContext.getSharedPreferences(
            preferencesName(SessionManager.get(appContext).session.value?.storageKey),
            Context.MODE_PRIVATE
        )

    /**
     * Sincroniza siempre las preferencias con la cuenta activa de
     * SessionManager para que tanto el servicio en segundo plano
     * (LocationTrackingService) como la UI lean y escriban exactamente
     * el mismo saldo de puntos.
     */
    private fun syncActivePrefs(): SharedPreferences {
        val sessionKey = SessionManager.get(appContext).session.value?.storageKey
        val effectiveKey = explicitAccountId ?: sessionKey
        val targetName = preferencesName(effectiveKey)
        val targetPrefs = appContext.getSharedPreferences(targetName, Context.MODE_PRIVATE)

        // Si el usuario tiene cuenta activa y habia puntos acumulados en el archivo anonimo,
        // migrarlos automaticamente para que no se pierdan.
        if (!effectiveKey.isNullOrBlank()) {
            val anonPrefs = appContext.getSharedPreferences("smartbus_points", Context.MODE_PRIVATE)
            val anonPoints = anonPrefs.getInt(KEY_POINTS, 0)
            if (anonPoints > 0) {
                val currentAccountPoints = targetPrefs.getInt(KEY_POINTS, 0)
                val merged = currentAccountPoints + anonPoints
                targetPrefs.edit().putInt(KEY_POINTS, merged).apply()
                anonPrefs.edit().putInt(KEY_POINTS, 0).apply()
            }
        }

        preferences = targetPrefs
        return targetPrefs
    }

    /**
     * Los puntos son por cuenta. Antes eran globales: dos personas que
     * usaban el telefono compartian el mismo saldo.
     */
    fun useAccount(accountId: String?) {
        explicitAccountId = accountId
        syncActivePrefs()
        syncPointsFromCloudIfNeeded()
    }

    private fun preferencesName(accountId: String?): String =
        if (accountId.isNullOrBlank()) {
            "smartbus_points"
        } else {
            "smartbus_points_$accountId"
        }

    /**
     * Sincroniza el saldo de puntos actual hacia la coleccion 'usuarios' de Firebase Firestore.
     */
    private fun syncPointsToFirestore(totalPoints: Int) {
        try {
            val session = SessionManager.get(appContext).session.value
            val authUid = runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull()
            val email = session?.email.orEmpty().trim().lowercase()
            val uid = when {
                !authUid.isNullOrBlank() -> authUid
                email.isNotBlank() -> email.replace("@", "_").replace(".", "_")
                session != null && session.userId.isNotBlank() -> session.userId
                else -> return
            }

            val data = hashMapOf<String, Any>(
                "puntos" to totalPoints,
                "updatedAt" to System.currentTimeMillis()
            )
            if (email.isNotBlank()) {
                data["correo"] = email
            }
            if (session != null && session.name.isNotBlank()) {
                data["nombre"] = session.name
            }

            FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(uid)
                .set(data, SetOptions.merge())
        } catch (_: Exception) {
        }
    }

    /**
     * Recupera los puntos guardados en Firestore ('usuarios') cuando el saldo local
     * aun no fue inicializado (por ejemplo al iniciar sesion en un telefono limpio).
     */
    fun syncPointsFromCloudIfNeeded() {
        try {
            val session = SessionManager.get(appContext).session.value ?: return
            if (session.isGuest) return
            val authUid = runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull()
            val email = session.email.trim().lowercase()
            val uid = when {
                !authUid.isNullOrBlank() -> authUid
                email.isNotBlank() -> email.replace("@", "_").replace(".", "_")
                session.userId.isNotBlank() -> session.userId
                else -> return
            }

            FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc != null && doc.exists()) {
                        val cloudPoints = (doc.getLong("puntos") ?: doc.getLong("points"))?.toInt() ?: return@addOnSuccessListener
                        val prefs = syncActivePrefs()
                        val localPoints = prefs.getInt(KEY_POINTS, 0)
                        val hasLocalHistory = !prefs.getString(KEY_HISTORY, null).isNullOrBlank()
                        if (localPoints == 0 && !hasLocalHistory && cloudPoints > 0) {
                            prefs.edit().putInt(KEY_POINTS, cloudPoints).apply()
                            PointsEventManager.notifyPointsAdded(cloudPoints)
                        } else if (localPoints > 0 && cloudPoints != localPoints) {
                            syncPointsToFirestore(localPoints)
                        }
                    }
                }
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val KEY_POINTS = "total_points"
        private const val KEY_HISTORY = "points_history"
        private const val KEY_REDEEMED = "redeemed_rewards"
    }

    /**
     * Devuelve el codigo de canje activo de una recompensa.
     * Si el voucher anterior ya fue marcado como usado, permite volver a canjear.
     */
    fun getRedemptionCode(rewardId: Int): String? {
        val prefs = syncActivePrefs()
        val json = prefs.getString(KEY_REDEEMED, null) ?: return null
        return try {
            val obj = JSONObject(json)
            if (!obj.has(rewardId.toString())) return null
            val value = obj.get(rewardId.toString())
            if (value is JSONObject) {
                if (value.optBoolean("isUsed", false)) null else value.optString("code")
            } else {
                value.toString()
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * True si la recompensa tiene un canje activo pendiente de usar.
     */
    fun isRewardRedeemed(rewardId: Int): Boolean = getRedemptionCode(rewardId) != null

    /**
     * Marca una recompensa como canjeada para bloquear su reutilizacion.
     */
    private fun markRewardRedeemed(
        rewardId: Int,
        code: String,
        isUsed: Boolean = false,
        usedAt: Long? = null,
        redeemedAt: Long = System.currentTimeMillis()
    ) {
        val prefs = syncActivePrefs()
        val obj = try {
            JSONObject(prefs.getString(KEY_REDEEMED, null) ?: "{}")
        } catch (_: Exception) {
            JSONObject()
        }
        val entry = JSONObject().apply {
            put("code", code)
            put("isUsed", isUsed)
            if (usedAt != null) put("usedAt", usedAt)
            put("redeemedAt", redeemedAt)
        }
        obj.put(rewardId.toString(), entry)
        prefs.edit().putString(KEY_REDEEMED, obj.toString()).commit()
    }

    /**
     * Confirma y consume un canje (1 solo uso). Una vez confirmado, no puede volver a usarse.
     */
    fun markRedemptionAsUsed(code: String): Boolean {
        val prefs = syncActivePrefs()
        val json = prefs.getString(KEY_REDEEMED, null) ?: return false
        return try {
            val obj = JSONObject(json)
            var matchedKey: String? = null
            var matchedObj: JSONObject? = null

            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = obj.get(key)
                if (value is JSONObject && value.optString("code") == code) {
                    matchedKey = key
                    matchedObj = value
                    break
                } else if (value.toString() == code) {
                    matchedKey = key
                    matchedObj = JSONObject().apply {
                        put("code", code)
                        put("redeemedAt", System.currentTimeMillis())
                    }
                    break
                }
            }

            if (matchedKey != null && matchedObj != null) {
                if (matchedObj.optBoolean("isUsed", false)) {
                    return false // Ya fue usado previamente
                }
                val now = System.currentTimeMillis()
                matchedObj.put("isUsed", true)
                matchedObj.put("usedAt", now)
                obj.put(matchedKey, matchedObj)
                prefs.edit().putString(KEY_REDEEMED, obj.toString()).commit()

                // Sincronizar en Firebase Firestore
                try {
                    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    firestore.collection("canjes").document(code).update(
                        mapOf(
                            "status" to "USADO",
                            "usedAt" to now
                        )
                    )
                } catch (_: Exception) {
                }
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Devuelve los ids de recompensas con canje activo.
     */
    fun getRedeemedRewardIds(): Set<Int> {
        val prefs = syncActivePrefs()
        val json = prefs.getString(KEY_REDEEMED, null) ?: return emptySet()
        return try {
            val obj = JSONObject(json)
            val ids = mutableSetOf<Int>()
            obj.keys().forEach { key ->
                val id = key.toIntOrNull() ?: 0
                if (id != 0) {
                    val value = obj.get(key)
                    val isUsed = value is JSONObject && value.optBoolean("isUsed", false)
                    if (!isUsed) {
                        ids.add(id)
                    }
                }
            }
            ids
        } catch (_: Exception) {
            emptySet()
        }
    }

    /**
     * Devuelve el detalle de las recompensas ya canjeadas:
     * id, nombre, costo, codigo de canje y si ya fue consumido.
     */
    fun getRedeemedRewards(): List<RedeemedReward> {
        val prefs = syncActivePrefs()
        val json = prefs.getString(KEY_REDEEMED, null) ?: return emptyList()
        return try {
            val obj = JSONObject(json)
            val result = mutableListOf<RedeemedReward>()
            obj.keys().forEach { key ->
                val id = key.toIntOrNull() ?: return@forEach
                if (id == 0) return@forEach
                val reward = RewardsCatalog.byId(id)
                val value = obj.get(key)
                val code: String
                val isUsed: Boolean
                val usedAt: Long?
                val redeemedAt: Long

                if (value is JSONObject) {
                    code = value.optString("code")
                    isUsed = value.optBoolean("isUsed", false)
                    usedAt = if (value.has("usedAt") && !value.isNull("usedAt")) value.getLong("usedAt") else null
                    redeemedAt = value.optLong("redeemedAt", System.currentTimeMillis())
                } else {
                    code = value.toString()
                    isUsed = false
                    usedAt = null
                    redeemedAt = System.currentTimeMillis()
                }

                result.add(
                    RedeemedReward(
                        id = id,
                        name = reward?.name ?: "Recompensa $id",
                        points = reward?.points ?: 0,
                        code = code,
                        isUsed = isUsed,
                        usedAt = usedAt,
                        redeemedAt = redeemedAt
                    )
                )
            }
            result.sortedByDescending { it.redeemedAt }
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
            val randomSuffix = (1000..9999).random()
            code = "MISTIGO-CANJE-$rewardId-" + System.currentTimeMillis().toString().takeLast(4) + "-" + randomSuffix
        } while (isCodeUsed(code))
        return code
    }

    private fun isCodeUsed(code: String): Boolean {
        val json = preferences.getString(KEY_REDEEMED, null) ?: return false
        return try {
            val obj = JSONObject(json)
            obj.keys().forEach { key ->
                val value = obj.get(key)
                val existingCode = if (value is JSONObject) value.optString("code") else value.toString()
                if (existingCode == code) return true
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

        // Registrar en Firebase Firestore para auditoría y validación en la nube
        try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            firestore.collection("canjes").document(code).set(
                mapOf(
                    "code" to code,
                    "rewardId" to rewardId,
                    "rewardName" to rewardName,
                    "pointsCost" to points,
                    "status" to "ACTIVO",
                    "redeemedAt" to System.currentTimeMillis(),
                    "usedAt" to null
                )
            )
        } catch (_: Exception) {
        }

        return code
    }

    /**
     * Devuelve el total actual de puntos.
     */
    fun getPoints(): Int {
        return syncActivePrefs().getInt(KEY_POINTS, 0)
    }

    /**
     * Agrega puntos después de validar un abordaje (por defecto 20 puntos por viaje en paradero).
     */
    fun addBoardingPoints(
        stopId: Int = 0,
        stopName: String = "",
        routeId: Int? = null,
        busId: Int? = null,
        points: Int = 20,
        reason: String = "Abordaje validado",
        evidence: String? = null
    ) {

        val prefs = syncActivePrefs()
        val currentPoints = prefs.getInt(KEY_POINTS, 0)

        val newTotal = currentPoints + points

        prefs
            .edit()
            .putInt(KEY_POINTS, newTotal)
            .commit()

        addTransaction(
            points = points,
            stopId = stopId,
            stopName = stopName,
            routeId = routeId,
            busId = busId,
            reason = reason,
            evidence = evidence
        )

        syncPointsToFirestore(newTotal)
        PointsEventManager.notifyPointsAdded(newTotal)
    }

    /**
     * Resta puntos inmediatamente cuando el usuario canjea una recompensa.
     */
    fun redeemPoints(
        points: Int,
        rewardName: String,
        rewardId: Int = 0,
        redemptionCode: String? = null
    ): Boolean {

        val prefs = syncActivePrefs()
        val currentPoints = prefs.getInt(KEY_POINTS, 0)

        if (currentPoints < points) {
            return false
        }

        val newTotal = (currentPoints - points).coerceAtLeast(0)

        prefs
            .edit()
            .putInt(KEY_POINTS, newTotal)
            .commit()

        addTransaction(
            points = -points,
            stopId = 0,
            stopName = rewardName,
            reason = if (redemptionCode.isNullOrEmpty()) "Canje de recompensa" else "Canje de recompensa (Código: $redemptionCode)",
            evidence = redemptionCode
        )

        syncPointsToFirestore(newTotal)
        PointsEventManager.notifyPointsAdded(newTotal)

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

        val prefs = syncActivePrefs()
        prefs
            .edit()
            .putString(
                KEY_HISTORY,
                jsonArray.toString()
            )
            .commit()
    }

    /**
     * Devuelve todo el historial.
     */
    fun getHistory(): List<PointTransaction> {

        val prefs = syncActivePrefs()
        val jsonString =
            prefs.getString(
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

        syncActivePrefs()
            .edit()
            .clear()
            .commit()
        syncPointsToFirestore(0)
        PointsEventManager.notifyPointsAdded(0)
    }
}

/**
 * Detalle de una recompensa ya canjeada.
 */
data class RedeemedReward(
    val id: Int,
    val name: String,
    val points: Int,
    val code: String,
    val isUsed: Boolean = false,
    val usedAt: Long? = null,
    val redeemedAt: Long = System.currentTimeMillis()
)
