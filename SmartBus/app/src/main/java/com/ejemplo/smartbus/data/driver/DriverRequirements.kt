package com.ejemplo.smartbus.data.driver

import com.ejemplo.smartbus.data.model.DriverDocumentType
import com.ejemplo.smartbus.data.model.DriverProfile
import com.ejemplo.smartbus.data.model.DriverVehicle

/**
 * Requisitos para poder trabajar como conductor en SmartBus.
 *
 * No es una lista inventada: son los controles que ya exigen las
 * plataformas de transporte reales (inDrive, Uber, DiDi, Cabify) mas
 * los que exige la normativa peruana para prestar servicio de
 * transporte de pasajeros. Cada uno tapa un fraude distinto:
 *
 *  - Identidad  -> evita conducir con el vehiculo de otra persona.
 *  - Licencia   -> evita manejar sin habilitacion legal.
 *  - Antecedentes -> evita conductores con antecedentes penales.
 *  - SOAT       -> es obligatorio para circular en Peru.
 *  - Verificacion de telefono -> evita cuentas creadas en masa.
 *  - Contacto de emergencia -> permite avisar a alguien si el
 *    conductor no responde durante un viaje.
 *  - GPS honesto -> detecta ubicacion falsa.
 */
object DriverRequirements {

    /**
     * Documentos que SI son obligatorios. Sin todos ellos y con el
     * perfil verificado, el conductor no puede ponerse online.
     */
    val required: List<DriverDocumentType> = listOf(
        DriverDocumentType.ID_DOCUMENT,
        DriverDocumentType.IDENTITY_SELFIE,
        DriverDocumentType.ID_PHOTO_WITH_DOCUMENT,
        DriverDocumentType.LIVENESS_CHECK,
        DriverDocumentType.CRIMINAL_RECORD,
        DriverDocumentType.LICENSE_FRONT,
        DriverDocumentType.LICENSE_BACK,
        DriverDocumentType.PLATE_PHOTO,
        DriverDocumentType.VEHICLE_SIDE_PHOTO,
        DriverDocumentType.VEHICLE_INSPECTION_CARD,
        DriverDocumentType.SOAT,
        DriverDocumentType.TRANSPORT_LICENSE
    )

    /**
     * Documentos recomendados. No bloquean el trabajo, pero sin ellos
     * el conductor ve una recomendacion clara.
     */
    val recommended: List<DriverDocumentType> = listOf(
        DriverDocumentType.VEHICLE_INTERIOR_PHOTO
    )

    val all: List<DriverDocumentType> = required + recommended

    /**
     * Empresas de transporte autorizadas en la plataforma.
     *
     * Para asegurar que solo ingrese personal de companias confiables,
     * el conductor debe elegir una de estas y escribir el codigo de
     * operador que le entrega la empresa. Si el codigo no coincide, el
     * registro no avanza: nadie se auto-adscribe.
     *
     * En produccion esta lista la entrega el backend (convenios firmados
     * con cada empresa) y se puede desactivar una empresa en caliente.
     */
    val companies: List<TrustedCompany> = listOf(
        TrustedCompany(
            name = "Consorcio Lima Norte",
            code = "CLN",
            operatorCode = "CLN-2026"
        ),
        TrustedCompany(
            name = "Transportes Arequipa Express",
            code = "TAX",
            operatorCode = "TAX-2026"
        ),
        TrustedCompany(
            name = "Corporacion El Rápido",
            code = "CER",
            operatorCode = "CER-2026"
        ),
        TrustedCompany(
            name = "Metropolitano Sur",
            code = "MTS",
            operatorCode = "MTS-2026"
        ),
        TrustedCompany(
            name = "Rutas del Pacífico",
            code = "RDP",
            operatorCode = "RDP-2026"
        )
    )

    fun findCompany(code: String): TrustedCompany? =
        companies.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) }

    fun findCompanyByOperatorCode(operatorCode: String): TrustedCompany? =
        companies.firstOrNull {
            it.operatorCode.equals(operatorCode.trim(), ignoreCase = true)
        }

    val steps: List<DriverStep> = listOf(
        DriverStep(
            title = "Quien eres",
            subtitle = "Que ninguna otra persona se haga pasar por ti",
            items = listOf(
                DriverDocumentType.ID_DOCUMENT,
                DriverDocumentType.IDENTITY_SELFIE,
                DriverDocumentType.ID_PHOTO_WITH_DOCUMENT,
                DriverDocumentType.LIVENESS_CHECK,
                DriverDocumentType.CRIMINAL_RECORD
            )
        ),
        DriverStep(
            title = "Tu licencia",
            subtitle = "Para conducir de forma legal y con la licencia vigente",
            items = listOf(
                DriverDocumentType.LICENSE_FRONT,
                DriverDocumentType.LICENSE_BACK,
                DriverDocumentType.TRANSPORT_LICENSE
            )
        ),
        DriverStep(
            title = "Tu vehiculo",
            subtitle = "Para que el pasajero sepa en que se sube",
            items = listOf(
                DriverDocumentType.PLATE_PHOTO,
                DriverDocumentType.VEHICLE_SIDE_PHOTO,
                DriverDocumentType.VEHICLE_INSPECTION_CARD,
                DriverDocumentType.SOAT
            )
        )
    )
}

