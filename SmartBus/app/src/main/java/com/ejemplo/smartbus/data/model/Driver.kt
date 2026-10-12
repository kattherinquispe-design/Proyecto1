package com.ejemplo.smartbus.data.model

/**
 * Rol con el que entro la persona.
 *
 * El pasajero es el que ya teniamos. El conductor es el nuevo: puede
 * registrarse, subir documentos, ponerse online y compartir su
 * ubicacion en tiempo real.
 */
enum class UserRole {
    PASSENGER,
    DRIVER;

    val label: String
        get() = when (this) {
            PASSENGER -> "Pasajero"
            DRIVER -> "Conductor"
        }

    fun opposite(): UserRole =
        if (this == PASSENGER) DRIVER else PASSENGER
}

/**
 * Estado de la verificacion del conductor.
 *
 * Nadie puede ponerse online en un estado distinto de VERIFIED: por
 * eso el paso es una puerta real y no un adorno de la interfaz.
 */
enum class DriverVerificationStatus {
    NOT_STARTED,
    PENDING_REVIEW,
    VERIFIED,
    REJECTED,
    SUSPENDED;

    val label: String
        get() = when (this) {
            NOT_STARTED -> "Sin registrar"
            PENDING_REVIEW -> "En revision"
            VERIFIED -> "Verificado"
            REJECTED -> "Rechazado"
            SUSPENDED -> "Suspendido"
        }

    val canDrive: Boolean get() = this == VERIFIED
}

/**
 * Cada documento que se le pide al conductor.
 *
 * Estos son los que exigen inDrive, Uber, DiDi y Cabify para poder
 * trabajar en una plataforma de transporte. Cada uno evita un tipo de
 * fraude distinto.
 */
enum class DriverDocumentType {

    // ---- Identidad de la persona ----

    /** DNI o pasaporte, con foto legible de ambas caras. */
    ID_DOCUMENT,

    /** Verificacion de identidad: selfie comparada con la del DNI. */
    IDENTITY_SELFIE,

    /** Foto tuya sosteniendo tu documento. Es la prueba mas directa
     * de que la persona y el documento son de la misma persona. */
    ID_PHOTO_WITH_DOCUMENT,

    /** Verificacion de identidad en vivo (liveness / sin fotos). */
    LIVENESS_CHECK,

    /** Antecedentes penales. */
    CRIMINAL_RECORD,

    // ---- Licencia de conducir ----

    /** Licencia de conducir, cara frontal. */
    LICENSE_FRONT,

    /** Licencia de conducir, cara posterior. */
    LICENSE_BACK,

    // ---- Vehiculo ----

    /** Placa del vehiculo fotografiada de frente. */
    PLATE_PHOTO,

    /** Foto del vehiculo completo, lado. */
    VEHICLE_SIDE_PHOTO,

    /** Foto interior del vehiculo, para ver el tablero. */
    VEHICLE_INTERIOR_PHOTO,

    /** Tarjeta de inspeccion vehicular. */
    VEHICLE_INSPECTION_CARD,

    /** SOAT, el seguro obligatorio vigente. */
    SOAT,

    // ---- Transporte ----

    /** Habilitacion para prestar servicio de transporte de pasajeros. */
    TRANSPORT_LICENSE;

    val title: String
        get() = when (this) {
            ID_DOCUMENT -> "Documento de identidad"
            IDENTITY_SELFIE -> "Selfie con tu rostro"
            ID_PHOTO_WITH_DOCUMENT -> "Tu foto con el DNI en la mano"
            LIVENESS_CHECK -> "Verificacion de identidad en vivo"
            CRIMINAL_RECORD -> "Certificado de antecedentes penales"
            LICENSE_FRONT -> "Licencia de conducir (frente)"
            LICENSE_BACK -> "Licencia de conducir (dorso)"
            PLATE_PHOTO -> "Foto de la placa"
            VEHICLE_SIDE_PHOTO -> "Foto del vehiculo de lado"
            VEHICLE_INTERIOR_PHOTO -> "Foto del interior del vehiculo"
            VEHICLE_INSPECTION_CARD -> "Tarjeta de inspeccion vehicular"
            SOAT -> "SOAT vigente"
            TRANSPORT_LICENSE -> "Habilitacion de transporte"
        }

