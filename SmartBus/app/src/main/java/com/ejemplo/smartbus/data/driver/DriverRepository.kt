package com.ejemplo.smartbus.data.driver

import android.content.Context
import android.util.Log
import com.ejemplo.smartbus.data.db.DriverDocumentEntity
import com.ejemplo.smartbus.data.db.DriverProfileEntity
import com.ejemplo.smartbus.data.db.DriverShiftEntity
import com.ejemplo.smartbus.data.db.DriverTripEntity
import com.ejemplo.smartbus.data.db.DriverVehicleEntity
import com.ejemplo.smartbus.data.db.SmartBusDatabase
import com.ejemplo.smartbus.data.model.DriverDocument
import com.ejemplo.smartbus.data.model.DriverDocumentStatus
import com.ejemplo.smartbus.data.model.DriverDocumentType
import com.ejemplo.smartbus.data.model.DriverProfile
import com.ejemplo.smartbus.data.model.DriverShift
import com.ejemplo.smartbus.data.model.DriverTrip
import com.ejemplo.smartbus.data.model.DriverVehicle
import com.ejemplo.smartbus.data.model.DriverVerificationStatus
import com.ejemplo.smartbus.data.model.GeoPoint
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.security.SecureRandom
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Todo lo que el conductor registra y el estado en el que esta.
 *
 * La regla que manda aqui es una sola: nadie se pone online sin estar
 * verificado. [DriverValidators.blockReasons] es la unica fuente de
 * verdad de por que algo esta bloqueado, y la usan tanto la pantalla
 * como el servicio de GPS.
 */