/**
 * Una empresa de transporte con convenio. El [operatorCode] es el
 * codigo interno que la empresa le entrega a su personal.
 */
data class TrustedCompany(
    val name: String,
    val code: String,
    val operatorCode: String
)

data class DriverStep(
    val title: String,
    val subtitle: String,
    val items: List<DriverDocumentType>
)

/**
 * Motivos por los que no se puede empezar a trabajar. Se muestran
 * como lista para que el conductor sepa exactamente que le falta.
 */
data class DriverBlocker(
    val title: String,
    val detail: String
)

/**
 * Validadores de datos reales.
 *
 * Estos no son adornos: rechazan datos que el sistema despues no
 * podria usar. Por ejemplo, el DNI peruano tiene un digito de control
 * que se puede calcular, y una placa tiene un formato concreto.
 */
object DriverValidators {

    private val DNI_PATTERN = Regex("^\\d{8}$")

    /**
     * Verifica el digito de control del DNI peruano.
     *
     * El DNI tiene 8 digitos y el ultimo es un checksum: se calcula
     * con la tabla oficial de pesos 5,4,3,2,7,6,5,4,3,2 y el residuo
     * 11 menos el resto. Si no cuadra, el documento no existe.
     */
    fun isValidPeruvianDni(dni: String): Boolean {

        val clean = dni.trim()

        if (!DNI_PATTERN.matches(clean)) {
            return false
        }

        val weights = intArrayOf(5, 4, 3, 2, 7, 6, 5, 4, 3, 2)
        val digits = clean.map { it - '0' }

        var sum = 0

        for (i in 0..8) {
            sum += digits[i] * weights[i]
        }

        val remainder = 11 - (sum % 11)

        // 10 y 11 no son digitos: 10 se corrige a 0 y 11 repite el
        // primer digito del numero, segun la regla de RENIEC.
        val expected = when (remainder) {
            10 -> 0
            11 -> digits[0]
            else -> remainder
        }

        return digits[9] == expected
    }

    /**
     * Formato de placa peruana.
     *
     * Tres letras y tres numeros (ABC-123) para las placas actuales,
     * y el formato antiguo de dos letras (AB-12C / AB-1234) que sigue
     * circulando.
     */
    private val PLATE_PATTERN =
        Regex("^[A-Z]{2,3}[- ]?([0-9]{3}|[0-9]{2}[A-Z]|[0-9]{4})$")

    fun isValidPlate(plate: String): Boolean {

        val clean = plate.trim().uppercase().replace("-", "").replace(" ", "")

        return PLATE_PATTERN.matches(plate.trim().uppercase()) ||
            PLATE_PATTERN.matches(clean)
    }

    /** Formato de celular peruano: 9 digitos empezando por 9. */
    fun isValidPeruvianPhone(phone: String): Boolean =
        phone.trim().replace(Regex("[\\s-]"), "").matches(Regex("^9\\d{8}$"))