    val explanation: String
        get() = when (this) {
            ID_DOCUMENT ->
                "DNI o pasaporte. Sin el no podemos verificar quien eres."
            IDENTITY_SELFIE ->
                "Una foto de tu rostro. Se compara con la del documento " +
                    "para confirmar que la persona coincide."
            ID_PHOTO_WITH_DOCUMENT ->
                "Muestras tu documento junto a tu cara. Si el DNI fuera " +
                    "de otra persona, el rostro no coincidira."
            LIVENESS_CHECK ->
                "Un video corto pasando la cara. Detecta si se usa una " +
                    "foto de otra persona, un cartel o una imagen impresa."
            CRIMINAL_RECORD ->
                "Certificado de antecedentes penales. Se caduca, hay que " +
                    "renovarlo."
            LICENSE_FRONT ->
                "Debe coincidir con el vehiculo que vas a conducir y no " +
                    "estar vencida."
            LICENSE_BACK ->
                "El mismo numero de licencia que en el frente."
            PLATE_PHOTO ->
                "Debe leerse completa y coincidir con los datos que " +
                    "registras."
            VEHICLE_SIDE_PHOTO ->
                "Se ve el vehiculo completo de lado, sin cortes."
            VEHICLE_INTERIOR_PHOTO ->
                "Del interior, para ver el tablero y confirmar que el " +
                    "vehiculo es el mismo."
            VEHICLE_INSPECTION_CARD ->
                "Tarjeta de inspeccion vehicular vigente."
            SOAT ->
                "El SOAT vigente es obligatorio para circular en Peru."
            TRANSPORT_LICENSE ->
                "Permiso para prestar servicio de transporte de " +
                    "pasajeros."
        }

    val category: DriverDocumentCategory
        get() = when (this) {
            ID_DOCUMENT,
            IDENTITY_SELFIE,
            ID_PHOTO_WITH_DOCUMENT,
            LIVENESS_CHECK,
            CRIMINAL_RECORD -> DriverDocumentCategory.IDENTITY

            LICENSE_FRONT,
            LICENSE_BACK,
            TRANSPORT_LICENSE -> DriverDocumentCategory.LICENSE

            else -> DriverDocumentCategory.VEHICLE
        }
}

enum class DriverDocumentCategory(val label: String) {
    IDENTITY("Tu identidad"),
    LICENSE("Tu licencia"),
    VEHICLE("Tu vehiculo")
}

/**
 * Estado de cada documento.
 */
enum class DriverDocumentStatus {
    MISSING,
    UPLOADED,
    UNDER_REVIEW,
    APPROVED,
    REJECTED;

    val label: String
        get() = when (this) {
            MISSING -> "Falta subirlo"
            UPLOADED -> "Subido, esperando revision"
            UNDER_REVIEW -> "En revision"
            APPROVED -> "Aprobado"
            REJECTED -> "Rechazado"
        }
}

/**
 * Perfil del conductor. Es 1:1 con la cuenta de la app.
 */
