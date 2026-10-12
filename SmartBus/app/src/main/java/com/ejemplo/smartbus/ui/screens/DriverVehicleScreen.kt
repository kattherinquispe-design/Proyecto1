package com.ejemplo.smartbus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.BusAlert
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ejemplo.smartbus.data.driver.DriverRepository
import com.ejemplo.smartbus.data.driver.DriverValidators
import com.ejemplo.smartbus.data.model.DriverVehicle
import com.ejemplo.smartbus.ui.theme.SmartBusBackground
import com.ejemplo.smartbus.ui.theme.SmartBusBlue
import com.ejemplo.smartbus.ui.theme.SmartBusGreen
import com.ejemplo.smartbus.ui.theme.SmartBusRed
import com.ejemplo.smartbus.ui.theme.SmartBusText
import com.ejemplo.smartbus.ui.theme.SmartBusTextSecondary
import com.ejemplo.smartbus.ui.theme.SmartBusYellow
import kotlinx.coroutines.launch

/**
 * Registro del vehiculo que va a conducir.
 *
 * La placa se valida con el formato peruano y se comprueba que no
 * este registrada con otro conductor: dos conductores con la misma
 * placa significaria una Confusion de identidad o un robo de vehiculo.
 */
/**
 * Unidad de transporte a la que puede asignarse un conductor.
 */
data class VehicleUnit(
    val id: Int,
    val code: String,
    val routeName: String
)