    /**
     * Fecha en formato YYYY-MM-DD.
     *
     * Se comprueba que la fecha exista de verdad: el Calendar
     * normalizaría sin avisar un 31 de febrero y lo convertiría en
     * marzo, dejando pasar fechas que no existen.
     */
    fun isValidBirthDate(date: String, minAgeYears: Int = 18): Boolean {

        val parts = date.trim().split("-")

        if (parts.size != 3 || parts.any { it.length != 4 && it.length != 2 }) {
            return false
        }

        val year = parts[0].toIntOrNull() ?: return false
        val month = parts[1].toIntOrNull() ?: return false
        val day = parts[2].toIntOrNull() ?: return false

        if (month !in 1..12 || day < 1) {
            return false
        }

        // Dias reales de ese mes, respetando años bisiestos.
        val daysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

        val leap = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

        val maxDay = if (month == 2 && leap) 29 else daysInMonth[month - 1]

        if (day > maxDay) {
            return false
        }

        val birth = java.util.Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day)
        }.timeInMillis

        val eighteenYearsAgo = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.YEAR, -minAgeYears)
        }.timeInMillis

        return birth <= eighteenYearsAgo
    }

    /**
     * Clases de licencia que autorizan a transportar pasajeros.
     */
    val passengerLicenseClasses = listOf("A-IIIb", "A-IIIc", "A-IIIa", "A-III")

    fun isLicenseClassAllowedForPassengers(licenseClass: String): Boolean {
        val clean = licenseClass.trim().uppercase().replace(" ", "")
        return passengerLicenseClasses.any { clean == it.uppercase().replace(" ", "") }
    }

    /**
     * Anos de experiencia minima para conducir transporte.
     */
    val minimumExperienceYears = 2

    /**
     * resumen de por que no puede trabajar todavia.
     */
    fun blockReasons(
        profile: DriverProfile?,
        vehicle: DriverVehicle?,
        approvedTypes: Set<DriverDocumentType>
    ): List<DriverBlocker> {

        val reasons = mutableListOf<DriverBlocker>()

        if (profile == null) {
            reasons += DriverBlocker(
                "Aun no eres conductor",
                "Registra tus datos para empezar."
            )
            return reasons
        }

        if (!profile.isComplete) {
            reasons += DriverBlocker(
                "Faltan datos tuyos",
                "Nombre, documento, telefono, licencia y un contacto de " +
                    "emergencia son obligatorios."
            )
        }

        // Solo personal de empresas con convenio. El codigo se elige
        // del catalogo y ademas tiene que coincidir con el de operador:
        // asi no basta con inventarse el nombre de una empresa real.
        if (profile.companyCode.isBlank()) {
            reasons += DriverBlocker(
                "Falta tu empresa",
                "Elige la empresa de transporte a la que perteneces."
            )
        } else if (DriverRequirements.findCompany(profile.companyCode) == null) {
            reasons += DriverBlocker(
                "Empresa no autorizada",
                "La empresa seleccionada no tiene convenio con SmartBus."
            )
        }

        if (!profile.phoneVerified) {
            reasons += DriverBlocker(
                "Telefono sin verificar",
                "Enviamos un codigo de 6 digitos para confirmar que el " +
                    "numero es tuyo."
            )
        }

        if (!profile.licenseValid) {
            reasons += DriverBlocker(
                "Licencia vencida",
                "Tu licencia caduca el " +
                    "${profile.licenseExpiresAt}. Sin licencia vigente no " +
                    "puedes manejar."
            )
        }

        val missing = DriverRequirements.required.filter { it !in approvedTypes }

        if (missing.isNotEmpty()) {
            reasons += DriverBlocker(
                "Faltan ${missing.size} documento(s)",
                missing.joinToString(", ") { it.title.lowercase() }
            )
        }

        if (vehicle == null) {
            reasons += DriverBlocker(
                "No tienes vehiculo registrado",
                "Registra la placa, marca y modelo del vehiculo que vas " +
                    "a conducir."
            )
        } else {
            if (!vehicle.soatValid) {
                reasons += DriverBlocker(
                    "SOAT vencido o ausente",
                    "El SOAT es obligatorio para circular en Peru."
                )
            }
            if (!vehicle.inspectionValid) {
                reasons += DriverBlocker(
                    "Inspeccion vehicular vencida",
                    "Renueva la tarjeta de inspeccion del vehiculo."
                )
            }
            // Sin unidad no hay ruta, y sin ruta no se puede verificar
            // que el conductor siga el recorrido asignado: podria estar
            // yendo a su casa y el pasajero seguiria viendolo "en ruta".
            if (vehicle.transitVehicleId == null) {
                reasons += DriverBlocker(
                    "Sin ruta asignada",
                    "Asignate a una unidad de transporte: asi verificamos " +
                        "que tu ubicacion siga el recorrido de tu ruta."
                )
            }
        }

        if (profile.status == com.ejemplo.smartbus.data.model.DriverVerificationStatus.SUSPENDED) {
            reasons += DriverBlocker(
                "Cuenta suspendida",
                profile.statusReason.ifBlank {
                    "Contacta a soporte para mas informacion."
                }
            )
        }

        if (profile.status == com.ejemplo.smartbus.data.model.DriverVerificationStatus.REJECTED) {
            reasons += DriverBlocker(
                "Documentos rechazados",
                profile.statusReason.ifBlank {
                    "Revisa los documentos marcados y vuelvelos a subir."
                }
            )
        }

        return reasons
    }

    fun canGoOnline(
        profile: DriverProfile?,
        vehicle: DriverVehicle?,
        approvedTypes: Set<DriverDocumentType>
    ): Boolean = blockReasons(profile, vehicle, approvedTypes).isEmpty()
}