data class DriverProfile(
    val accountId: String,
    val fullName: String,
    val documentId: String,
    val documentType: String,
    /**
     * Empresa o consorcio de transporte al que pertenece el conductor.
     *
     * Solo se admite una compania de la lista de confianza (ver
     * DriverRequirements.companies): asi la plataforma no se llena de
     * conductores sueltos sin respaldo, y el pasajero sabe que el
     * vehiculo pertenece a un operador real.
     */
    val companyName: String = "",
    val companyCode: String = "",
    val birthDate: String? = null,
    val phone: String = "",
    val phoneVerified: Boolean = false,
    val licenseNumber: String = "",
    val licenseClass: String = "",
    val licenseExpiresAt: Long? = null,
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val status: DriverVerificationStatus = DriverVerificationStatus.NOT_STARTED,
    val statusReason: String = "",
    val rating: Float = 0f,
    val totalTrips: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {

    val isComplete: Boolean
        get() = fullName.isNotBlank() &&
            documentId.isNotBlank() &&
            companyName.isNotBlank() &&
            companyCode.isNotBlank() &&
            phone.isNotBlank() &&
            licenseNumber.isNotBlank() &&
            emergencyContactName.isNotBlank() &&
            emergencyContactPhone.isNotBlank()

    /** La licencia vencida bloquea el trabajo igual que no tenerla. */
    val licenseValid: Boolean
        get() = licenseExpiresAt == null || licenseExpiresAt > System.currentTimeMillis()
}

/**
 * Vehiculo que conduce. Un conductor puede tener mas de uno.
 */
data class DriverVehicle(
    val id: Long = 0,
    val accountId: String,
    val plate: String,
    val brand: String,
    val model: String,
    val year: Int = 0,
    val color: String = "",
    val plateCount: Int = 0,
    val capacity: Int = 0,
    val isPrimary: Boolean = false,
    val soatExpiresAt: Long? = null,
    val inspectionExpiresAt: Long? = null,
    val status: DriverVerificationStatus = DriverVerificationStatus.NOT_STARTED,
    /**
     * Unidad de transito asignada. Sin esto el conductor comparte GPS
     * pero el pasajero no tiene donde verlo.
     */
    val transitVehicleId: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
) {

    val plateNormalized: String get() = plate.uppercase().trim()

    val soatValid: Boolean
        get() = soatExpiresAt != null && soatExpiresAt > System.currentTimeMillis()

    val inspectionValid: Boolean
        get() = inspectionExpiresAt == null ||
            inspectionExpiresAt > System.currentTimeMillis()

    val description: String
        get() = listOf(brand, model, year)
            .filter { it.toString().isNotBlank() && it.toString() != "0" }
            .joinToString(" ")
}

/**
 * Un documento subido, con el estado de su revision.
 */
data class DriverDocument(
    val id: Long = 0,
    val accountId: String,
    val type: DriverDocumentType,
    val filePath: String,
    val status: DriverDocumentStatus = DriverDocumentStatus.UPLOADED,
    val note: String = "",
    val expiresAt: Long? = null,
    val uploadedAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long? = null
) {

    val expired: Boolean
        get() = expiresAt != null && expiresAt < System.currentTimeMillis()

    val satisfied: Boolean
        get() = status == DriverDocumentStatus.APPROVED && !expired
}

/**
 * Turno actual del conductor: si esta o no en linea y donde esta.
 */
data class DriverShift(
    val accountId: String,
    val vehicleId: Long?,
    val isOnline: Boolean = false,
    val isOnTrip: Boolean = false,
    val routeId: Int? = null,
    val startedAt: Long? = null,
    val lastLatitude: Double? = null,
    val lastLongitude: Double? = null,
    val lastSpeedKmh: Float = 0f,
    val lastHeadingDegrees: Float? = null,
    val gpsAccuracyMeters: Float? = null,
    val lastLocationAt: Long? = null
) {

    /** GPS viejo no sirve: si hace mas de esto, el conductor aparece desconectado. */
    fun hasFreshLocation(maxAgeMs: Long = 90_000L): Boolean {
        val at = lastLocationAt ?: return false
        return System.currentTimeMillis() - at <= maxAgeMs
    }
}

/**
 * Viaje del conductor, para las ganancias y para que el pasajero vea
 * el recorrido.
 */
data class DriverTrip(
    val id: Long = 0,
    val accountId: String,
    val vehicleId: Long?,
    val routeId: Int?,
    val originName: String = "",
    val destinationName: String = "",
    val startedAt: Long,
    val endedAt: Long? = null,
    val distanceMeters: Float = 0f,
    val fare: Double = 0.0,
    val passengerName: String? = null
) {

    val isFinished: Boolean get() = endedAt != null
    val durationMs: Long get() = (endedAt ?: System.currentTimeMillis()) - startedAt
}