@Composable
fun DriverVehicleScreen(
    accountId: String,
    repository: DriverRepository,
    existing: DriverVehicle?,
    transitUnits: List<VehicleUnit>,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {

    val scope = rememberCoroutineScope()

    var plate by remember { mutableStateOf(existing?.plate.orEmpty()) }
    var brand by remember { mutableStateOf(existing?.brand.orEmpty()) }
    var model by remember { mutableStateOf(existing?.model.orEmpty()) }
    var year by remember { mutableStateOf(existing?.year?.takeIf { it > 0 }?.toString().orEmpty()) }
    var color by remember { mutableStateOf(existing?.color.orEmpty()) }
    var capacity by remember { mutableStateOf(existing?.capacity?.takeIf { it > 0 }?.toString().orEmpty()) }
    var soatExpiry by remember { mutableStateOf("") }
    var inspectionExpiry by remember { mutableStateOf("") }
    var isPrimary by remember { mutableStateOf(existing?.isPrimary ?: true) }
    var transitVehicleId by remember { mutableStateOf(existing?.transitVehicleId) }
    var showUnitPicker by remember { mutableStateOf(false) }

    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    val plateValid = remember(plate) {
        plate.isBlank() || DriverValidators.isValidPlate(plate)
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

        Row(verticalAlignment = Alignment.CenterVertically) {

            Icon(
                Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = SmartBusBlue,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "Tu vehiculo",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = SmartBusText
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "El pasajero ve la marca, el modelo y la placa antes de " +
                "subir. Por eso los datos tienen que ser los reales.",
            style = MaterialTheme.typography.bodySmall,
            color = SmartBusTextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = plate,
            onValueChange = { plate = it.uppercase() },
            label = { Text("Placa") },
            placeholder = { Text("ABC-123") },
            singleLine = true,
            isError = plate.isNotBlank() && !plateValid,
            supportingText = {
                Text(
                    if (plateValid) {
                        "Placa peruana"
                    } else {
                        "Formato: ABC-123"
                    }
                )
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {

            OutlinedTextField(
                value = brand,
                onValueChange = { brand = it },
                label = { Text("Marca") },
                placeholder = { Text("Hyundai") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("Modelo") },
                placeholder = { Text("H1") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {

            OutlinedTextField(
                value = year,
                onValueChange = { year = it.filter { c -> c.isDigit() }.take(4) },
                label = { Text("Ano") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = color,
                onValueChange = { color = it },
                label = { Text("Color") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = capacity,
            onValueChange = { capacity = it.filter { c -> c.isDigit() }.take(2) },
            label = { Text("Puestos de pasajeros") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Documentos del vehiculo",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = SmartBusText
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = soatExpiry,
            onValueChange = { soatExpiry = it },
            label = { Text("SOAT vence (AAAA-MM-DD)") },
            supportingText = {
                Text("El SOAT es obligatorio para circular en Peru.")
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = inspectionExpiry,
            onValueChange = { inspectionExpiry = it },
            label = { Text("Inspeccion vehicular vence (AAAA-MM-DD)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Vehiculo principal",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = SmartBusText
                    )
                    Text(
                        text = "Es el que aparece en el mapa cuando estas " +
                            "en servicio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SmartBusTextSecondary
                    )
                }
                Switch(checked = isPrimary, onCheckedChange = { isPrimary = it })
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        /**
         * Unidad de transporte.
         *
         * El mapa del pasajero dibuja vehiculos, no conductores. Sin
         * esta asignacion el conductor comparte GPS en segundo plano
         * pero el pasajero no lo ve aparecer en ninguna parte, por eso
         * se pide de forma explicita y se explica el por que.
         */
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            onClick = { showUnitPicker = true }
        ) {
            Column(modifier = Modifier.padding(14.dp)) {

                Row(verticalAlignment = Alignment.CenterVertically) {

                    Icon(
                        Icons.Default.BusAlert,
                        contentDescription = null,
                        tint = if (transitVehicleId == null) SmartBusYellow else SmartBusGreen,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {

                        Text(
                            text = "Unidad de transporte",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = SmartBusText
                        )

                        Text(
                            text = transitUnits
                                .firstOrNull { it.id == transitVehicleId }
                                ?.let { "${it.code} - ${it.routeName}" }
                                ?: "Toca para elegir donde trabajas",
                            style = MaterialTheme.typography.bodySmall,
                            color = SmartBusTextSecondary
                        )
                    }
                }

                if (transitVehicleId == null) {

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Sin unidad no te veran los pasajeros, aunque " +
                            "el GPS este funcionando.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SmartBusYellow
                    )
                }
            }
        }

        if (showUnitPicker) {

            AlertDialog(
                onDismissRequest = { showUnitPicker = false },
                title = { Text("Elige tu unidad") },
                text = {
                    Column {
                        Text(
                            text = "Elige la unidad a la que perteneces. " +
                                "En una flota real esto lo asigna la " +
                                "empresa; aqui lo eliges tu.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SmartBusTextSecondary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        transitUnits.forEach { unit ->

                            val selected = unit.id == transitVehicleId

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        transitVehicleId = unit.id
                                        showUnitPicker = false
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                RadioButton(
                                    selected = selected,
                                    onClick = {
                                        transitVehicleId = unit.id
                                        showUnitPicker = false
                                    }
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Column {
                                    Text(
                                        text = unit.code,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SmartBusText
                                    )
                                    Text(
                                        text = unit.routeName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SmartBusTextSecondary
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showUnitPicker = false }) {
                        Text("Cerrar")
                    }
                }
            )
        }

        if (error != null) {

            Spacer(modifier = Modifier.height(14.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SmartBusRed.copy(alpha = 0.10f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = SmartBusRed,
                        modifier = Modifier.size(18.dp)
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

                    !DriverValidators.isValidPlate(plate) ->
                        error = "La placa no tiene el formato peruano."

                    plate.isBlank() ->
                        error = "Falta la placa."

                    brand.isBlank() ->
                        error = "Falta la marca."

                    model.isBlank() ->
                        error = "Falta el modelo."

                    year.toIntOrNull()?.let { it < 1990 } == true ->
                        error = "El ano no parece correcto."

                    soatExpiry.isBlank() ->
                        error = "Necesitamos la fecha de vencimiento del SOAT."

                    parseDate(soatExpiry)?.let {
                        it <= System.currentTimeMillis()
                    } == true ->
                        error = "El SOAT esta vencido. No puedes trabajar " +
                            "con el SOAT vencido."

                    else -> {

                        saving = true

                        scope.launch {

                            val cleanPlate = plate.trim().uppercase()

                            val others = repository
                                .getVehicles(accountId)
                                .filter { it.plateNormalized != cleanPlate }

                            val alreadyTaken =
                                repository.isPlateAlreadyRegistered(cleanPlate) &&
                                    others.isEmpty()

                            if (alreadyTaken) {
                                error = "Esa placa ya esta registrada con " +
                                    "otro conductor."
                                saving = false
                                return@launch
                            }

                            repository.saveVehicle(
                                DriverVehicle(
                                    id = existing?.id ?: 0,
                                    accountId = accountId,
                                    plate = cleanPlate,
                                    brand = brand.trim(),
                                    model = model.trim(),
                                    year = year.toIntOrNull() ?: 0,
                                    color = color.trim(),
                                    plateCount = existing?.plateCount ?: 0,
                                    capacity = capacity.toIntOrNull() ?: 0,
                                    isPrimary = isPrimary,
                                    soatExpiresAt = parseDate(soatExpiry),
                                    inspectionExpiresAt = parseDate(inspectionExpiry),
                                    status = existing?.status
                                        ?: com.ejemplo.smartbus.data.model
                                            .DriverVerificationStatus.NOT_STARTED,
                                    transitVehicleId = transitVehicleId,
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
                Text("Guardar vehiculo")
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}