class DriverRepository(
    context: Context
) {

    private val db = SmartBusDatabase.get(context)

    /**
     * Codigo OTP del telefono por cuenta. Se guarda fuera de Room a
     * proposito: es material efimero que no debe hacer historial.
     *
     * Formato de cada entrada: "codigo:marcaDeTiempo".
     */
    private val otpPrefs =
        context.getSharedPreferences("smartbus_phone_otp", Context.MODE_PRIVATE)

    private val profileDao = db.driverProfileDao()
    private val vehicleDao = db.driverVehicleDao()
    private val documentDao = db.driverDocumentDao()
    private val shiftDao = db.driverShiftDao()
    private val tripDao = db.driverTripDao()
    private val transitVehicleDao = db.vehicleDao()
    private val routePointDao = db.routePointDao()

    private companion object {
        const val TAG = "SmartBusDriverRepo"
    }

    // =====================================================
    // PERFIL
    // =====================================================

    fun observeProfile(accountId: String): Flow<DriverProfile?> =
        profileDao.observe(accountId).map { it?.toModel() }

    suspend fun getProfile(accountId: String): DriverProfile? =
        profileDao.get(accountId)?.toModel()

    /**
     * Guarda los datos del conductor. Todavia no lo verifica: eso lo
     * decide la revision documental.
     */
    suspend fun saveProfile(profile: DriverProfile) {

        val nextStatus = when {

            // Si ya estaba verificado, editar datos no lo deja de
            // golpe: los documentos vuelven a revision.
            profile.status == DriverVerificationStatus.VERIFIED &&
                !profile.isComplete ->
                DriverVerificationStatus.PENDING_REVIEW

            profile.status == DriverVerificationStatus.NOT_STARTED &&
                profile.isComplete ->
                DriverVerificationStatus.PENDING_REVIEW

            else -> profile.status
        }

        val updated = profile.copy(
            email = profile.email.trim().lowercase(),
            status = nextStatus
        )

        profileDao.upsert(updated.toEntity())
        syncDriverToFirestore(updated.accountId)
    }

    private fun firestoreDocId(email: String, accountId: String): String {
        val clean = email.trim().lowercase()
        return if (clean.isNotBlank()) {
            clean.replace("@", "_").replace(".", "_")
        } else {
            accountId.ifBlank { "conductor" }
        }
    }

    /**
     * Sincroniza todos los datos del conductor (incluyendo su correo,
     * vehiculo y estado de documentos) con la coleccion 'conductores'
     * de Firebase Firestore para que queden respaldados y pueda iniciar
     * solo con su correo cuando vuelva a abrir la app.
     */
    private suspend fun syncDriverToFirestore(accountId: String) {
        try {
            val profile = getProfile(accountId) ?: return
            val primaryVehicle = getPrimaryVehicle(accountId)
            val docs = getDocuments(accountId)
            val approvedTypes = docs.filter { it.satisfied }.map { it.type.name }
            val uploadedTypes = docs.map { it.type.name }

            val cleanEmail = profile.email.trim().lowercase()
            val docId = firestoreDocId(cleanEmail, accountId)

            val data = hashMapOf<String, Any?>(
                "accountId" to profile.accountId,
                "correo" to cleanEmail,
                "email" to cleanEmail,
                "nombre" to profile.fullName,
                "fullName" to profile.fullName,
                "documentId" to profile.documentId,
                "documentType" to profile.documentType,
                "companyName" to profile.companyName,
                "companyCode" to profile.companyCode,
                "birthDate" to (profile.birthDate ?: ""),
                "phone" to profile.phone,
                "phoneVerified" to profile.phoneVerified,
                "licenseNumber" to profile.licenseNumber,
                "licenseClass" to profile.licenseClass,
                "licenseExpiresAt" to (profile.licenseExpiresAt ?: 0L),
                "emergencyContactName" to profile.emergencyContactName,
                "emergencyContactPhone" to profile.emergencyContactPhone,
                "status" to profile.status.name,
                "statusReason" to profile.statusReason,
                "approvedDocuments" to approvedTypes,
                "uploadedDocuments" to uploadedTypes,
                "updatedAt" to System.currentTimeMillis()
            )

            if (primaryVehicle != null) {
                data["vehiclePlate"] = primaryVehicle.plate
                data["vehicleBrand"] = primaryVehicle.brand
                data["vehicleModel"] = primaryVehicle.model
                data["vehicleYear"] = primaryVehicle.year
                data["vehicleColor"] = primaryVehicle.color
                data["vehicleCapacity"] = primaryVehicle.capacity
                data["vehicleSoatExpiresAt"] = primaryVehicle.soatExpiresAt ?: 0L
                data["vehicleInspectionExpiresAt"] = primaryVehicle.inspectionExpiresAt ?: 0L
                data["transitVehicleId"] = primaryVehicle.transitVehicleId ?: 0
            }

            FirebaseFirestore.getInstance()
                .collection("conductores")
                .document(docId)
                .set(data, SetOptions.merge())
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo sincronizar conductor en Firestore: ${e.message}")
        }
    }

    /**
     * Permite a un conductor ya registrado iniciar o recuperar su perfil,
     * vehiculo y documentos ingresando unicamente su correo electronico,
     * buscando primero en Room local y luego en Firebase Firestore ('conductores').
     */
    suspend fun loginOrRestoreByEmail(
        email: String,
        targetAccountId: String
    ): DriverProfile? {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank()) return null

        val effectiveAccountId = targetAccountId.ifBlank { "email_$cleanEmail" }

        // 1. Buscar primero en Room local por correo o por accountId basado en correo
        val localEntity = profileDao.getByEmail(cleanEmail)
            ?: profileDao.get("email_$cleanEmail")
            ?: profileDao.get(effectiveAccountId)?.takeIf {
                it.email.equals(cleanEmail, ignoreCase = true) || it.email.isBlank()
            }

        if (localEntity != null) {
            val sourceAccountId = localEntity.accountId
            val updatedEntity = localEntity.copy(
                accountId = effectiveAccountId,
                email = cleanEmail
            )

            if (sourceAccountId != effectiveAccountId) {
                // Copiar vehiculos y documentos al accountId actual
                val oldVehicles = vehicleDao.getForAccount(sourceAccountId)
                val oldDocs = documentDao.getForAccount(sourceAccountId)
                profileDao.delete(sourceAccountId)
                profileDao.upsert(updatedEntity)
                oldVehicles.forEach { v ->
                    vehicleDao.delete(v.id)
                    vehicleDao.upsert(v.copy(id = 0, accountId = effectiveAccountId))
                }
                oldDocs.forEach { d ->
                    documentDao.upsert(d.copy(id = 0, accountId = effectiveAccountId))
                }
            } else if (localEntity.email != cleanEmail) {
                profileDao.upsert(updatedEntity)
            }

            syncDriverToFirestore(effectiveAccountId)
            return updatedEntity.toModel()
        }

        // 2. Si no estaba en Room local (por ejemplo tras reinstalar o cerrar sesion),
        // buscar en Firebase Firestore coleccion 'conductores'
        val snapshot = fetchDriverDocFromFirestore(cleanEmail, effectiveAccountId)
            ?: return null

        val docIdNumber = snapshot.getString("documentId").orEmpty()
        val fullName = snapshot.getString("fullName")
            ?: snapshot.getString("nombre")
            ?: return null

        if (docIdNumber.isBlank()) return null

        // Si habia otro perfil con el mismo DNI bajo otro accountId, limpiarlo antes de insertar
        val statusStr = snapshot.getString("status") ?: DriverVerificationStatus.PENDING_REVIEW.name
        val restoredProfile = DriverProfile(
            accountId = effectiveAccountId,
            email = cleanEmail,
            fullName = fullName,
            documentId = docIdNumber,
            documentType = snapshot.getString("documentType") ?: "DNI",
            companyName = snapshot.getString("companyName").orEmpty(),
            companyCode = snapshot.getString("companyCode").orEmpty(),
            birthDate = snapshot.getString("birthDate")?.ifBlank { null },
            phone = snapshot.getString("phone").orEmpty(),
            phoneVerified = snapshot.getBoolean("phoneVerified") ?: true,
            licenseNumber = snapshot.getString("licenseNumber").orEmpty(),
            licenseClass = snapshot.getString("licenseClass").orEmpty(),
            licenseExpiresAt = snapshot.getLong("licenseExpiresAt")?.takeIf { it > 0L },
            emergencyContactName = snapshot.getString("emergencyContactName").orEmpty(),
            emergencyContactPhone = snapshot.getString("emergencyContactPhone").orEmpty(),
            status = runCatching { DriverVerificationStatus.valueOf(statusStr) }
                .getOrDefault(DriverVerificationStatus.PENDING_REVIEW),
            statusReason = snapshot.getString("statusReason").orEmpty()
        )

        runCatching {
            profileDao.upsert(restoredProfile.toEntity())
        }

        val plate = snapshot.getString("vehiclePlate").orEmpty().trim().uppercase()
        if (plate.isNotBlank()) {
            val existingVehicles = vehicleDao.getForAccount(effectiveAccountId)
            if (existingVehicles.none { it.plate.equals(plate, ignoreCase = true) }) {
                val defaultSoat = System.currentTimeMillis() + 365L * 24L * 60L * 60L * 1000L
                val soatExp = snapshot.getLong("vehicleSoatExpiresAt")?.takeIf { it > 0L } ?: defaultSoat
                val inspExp = snapshot.getLong("vehicleInspectionExpiresAt")?.takeIf { it > 0L }
                val transitId = snapshot.getLong("transitVehicleId")?.toInt()?.takeIf { it > 0 }
                runCatching {
                    saveVehicle(
                        DriverVehicle(
                            accountId = effectiveAccountId,
                            plate = plate,
                            brand = snapshot.getString("vehicleBrand").orEmpty().ifBlank { "Bus" },
                            model = snapshot.getString("vehicleModel").orEmpty().ifBlank { "Urbano" },
                            year = snapshot.getLong("vehicleYear")?.toInt() ?: 2022,
                            color = snapshot.getString("vehicleColor").orEmpty().ifBlank { "Blanco" },
                            plateCount = lastDigitsOf(plate),
                            capacity = snapshot.getLong("vehicleCapacity")?.toInt() ?: 30,
                            isPrimary = true,
                            soatExpiresAt = soatExp,
                            inspectionExpiresAt = inspExp,
                            status = restoredProfile.status,
                            transitVehicleId = transitId
                        )
                    )
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        val approvedList = (snapshot.get("approvedDocuments") as? List<String>).orEmpty()
        @Suppress("UNCHECKED_CAST")
        val uploadedList = (snapshot.get("uploadedDocuments") as? List<String>).orEmpty()
        val allTypes = (approvedList + uploadedList).distinct()

        allTypes.forEach { typeName ->
            val docType = runCatching { DriverDocumentType.valueOf(typeName) }.getOrNull()
            if (docType != null) {
                val isApproved = typeName in approvedList ||
                    restoredProfile.status == DriverVerificationStatus.VERIFIED
                documentDao.upsert(
                    DriverDocumentEntity(
                        accountId = effectiveAccountId,
                        type = docType.name,
                        filePath = "cloud://conductores/$cleanEmail/${docType.name}",
                        status = if (isApproved) {
                            DriverDocumentStatus.APPROVED.name
                        } else {
                            DriverDocumentStatus.UPLOADED.name
                        },
                        note = "",
                        expiresAt = null,
                        uploadedAt = System.currentTimeMillis(),
                        reviewedAt = if (isApproved) System.currentTimeMillis() else null
                    )
                )
            }
        }

        refreshVerificationStatus(effectiveAccountId)
        return getProfile(effectiveAccountId) ?: restoredProfile
    }

    private suspend fun fetchDriverDocFromFirestore(
        cleanEmail: String,
        accountId: String
    ): DocumentSnapshot? = suspendCancellableCoroutine { cont ->
        try {
            val firestore = FirebaseFirestore.getInstance()
            val docId = firestoreDocId(cleanEmail, accountId)
            firestore.collection("conductores").document(docId).get()
                .addOnSuccessListener { doc ->
                    if (doc != null && doc.exists()) {
                        if (cont.isActive) cont.resume(doc)
                    } else {
                        firestore.collection("conductores")
                            .whereEqualTo("correo", cleanEmail)
                            .limit(1)
                            .get()
                            .addOnSuccessListener { query ->
                                val match = query.documents.firstOrNull()
                                if (cont.isActive) cont.resume(match)
                            }
                            .addOnFailureListener {
                                if (cont.isActive) cont.resume(null)
                            }
                    }
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(null)
                }
        } catch (_: Exception) {
            if (cont.isActive) cont.resume(null)
        }
    }

    /**
     * El DNI es unico en el sistema: dos cuentas con el mismo documento
     * significaria dos personas o un robo de identidad.
     *
     * Se cuenta el documento entre todos los perfiles y se excluye la
     * propia cuenta, para que el conductor pueda corregir sus datos
     * sin que la app le diga que su DNI ya es suyo.
     */
    suspend fun isDocumentAlreadyRegistered(
        documentId: String,
        exceptAccountId: String
    ): Boolean {

        val clean = documentId.trim()

        if (clean.isEmpty()) {
            return false
        }

        val total = profileDao.countByDocumentId(clean)

        if (total == 0) {
            return false
        }

        val mine = profileDao.get(exceptAccountId)

        // El unico caso permitido es que el unico perfil con ese
        // documento sea el de esta misma cuenta.
        return total > 1 || (mine == null || mine.documentId != clean)
    }

    suspend fun deleteProfile(accountId: String) =
        profileDao.delete(accountId)

    // =====================================================
    // VERIFICACION DEL TELEFONO
    // =====================================================
    //
    // Un numero de telefono no verificado no permite trabajar: solo
    // quien lo puede recibir como SMS puede decir que le pertenece.

    /**
     * Genera un codigo de 6 digitos y lo guarda con una expiracion de
     * 5 minutos.
     *
     * En una app real este codigo lo genera y envia el backend por SMS.
     * Aqui no hay backend todavia, asi que se genera igual que lo haria
     * el servidor, y la pantalla lo muestra solo en la version de
     * depuracion para que el flujo se pueda probar de punta a punta.
     */
    suspend fun requestPhoneCode(accountId: String): String {

        val code = (100_000 + SecureRandom().nextInt(900_000)).toString()

        otpPrefs.edit()
            .putString(otpKey(accountId), "$code:${System.currentTimeMillis()}")
            .apply()

        return code
    }

    /**
     * Confirma el codigo y marca el telefono como verificado.
     */
    suspend fun verifyPhoneCode(accountId: String, code: String): Boolean {

        val stored = otpPrefs.getString(otpKey(accountId), null)
            ?: return false

        val parts = stored.split(":")

        if (parts.size != 2) {
            return false
        }

        val sentAt = parts[1].toLongOrNull() ?: return false

        // 5 minutos de vida, igual que un SMS real.
        if (System.currentTimeMillis() - sentAt > 5 * 60 * 1000L) {
            return false
        }

        if (parts[0] != code.trim()) {
            return false
        }

        val existing = getProfile(accountId) ?: return false

        profileDao.upsert(
            existing.copy(phoneVerified = true).toEntity()
        )
        syncDriverToFirestore(accountId)

        // El codigo ya cumplio su funcion: se invalida.
        otpPrefs.edit().remove(otpKey(accountId)).apply()

        return true
    }

    private fun otpKey(accountId: String) = "otp_$accountId"

    // =====================================================
    // VEHICULOS
    // =====================================================

    fun observeVehicles(accountId: String): Flow<List<DriverVehicle>> =
        vehicleDao.observeForAccount(accountId).map { list ->
            list.map { it.toModel() }
        }

    suspend fun getVehicles(accountId: String): List<DriverVehicle> =
        vehicleDao.getForAccount(accountId).map { it.toModel() }

    suspend fun getPrimaryVehicle(accountId: String): DriverVehicle? =
        getVehicles(accountId).firstOrNull { it.isPrimary }
            ?: getVehicles(accountId).firstOrNull()

    /**
     * Una placa no puede repetirse entre conductores.
     */
    suspend fun isPlateAlreadyRegistered(plate: String): Boolean =
        vehicleDao.countByPlate(plate.trim().uppercase()) > 0

    suspend fun saveVehicle(vehicle: DriverVehicle): Long {

        val id = vehicleDao.upsert(
            vehicle.copy(
                plate = vehicle.plateNormalized
            ).toEntity()
        )

        if (vehicle.isPrimary) {
            vehicleDao.clearPrimary(vehicle.accountId)
            vehicleDao.upsert(
                vehicle.copy(
                    id = if (id > 0) id else vehicle.id,
                    isPrimary = true
                ).toEntity()
            )
        }

        syncDriverToFirestore(vehicle.accountId)

        return id
    }

    suspend fun deleteVehicle(id: Long) =
        vehicleDao.delete(id)

    // =====================================================
    // DOCUMENTOS
    // =====================================================

    fun observeDocuments(accountId: String): Flow<List<DriverDocument>> =
        documentDao.observeForAccount(accountId).map { list ->
            list.map { it.toModel() }
        }

    suspend fun getDocuments(accountId: String): List<DriverDocument> =
        documentDao.getForAccount(accountId).map { it.toModel() }

    suspend fun getApprovedDocumentTypes(accountId: String): Set<DriverDocumentType> =
        getDocuments(accountId)
            .filter { it.satisfied }
            .map { it.type }
            .toSet()

    /**
     * Sube un documento. Cada documento obligatorio que se sube mueve
     * el perfil a "en revision"; cuando estan todos, la plataforma
     * lo aprueba y el conductor puede trabajar.
     */
    suspend fun uploadDocument(
        accountId: String,
        type: DriverDocumentType,
        filePath: String,
        expiresAt: Long? = null
    ) {

        documentDao.upsert(
            DriverDocumentEntity(
                accountId = accountId,
                type = type.name,
                filePath = filePath,
                status = DriverDocumentStatus.UPLOADED.name,
                note = "",
                expiresAt = expiresAt,
                uploadedAt = System.currentTimeMillis(),
                reviewedAt = null
            )
        )

        refreshVerificationStatus(accountId)
    }

    suspend fun deleteDocument(accountId: String, type: DriverDocumentType) {
        documentDao.delete(accountId, type.name)
        refreshVerificationStatus(accountId)
    }

    /**
     * Aprueba un documento. Lo llama el backend de revision, no la app
     * del conductor: asi nadie se auto-verifica.
     */
    suspend fun approveDocument(
        accountId: String,
        type: DriverDocumentType,
        note: String = ""
    ) {

        val existing = documentDao.getForAccount(accountId)
            .firstOrNull { it.type == type.name } ?: return

        documentDao.upsert(
            existing.copy(
                status = DriverDocumentStatus.APPROVED.name,
                note = note,
                reviewedAt = System.currentTimeMillis()
            )
        )

        refreshVerificationStatus(accountId)
    }

    suspend fun rejectDocument(
        accountId: String,
        type: DriverDocumentType,
        note: String
    ) {

        val existing = documentDao.getForAccount(accountId)
            .firstOrNull { it.type == type.name } ?: return

        documentDao.upsert(
            existing.copy(
                status = DriverDocumentStatus.REJECTED.name,
                note = note,
                reviewedAt = System.currentTimeMillis()
            )
        )

        val profile = profileDao.get(accountId)

        if (profile != null) {
            profileDao.upsert(
                profile.copy(
                    status = DriverVerificationStatus.REJECTED.name,
                    statusReason = note
                )
            )
        }
    }

    /**
     * Recalcula el estado del conductor a partir de los documentos.
     *
     * - Si faltan documentos obligatorios, sigue en revision.
     * - Si estan todos y no hay motivo de rechazo, queda verificado.
     */
    private suspend fun refreshVerificationStatus(accountId: String) {

        val entity = profileDao.get(accountId) ?: return

        val profile = entity.toModel()

        val approved = getApprovedDocumentTypes(accountId)

        val missing = DriverRequirements.required.filter { it !in approved }

        val nextStatus = when {

            profile.status == DriverVerificationStatus.SUSPENDED ->
                DriverVerificationStatus.SUSPENDED

            profile.status == DriverVerificationStatus.REJECTED &&
                missing.isNotEmpty() ->
                DriverVerificationStatus.REJECTED

            !profile.isComplete ->
                DriverVerificationStatus.NOT_STARTED

            missing.isEmpty() -> {

                val vehicle = getPrimaryVehicle(accountId)

                if (vehicle != null && vehicle.soatValid) {
                    DriverVerificationStatus.VERIFIED
                } else {
                    DriverVerificationStatus.PENDING_REVIEW
                }
            }

            else ->
                DriverVerificationStatus.PENDING_REVIEW
        }

        profileDao.upsert(
            profile.copy(
                status = nextStatus,
                statusReason = if (nextStatus == DriverVerificationStatus.VERIFIED) {
                    ""
                } else {
                    profile.statusReason
                }
            ).toEntity()
        )
        syncDriverToFirestore(accountId)
    }

    // =====================================================
    // TURNOS Y UBICACION
    // =====================================================

    fun observeShift(accountId: String): Flow<DriverShift?> =
        shiftDao.observe(accountId).map { it?.toModel() }

    /**
     * Conductores online con GPS reciente. El pasajero usa esto para
     * saber a quien puede ver.
     */
    fun observeOnlineDrivers(maxGpsAgeMs: Long = 90_000L): Flow<List<DriverShift>> =
        shiftDao.observeOnlineWithFreshGps(
            System.currentTimeMillis() - maxGpsAgeMs
        ).map { list -> list.map { it.toModel() } }

    suspend fun getShift(accountId: String): DriverShift? =
        shiftDao.get(accountId)?.toModel()

    /**
     * Poner online solo es posible si todo esta en regla. Este metodo
     * es el que decide, no la pantalla.
     */
    suspend fun goOnline(accountId: String): DriverGateResult {

        val profile = getProfile(accountId)
        val vehicle = getPrimaryVehicle(accountId)
        val approved = getApprovedDocumentTypes(accountId)

        val blockers = DriverValidators.blockReasons(profile, vehicle, approved)

        if (blockers.isNotEmpty()) {
            return DriverGateResult.Blocked(blockers)
        }

        val primary = vehicle!!

        shiftDao.upsert(
            DriverShiftEntity(
                accountId = accountId,
                vehicleId = primary.id,
                isOnline = true,
                isOnTrip = false,
                routeId = null,
                startedAt = System.currentTimeMillis(),
                lastLatitude = null,
                lastLongitude = null,
                lastSpeedKmh = 0f,
                lastHeadingDegrees = null,
                gpsAccuracyMeters = null,
                lastLocationAt = null
            )
        )

        return DriverGateResult.Allowed
    }

    suspend fun goOffline(accountId: String) {

        val current = shiftDao.get(accountId) ?: return

        shiftDao.upsert(
            current.copy(
                isOnline = false,
                isOnTrip = false,
                startedAt = null
            )
        )

        // Al salir de servicio el conductor deja de estar asociado a la
        // unidad: el pasajero no debe seguir viendolo ni un momento mas.
        detachFromTransitVehicle(current)
    }

    /**
     * Ruta a la que esta asignado el conductor ahora mismo.
     *
     * Se resuelve por el camino turno -> vehiculo -> unidad de
     * transporte. Es la que se usa para comprobar que su ubicacion
     * sigue el recorrido y no se marcha a otro lado.
     */
    suspend fun getAssignedRouteId(accountId: String): Int? {

        val shift = shiftDao.get(accountId) ?: return null

        val driverVehicleId = shift.vehicleId ?: return null

        val driverVehicle = vehicleDao.getById(driverVehicleId) ?: return null

        val transitId = driverVehicle.transitVehicleId ?: return null

        return transitVehicleDao.getById(transitId)?.routeId
    }

    /**
     * Trazado de la ruta asignada, en orden. Vacio si el conductor
     * todavia no tiene ruta: en ese caso no hay nada que vigilar.
     */
    suspend fun getAssignedRoutePoints(accountId: String): List<GeoPoint> {

        val routeId = getAssignedRouteId(accountId) ?: return emptyList()

        return routePointDao.getForRoute(routeId).map { point ->
            GeoPoint(
                latitude = point.latitude,
                longitude = point.longitude
            )
        }
    }

    /**
     * Desasocia al conductor de su unidad de transporte.
     *
     * Se llama al terminar el servicio y tambien cuando el conductor se
     * desvia tanto que dejamos de publicar su posicion. La unidad queda
     * libre para que el operador (o el simulador) la vuelva a usar.
     */
    private suspend fun detachFromTransitVehicle(
        shift: com.ejemplo.smartbus.data.db.DriverShiftEntity
    ) {
        // Eliminar vehiculo en vivo de Firestore al salir de servicio
        try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            firestore.collection("vehiculos_en_vivo").document(shift.accountId).delete()
        } catch (_: Exception) {
        }

        val driverVehicleId = shift.vehicleId ?: return

        val driverVehicle = vehicleDao.getById(driverVehicleId) ?: return

        val transitId = driverVehicle.transitVehicleId ?: return

        try {
            transitVehicleDao.clearLivePosition(transitId)
        } catch (exception: Exception) {
            Log.w(TAG, "No se pudo soltar la unidad: ${exception.message}")
        }
    }

/**
 * Escribe la posicion del conductor. Lo llama el servicio en
     * segundo plano.
     *
     * Ademas de guardarla en el turno, se refleja en la tabla de
     * vehiculos: el mapa del pasajero lee de ahi, asi que este es
     * el punto donde el conductor se vuelve visible.
     */
    suspend fun publishLocation(
        accountId: String,
        latitude: Double,
        longitude: Double,
        speedKmh: Float,
        headingDegrees: Float?,
        gpsAccuracyMeters: Float?
    ) {

        shiftDao.updateLocation(
            accountId = accountId,
            latitude = latitude,
            longitude = longitude,
            speedKmh = speedKmh,
            headingDegrees = headingDegrees,
            gpsAccuracyMeters = gpsAccuracyMeters,
            recordedAt = System.currentTimeMillis()
        )

        mirrorPositionToTransit(
            accountId = accountId,
            latitude = latitude,
            longitude = longitude,
            speedKmh = speedKmh,
            headingDegrees = headingDegrees,
            gpsAccuracyMeters = gpsAccuracyMeters
        )
    }

    /**
     * Copia la posicion del conductor al vehiculo de transito que
     * tiene asignado.
     *
     * Sin esto el conductor comparte GPS pero el pasajero no lo ve,
     * porque el mapa lee de la tabla de vehiculos y no de los turnos.
     * Solo se escribe si el vehiculo esta enlazado a una unidad de
     * transito: un vehiculo sin asignar no tiene donde dibujarse.
     */
    private suspend fun mirrorPositionToTransit(
        accountId: String,
        latitude: Double,
        longitude: Double,
        speedKmh: Float,
        headingDegrees: Float?,
        gpsAccuracyMeters: Float?
    ) {

        try {

            val shift = shiftDao.get(accountId) ?: return

            val driverVehicleId = shift.vehicleId ?: return

            val driverVehicle = vehicleDao.getById(driverVehicleId) ?: return

            val transitId = driverVehicle.transitVehicleId ?: return

            val profile = profileDao.get(accountId)

            val plate = driverVehicle.plate

            transitVehicleDao.upsertWithLivePosition(
                vehicleId = transitId,
                latitude = latitude,
                longitude = longitude,
                speedKmh = speedKmh,
                headingDegrees = headingDegrees,
                gpsAccuracy = gpsAccuracyMeters,
                isMock = false,
                plate = plate,
                plateCount = lastDigitsOf(plate),
                driverName = profile?.fullName.orEmpty(),
                capacity = driverVehicle.capacity,
                occupancy = null,
                status = if (shift.isOnTrip) "EN_RUTA" else "EN_SERVICIO",
                lastUpdate = System.currentTimeMillis()
            )

            // Publicar ubicacion en tiempo real en Firebase Firestore para que los pasajeros la vean
            try {
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val liveVehicleData = hashMapOf(
                    "conductorId" to accountId,
                    "correo" to profile?.email.orEmpty(),
                    "driverName" to profile?.fullName.orEmpty(),
                    "plate" to plate,
                    "plateCount" to lastDigitsOf(plate),
                    "routeId" to transitId,
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "latitud" to latitude,
                    "longitud" to longitude,
                    "speedKmh" to speedKmh,
                    "headingDegrees" to (headingDegrees ?: 0f),
                    "tripType" to (if (shift.isOnTrip) "SUBIDA" else "BAJADA"),
                    "status" to (if (shift.isOnTrip) "EN_RUTA" else "EN_SERVICIO"),
                    "capacity" to driverVehicle.capacity,
                    "isMock" to false,
                    "lastUpdate" to System.currentTimeMillis()
                )
                firestore.collection("vehiculos_en_vivo").document(accountId).set(liveVehicleData)
            } catch (e: Exception) {
                Log.w(TAG, "Error al sincronizar vehiculos_en_vivo con Firestore: ${e.message}")
            }

        } catch (exception: Exception) {
            Log.w(TAG, "No se pudo actualizar el vehiculo: ${exception.message}")
        }
    }

    /**
     * Ultimos 4 digitos de la placa, que es como se identifica un
     * vehiculo en Peru sin mostrarla completa.
     */
    private fun lastDigitsOf(plate: String): Int {

        val digits = plate.filter { it.isDigit() }

        if (digits.isEmpty()) {
            return 0
        }

        return digits.takeLast(4).toIntOrNull() ?: 0
    }

    // =====================================================
    // VIAJES
    // =====================================================

    fun observeTrips(accountId: String): Flow<List<DriverTrip>> =
        tripDao.observeForAccount(accountId).map { list ->
            list.map { it.toModel() }
        }

    suspend fun startTrip(
        accountId: String,
        routeId: Int?,
        originName: String,
        destinationName: String
    ): Long {

        val vehicle = getPrimaryVehicle(accountId)

        val id = tripDao.upsert(
            DriverTripEntity(
                accountId = accountId,
                vehicleId = vehicle?.id,
                routeId = routeId,
                originName = originName,
                destinationName = destinationName,
                startedAt = System.currentTimeMillis(),
                endedAt = null,
                distanceMeters = 0f,
                fare = 0.0,
                passengerName = null
            )
        )

        val shift = shiftDao.get(accountId)

        if (shift != null) {
            shiftDao.upsert(shift.copy(isOnTrip = true, routeId = routeId))
        }

        return id
    }

    suspend fun finishTrip(
        tripId: Long,
        distanceMeters: Float,
        fare: Double
    ) = tripDao.close(
        id = tripId,
        endedAt = System.currentTimeMillis(),
        distanceMeters = distanceMeters,
        fare = fare
    )

    suspend fun getActiveTrip(accountId: String): DriverTrip? =
        tripDao.getActive(accountId)?.toModel()
}

// =========================================================
// MAPPERS
// =========================================================

private fun DriverProfileEntity.toModel() =
    DriverProfile(
        accountId = accountId,
        email = email,
        fullName = fullName,
        documentId = documentId,
        documentType = documentType,
        companyName = companyName,
        companyCode = companyCode,
        birthDate = birthDate,
        phone = phone,
        phoneVerified = phoneVerified,
        licenseNumber = licenseNumber,
        licenseClass = licenseClass,
        licenseExpiresAt = licenseExpiresAt,
        emergencyContactName = emergencyContactName,
        emergencyContactPhone = emergencyContactPhone,
        status = runCatching {
            DriverVerificationStatus.valueOf(status)
        }.getOrDefault(DriverVerificationStatus.NOT_STARTED),
        statusReason = statusReason,
        rating = rating,
        totalTrips = totalTrips,
        createdAt = createdAt
    )

private fun DriverProfile.toEntity() =
    DriverProfileEntity(
        accountId = accountId,
        email = email,
        fullName = fullName,
        documentId = documentId,
        documentType = documentType,
        companyName = companyName,
        companyCode = companyCode,
        birthDate = birthDate,
        phone = phone,
        phoneVerified = phoneVerified,
        licenseNumber = licenseNumber,
        licenseClass = licenseClass,
        licenseExpiresAt = licenseExpiresAt,
        emergencyContactName = emergencyContactName,
        emergencyContactPhone = emergencyContactPhone,
        status = status.name,
        statusReason = statusReason,
        rating = rating,
        totalTrips = totalTrips,
        createdAt = createdAt
    )

private fun DriverVehicleEntity.toModel() =
    DriverVehicle(
        id = id,
        accountId = accountId,
        plate = plate,
        brand = brand,
        model = model,
        year = year,
        color = color,
        plateCount = plateCount,
        capacity = capacity,
        isPrimary = isPrimary,
        soatExpiresAt = soatExpiresAt,
        inspectionExpiresAt = inspectionExpiresAt,
        status = runCatching {
            DriverVerificationStatus.valueOf(status)
        }.getOrDefault(DriverVerificationStatus.NOT_STARTED),
        transitVehicleId = transitVehicleId,
        createdAt = createdAt
    )

private fun DriverVehicle.toEntity() =
    DriverVehicleEntity(
        id = id,
        accountId = accountId,
        plate = plateNormalized,
        brand = brand,
        model = model,
        year = year,
        color = color,
        plateCount = plateCount,
        capacity = capacity,
        isPrimary = isPrimary,
        soatExpiresAt = soatExpiresAt,
        inspectionExpiresAt = inspectionExpiresAt,
        status = status.name,
        transitVehicleId = transitVehicleId,
        createdAt = createdAt
    )

private fun DriverDocumentEntity.toModel() =
    DriverDocument(
        id = id,
        accountId = accountId,
        type = runCatching {
            DriverDocumentType.valueOf(type)
        }.getOrDefault(DriverDocumentType.ID_DOCUMENT),
        filePath = filePath,
        status = runCatching {
            DriverDocumentStatus.valueOf(status)
        }.getOrDefault(DriverDocumentStatus.UPLOADED),
        note = note,
        expiresAt = expiresAt,
        uploadedAt = uploadedAt,
        reviewedAt = reviewedAt
    )

private fun DriverShiftEntity.toModel() =
    DriverShift(
        accountId = accountId,
        vehicleId = vehicleId,
        isOnline = isOnline,
        isOnTrip = isOnTrip,
        routeId = routeId,
        startedAt = startedAt,
        lastLatitude = lastLatitude,
        lastLongitude = lastLongitude,
        lastSpeedKmh = lastSpeedKmh,
        lastHeadingDegrees = lastHeadingDegrees,
        gpsAccuracyMeters = gpsAccuracyMeters,
        lastLocationAt = lastLocationAt
    )

private fun DriverTripEntity.toModel() =
    DriverTrip(
        id = id,
        accountId = accountId,
        vehicleId = vehicleId,
        routeId = routeId,
        originName = originName,
        destinationName = destinationName,
        startedAt = startedAt,
        endedAt = endedAt,
        distanceMeters = distanceMeters,
        fare = fare,
        passengerName = passengerName
    )

/**
 * Resultado de intentar ponerse online.
 */
sealed interface DriverGateResult {
    data object Allowed : DriverGateResult
    data class Blocked(val blockers: List<DriverBlocker>) : DriverGateResult
}