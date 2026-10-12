package com.ejemplo.smartbus.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ejemplo.smartbus.BuildConfig
import com.ejemplo.smartbus.data.driver.DriverRepository
import com.ejemplo.smartbus.data.driver.DriverRequirements
import com.ejemplo.smartbus.data.driver.DriverValidators
import com.ejemplo.smartbus.data.driver.TrustedCompany
import com.ejemplo.smartbus.data.model.DriverProfile
import com.ejemplo.smartbus.ui.theme.SmartBusBackground
import com.ejemplo.smartbus.ui.theme.SmartBusBlue
import com.ejemplo.smartbus.ui.theme.SmartBusGreen
import com.ejemplo.smartbus.ui.theme.SmartBusRed
import com.ejemplo.smartbus.ui.theme.SmartBusText
import com.ejemplo.smartbus.ui.theme.SmartBusTextSecondary
import com.ejemplo.smartbus.ui.theme.SmartBusYellow
import kotlinx.coroutines.launch

/**
 * Alta de conductor, paso a paso.
 *
 * El paso 1 son los datos de la persona. Los pasos de documentos y de
 * vehiculo siguen en [DriverDocumentsScreen] y [DriverVehicleScreen].
 */
@Composable
fun DriverOnboardingScreen(
    accountId: String,
    repository: DriverRepository,
    existing: DriverProfile?,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {

    val scope = rememberCoroutineScope()

    var fullName by remember { mutableStateOf(existing?.fullName.orEmpty()) }
    var documentType by remember { mutableStateOf(existing?.documentType ?: "DNI") }
    var documentId by remember { mutableStateOf(existing?.documentId.orEmpty()) }
    var birthDate by remember { mutableStateOf(existing?.birthDate.orEmpty()) }
    var phone by remember { mutableStateOf(existing?.phone.orEmpty()) }
    var licenseNumber by remember { mutableStateOf(existing?.licenseNumber.orEmpty()) }
    var licenseClass by remember { mutableStateOf(existing?.licenseClass.orEmpty()) }
    var licenseExpiry by remember { mutableStateOf("") }
    var emergencyName by remember { mutableStateOf(existing?.emergencyContactName.orEmpty()) }
    var emergencyPhone by remember {
        mutableStateOf(existing?.emergencyContactPhone.orEmpty())
    }

    /**
     * Empresa de transporte con convenio. No se escribe a mano: se
     * elige de la lista de confianza y se confirma con el codigo de
     * operador que entrega la empresa.
     */
    var companyCode by remember { mutableStateOf(existing?.companyCode.orEmpty()) }
    var operatorCode by remember { mutableStateOf("") }
    var showCompanyPicker by remember { mutableStateOf(false) }

    val selectedCompany = remember(companyCode) {
        DriverRequirements.findCompany(companyCode)
    }

    /**
     * La empresa ya valia si el perfil guardado la tenia. Si es nueva,
     * el codigo de operador tiene que coincidir exactamente.
     */
    val companyValid = remember(selectedCompany, operatorCode, existing) {
        when {
            selectedCompany == null -> false
            existing?.companyCode.equals(companyCode, ignoreCase = true) &&
                existing?.companyName == selectedCompany.name -> true
            else -> selectedCompany.operatorCode.equals(
                operatorCode.trim(),
                ignoreCase = true
            )
        }
    }

    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    /**
     * Verificacion del celular: un codigo de 6 digitos por cuenta. Sin
     * esto el conductor no puede ponerse online, aunque lo demas este
     * completo.
     */
    val verifiedPhone = remember(existing, phone) {
        existing?.phoneVerified == true && existing?.phone == phone.trim()
    }

    var lastSentCode by remember { mutableStateOf<String?>(null) }
    var otpInput by remember { mutableStateOf("") }
    var verifying by remember { mutableStateOf(false) }
    var phoneJustVerified by remember { mutableStateOf(false) }

    /**
     * El DNI peruano lleva un digito de control. Validarlo aqui evita
     * que el conductor escriba cualquier numero y que el sistema le
     * acepte un documento que no existe.
     */
    val documentValid = remember(documentId, documentType) {
        when (documentType.uppercase()) {
            "DNI" -> DriverValidators.isValidPeruvianDni(documentId)
            "PASAPORTE" -> documentId.trim().length >= 6
            else -> documentId.isNotBlank()
        }
    }

    val phoneValid = remember(phone) {
        DriverValidators.isValidPeruvianPhone(phone)
    }

    val birthValid = remember(birthDate) {
        birthDate.isBlank() || DriverValidators.isValidBirthDate(birthDate)
    }

    val emergencyValid = remember(emergencyPhone) {
        DriverValidators.isValidPeruvianPhone(emergencyPhone)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SmartBusBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {

        TextButton(onClick = onBack) {
            Text("Volver")
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Conduce con SmartBus",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = SmartBusText
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Necesitamos saber quien eres y que vehiculo manejas. " +
                "Tus datos se revisan antes de que puedas trabajar.",
            style = MaterialTheme.typography.bodyMedium,
            color = SmartBusTextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        DriverPermissionsCard()

        Spacer(modifier = Modifier.height(16.dp))

        DriverRequirementsCard()

        Spacer(modifier = Modifier.height(20.dp))

        SectionTitle("Tus datos")

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Nombre completo") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {

            Box(modifier = Modifier.width(120.dp)) {

                OutlinedTextField(
                    value = documentType,
                    onValueChange = { documentType = it.uppercase() },
                    label = { Text("Tipo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = documentId,
                onValueChange = { documentId = it.trim() },
                label = { Text("Numero de documento") },
                singleLine = true,
                isError = documentId.isNotBlank() && !documentValid,
                supportingText = if (documentId.isNotBlank() && !documentValid) {
                    {
                        Text(
                            if (documentType.uppercase() == "DNI") {
                                "El digito de control no cuadra."
                            } else {
                                "Documento demasiado corto."
                            }
                        )
                    }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = birthDate,
            onValueChange = { birthDate = it },
            label = { Text("Fecha de nacimiento (AAAA-MM-DD)") },
            singleLine = true,
            isError = !birthValid,
            supportingText = if (!birthValid) {
                { Text("Debes ser mayor de 18 anos.") }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it.filter { c -> c.isDigit() } },
            label = { Text("Celular (9 digitos)") },
            singleLine = true,
            isError = phone.isNotBlank() && !phoneValid,
            supportingText = {
                Text(
                    if (phoneValid) {
                        "Te enviaremos un codigo para confirmarlo."
                    } else {
                        "Formato: 9xxxxxxxx"
                    }
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )

        if (phone.isNotBlank() && phoneValid && !verifiedPhone && !phoneJustVerified) {

            PhoneVerificationCard(
                verifying = verifying,
                verified = verifiedPhone || phoneJustVerified,
                otpInput = otpInput,
                lastSentCode = lastSentCode,
                onOtpChange = {
                    otpInput = it.filter { c -> c.isDigit() }.take(6)
                },
                onRequestCode = {
                    scope.launch {
                        lastSentCode = repository.requestPhoneCode(accountId)
                    }
                },
                onVerify = {
                    scope.launch {
                        verifying = true
                        phoneJustVerified =
                            repository.verifyPhoneCode(accountId, otpInput)
                        verifying = false
                        if (phoneJustVerified) {
                            otpInput = ""
                            lastSentCode = null
                        }
                    }
                }
            )
        } else if ((verifiedPhone || phoneJustVerified) && phone.isNotBlank()) {

            PhoneVerifiedCard()
        }

        Spacer(modifier = Modifier.height(20.dp))

        SectionTitle("Tu empresa")

        Text(
            text = "SmartBus solo admite conductores de empresas con " +
                "convenio. Elige la tuya y confirma con el codigo de " +
                "operador que te entrega la empresa.",
            style = MaterialTheme.typography.bodySmall,
            color = SmartBusTextSecondary
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            onClick = { showCompanyPicker = true }
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Business,
                    contentDescription = null,
                    tint = if (companyValid) SmartBusGreen else SmartBusBlue,
                    modifier = Modifier.size(22.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Empresa de transporte",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = SmartBusText
                    )
                    Text(
                        text = selectedCompany?.name
                            ?: "Toca para elegir tu empresa",
                        style = MaterialTheme.typography.bodySmall,
                        color = SmartBusTextSecondary
                    )
                }

                if (companyValid) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Empresa confirmada",
                        tint = SmartBusGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (selectedCompany != null) {

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = operatorCode,
                onValueChange = { operatorCode = it.uppercase() },
                label = { Text("Codigo de operador") },
                placeholder = { Text(selectedCompany.operatorCode) },
                singleLine = true,
                isError = operatorCode.isNotBlank() && !companyValid,
                supportingText = {
                    Text(
                        if (companyValid) {
                            "Empresa verificada con SmartBus."
                        } else {
                            "Te lo entrega tu empresa al firmar el convenio."
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (showCompanyPicker) {

            CompanyPickerDialog(
                selectedCode = companyCode,
                onSelect = { company ->
                    companyCode = company.code
                    operatorCode = ""
                    showCompanyPicker = false
                },
                onDismiss = { showCompanyPicker = false }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        SectionTitle("Tu licencia")

        OutlinedTextField(
            value = licenseNumber,
            onValueChange = { licenseNumber = it.trim() },
            label = { Text("Numero de licencia") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {

            OutlinedTextField(
                value = licenseClass,
                onValueChange = { licenseClass = it.uppercase() },
                label = { Text("Clase") },
                singleLine = true,
                placeholder = { Text("A-IIIb") },
                isError = licenseClass.isNotBlank() &&
                    !DriverValidators.isLicenseClassAllowedForPassengers(licenseClass),
                supportingText = {
                    Text(
                        if (DriverValidators.isLicenseClassAllowedForPassengers(licenseClass)) {
                            "Aceptada"
                        } else {
                            "Se requiere A-III"
                        }
                    )
                },
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = licenseExpiry,
                onValueChange = { licenseExpiry = it },
                label = { Text("Vence (AAAA-MM-DD)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        SectionTitle("Contacto de emergencia")

        Text(
            text = "Si no respondes durante un viaje, avisamos a esta persona.",
            style = MaterialTheme.typography.bodySmall,
            color = SmartBusTextSecondary
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = emergencyName,
            onValueChange = { emergencyName = it },
            label = { Text("Nombre") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = emergencyPhone,
            onValueChange = { emergencyPhone = it.filter { c -> c.isDigit() } },
            label = { Text("Celular") },
            singleLine = true,
            isError = emergencyPhone.isNotBlank() && !emergencyValid,
            supportingText = {
                Text(if (emergencyValid) "Correcto" else "Formato: 9xxxxxxxx")
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {

            Spacer(modifier = Modifier.height(14.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = SmartBusRed.copy(alpha = 0.10f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = SmartBusRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = error.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = SmartBusRed
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {

                error = null

                when {

                    fullName.trim().split(" ").size < 2 ->
                        error = "Escribe tu nombre y apellido."

                    documentId.isBlank() ->
                        error = "Falta tu documento de identidad."

                    !documentValid ->
                        error = "El documento no es valido. Revisa el numero."

                    !birthValid ->
                        error = "La fecha de nacimiento no es valida o no " +
                            "cumples los 18 anos."

                    !phoneValid ->
                        error = "El celular debe tener 9 digitos y empezar por 9."

                    licenseNumber.isBlank() ->
                        error = "Falta el numero de licencia."

                    licenseClass.isNotBlank() &&
                        !DriverValidators.isLicenseClassAllowedForPassengers(licenseClass) ->
                        error = "Tu clase de licencia no permite transportar " +
                            "pasajeros. Se requiere A-III."

                    emergencyName.isBlank() || emergencyPhone.isBlank() ->
                        error = "Necesitamos un contacto de emergencia."

                    !emergencyValid ->
                        error = "El celular de emergencia debe tener 9 digitos."

                    selectedCompany == null ->
                        error = "Elige la empresa de transporte a la que " +
                            "perteneces."

                    !companyValid ->
                        error = "El codigo de operador no coincide con la " +
                            "empresa seleccionada."

                    !verifiedPhone && !phoneJustVerified ->
                        error = "Verifica tu celular con el codigo de 6 " +
                            "digitos que te enviamos por SMS."

                    else -> {

                        saving = true

                        scope.launch {

                            if (
                                repository.isDocumentAlreadyRegistered(
                                    documentId = documentId.trim(),
                                    exceptAccountId = accountId
                                )
                            ) {
                                error = "Ese documento ya esta registrado " +
                                    "con otra cuenta."
                                saving = false
                                return@launch
                            }

                            val expiryMillis = parseDate(licenseExpiry)

                            repository.saveProfile(
                                DriverProfile(
                                    accountId = accountId,
                                    fullName = fullName.trim(),
                                    documentId = documentId.trim(),
                                    documentType = documentType.trim().uppercase(),
                                    companyName = selectedCompany?.name.orEmpty(),
                                    companyCode = selectedCompany?.code.orEmpty(),
                                    birthDate = birthDate.trim().ifBlank { null },
                                    phone = phone.trim(),
                                    licenseNumber = licenseNumber.trim(),
                                    licenseClass = licenseClass.trim().uppercase(),
                                    licenseExpiresAt = expiryMillis,
                                    emergencyContactName = emergencyName.trim(),
                                    emergencyContactPhone = emergencyPhone.trim(),
                                    phoneVerified = verifiedPhone || phoneJustVerified,
                                    status = existing?.status
                                        ?: com.ejemplo.smartbus.data.model
                                            .DriverVerificationStatus.NOT_STARTED,
                                    statusReason = existing?.statusReason.orEmpty(),
                                    rating = existing?.rating ?: 0f,
                                    totalTrips = existing?.totalTrips ?: 0,
                                    createdAt = existing?.createdAt
                                        ?: System.currentTimeMillis()
                                )
                            )

                            saving = false
                            onSaved()
                        }
                    }
                }
            },
            enabled = !saving,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (saving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Guardar y continuar")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = SmartBusBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Que sigue despues",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = SmartBusText
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                DriverRequirements.steps.drop(1).forEach { step ->
                    Text(
                        text = "- ${step.title}: ${step.items.size} documento(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = SmartBusTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = SmartBusText,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

/**
 * Tarjeta para pedir el codigo y confirmarlo. En la version de prueba
 * el codigo se muestra aqui mismo porque todavia no hay SMS de verdad.
 */
@Composable
private fun PhoneVerificationCard(
    verifying: Boolean,
    verified: Boolean,
    otpInput: String,
    lastSentCode: String?,
    onOtpChange: (String) -> Unit,
    onRequestCode: () -> Unit,
    onVerify: () -> Unit
) {

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {

                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = SmartBusBlue,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Verifica tu celular",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SmartBusText
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Para proteger tu cuenta, te enviamos un codigo de " +
                    "6 digitos. Sin confirmarlo no puedes ponerte en servicio.",
                style = MaterialTheme.typography.bodySmall,
                color = SmartBusTextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (lastSentCode == null) {

                OutlinedButton(
                    onClick = onRequestCode,
                    enabled = !verifying,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enviar codigo")
                }

            } else {

                OutlinedTextField(
                    value = otpInput,
                    onValueChange = onOtpChange,
                    label = { Text("Codigo de 6 digitos") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = otpInput.length == 6 && !phoneCodeValid(otpInput, lastSentCode) && !verified,
                    supportingText = {
                        if (BuildConfig.DEBUG && lastSentCode != null) {
                            Text("Version de prueba. Tu codigo es: $lastSentCode")
                        } else {
                            Text("Vence en 5 minutos.")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onVerify,
                    enabled = otpInput.length == 6 && !verifying,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (verifying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Confirmar codigo")
                    }
                }
            }
        }
    }
}

/**
 * Estado final: el numero ya fue confirmado.
 */
@Composable
private fun PhoneVerifiedCard() {

    Card(
        colors = CardDefaults.cardColors(
            containerColor = SmartBusGreen.copy(alpha = 0.10f)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = SmartBusGreen,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Celular verificado",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = SmartBusGreen
            )
        }
    }
}

/**
 * Ayuda a mostrar un error si el codigo no coincide antes de
 * presionar Confirmar: compara el codigo guardado.
 */
private fun phoneCodeValid(input: String, sent: String?): Boolean =
    input.trim() == sent

/**
 * Convierte AAAA-MM-DD a milisegundos, o null si no se puede.
 */
internal fun parseDate(value: String): Long? {

    val clean = value.trim()

    if (clean.isEmpty()) {
        return null
    }

    val parts = clean.split("-")

    if (parts.size != 3) {
        return null
    }

    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null

    if (month !in 1..12 || day !in 1..31) {
        return null
    }

    return java.util.Calendar.getInstance().apply {
        clear()
        set(year, month - 1, day, 23, 59, 59)
    }.timeInMillis
}

/**
 * Permisos que la app necesita para operar como conductor.
 *
 * Se piden aqui, con explicacion, y no al arrancar la app: nadie debe
 * ver dialogos de sistema sin saber para que son. La ubicacion es para
 * que el pasajero vea el vehiculo; la de segundo plano para que siga
 * compartiendose con la pantalla apagada; las notificaciones para
 * mantener vivo el servicio de GPS.
 */
@Composable
private fun DriverPermissionsCard() {

    val context = LocalContext.current

    fun granted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED

    var locationGranted by remember {
        mutableStateOf(granted(Manifest.permission.ACCESS_FINE_LOCATION))
    }

    var backgroundGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                granted(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        )
    }

    var notificationsGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                granted(Manifest.permission.POST_NOTIFICATIONS)
        )
    }

    val powerManager = context.getSystemService(PowerManager::class.java)

    fun ignoringBatteryOptimizations(): Boolean =
        powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true

    var batteryOptimized by remember {
        mutableStateOf(!ignoringBatteryOptimizations())
    }

    // Al volver de los ajustes del sistema hay que releer el estado: el
    // usuario pudo haber cambiado el permiso mientras la app no se veia.
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                batteryOptimized = !ignoringBatteryOptimizations()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val foregroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        locationGranted =
            result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationsGranted =
                result[Manifest.permission.POST_NOTIFICATIONS] == true
        }
    }

    val backgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { backgroundGranted = it }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = SmartBusBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Permisos para trabajar",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SmartBusText
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            PermissionRow(
                icon = Icons.Default.LocationOn,
                title = "Ubicacion",
                detail = "Para mostrar tu vehiculo al pasajero y calcular su llegada.",
                granted = locationGranted
            )

            PermissionRow(
                icon = Icons.Default.Notifications,
                title = "Notificaciones",
                detail = "Avisos de viaje y para mantener el GPS activo.",
                granted = notificationsGranted
            )

            PermissionRow(
                icon = Icons.Default.LocationOn,
                title = "Ubicacion en segundo plano",
                detail = "Sigue compartiendo tu posicion con la pantalla apagada.",
                granted = backgroundGranted
            )

            PermissionRow(
                icon = Icons.Default.BatteryFull,
                title = "Sin ahorro de bateria",
                detail = "Evita que Android corte el GPS al apagar la pantalla.",
                granted = !batteryOptimized
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val permissions = mutableListOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissions += Manifest.permission.POST_NOTIFICATIONS
                    }
                    foregroundLauncher.launch(permissions.toTypedArray())
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Activar permisos")
            }

            if (locationGranted && !backgroundGranted &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        backgroundLauncher.launch(
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Permitir ubicacion siempre")
                }
            }

            if (batteryOptimized) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        try {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    Uri.parse("package:${context.packageName}")
                                )
                            )
                        } catch (_: Exception) {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:${context.packageName}")
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Trabajar siempre (sin ahorro de bateria)")
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    detail: String,
    granted: Boolean
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (granted) Icons.Default.CheckCircle else icon,
            contentDescription = null,
            tint = if (granted) SmartBusGreen else SmartBusBlue,
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = SmartBusText
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = SmartBusTextSecondary
            )
        }
    }
}

/**
 * Resumen de lo que se le va a pedir al conductor. Se muestra antes de
 * empezar para que nadie se sorprenda a mitad del registro.
 */
@Composable
private fun DriverRequirementsCard() {

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SmartBusGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Requisitos para ser conductor",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SmartBusText
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            DriverRequirements.steps.forEach { step ->
                Row(
                    modifier = Modifier.padding(vertical = 5.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = SmartBusBlue,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SmartBusText
                        )
                        Text(
                            text = step.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = SmartBusTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Ademas: verificar tu celular y pertenecer a una " +
                    "empresa de transporte con convenio con SmartBus.",
                style = MaterialTheme.typography.bodySmall,
                color = SmartBusYellow
            )
        }
    }
}

/**
 * Selector de empresa. Solo lista las que tienen convenio; no se puede
 * escribir una libre.
 */
@Composable
private fun CompanyPickerDialog(
    selectedCode: String,
    onSelect: (TrustedCompany) -> Unit,
    onDismiss: () -> Unit
) {

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Elige tu empresa") },
        text = {
            Column {
                Text(
                    text = "Solo aparecen empresas con convenio firmado " +
                        "con SmartBus. Si la tuya no esta, pide a tu " +
                        "operador que la registre.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SmartBusTextSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                DriverRequirements.companies.forEach { company ->

                    val selected =
                        company.code.equals(selectedCode, ignoreCase = true)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(company) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { onSelect(company) }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Text(
                                text = company.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = SmartBusText
                            )
                            Text(
                                text = "Codigo: ${company.code}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SmartBusTextSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}