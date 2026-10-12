package com.ejemplo.smartbus.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ejemplo.smartbus.data.model.BusStop
import com.ejemplo.smartbus.data.model.Report
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportsScreen(
    reports: List<Report>,
    stops: List<BusStop>,
    onSaveReport: (
        String,
        String,
        Int?,
        String?,
        Int?,
        String?,
        String?
    ) -> Unit,
    onAttachPhoto: (() -> Unit)? = null,
    attachedPhotoUri: String? = null,
    onBack: () -> Unit
) {

    var selectedType by remember {
        mutableStateOf("Retraso")
    }

    var description by remember {
        mutableStateOf("")
    }

    var selectedStopId by remember {
        mutableStateOf<Int?>(null)
    }

    val reportTypes = listOf(
        "Retraso",
        "Desvío de ruta",
        "Bus lleno",
        "Problema del servicio",
        "Otro"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // =====================================================
        // TÍTULO
        // =====================================================

        Text(
            text = "Reportar problema Reportar problema",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        // =====================================================
        // CONTENIDO
        // =====================================================

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // =================================================
            // TIPO DE REPORTE
            // =================================================

            item {

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Text(
                            text = "Tipo de problema",
                            style = MaterialTheme.typography.titleLarge
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        reportTypes.forEach { type ->

                            Row(
                                modifier = Modifier.fillMaxWidth()
                            ) {

                                RadioButton(
                                    selected =
                                        selectedType == type,

                                    onClick = {
                                        selectedType = type
                                    }
                                )

                                Text(
                                    text = type,
                                    modifier = Modifier.padding(
                                        top = 12.dp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // =================================================
            // PARADERO
            // =================================================

            item {

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Text(
                            text = "Paradero relacionado Paradero relacionado",
                            style = MaterialTheme.typography.titleLarge
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            RadioButton(
                                selected =
                                    selectedStopId == null,

                                onClick = {
                                    selectedStopId = null
                                }
                            )

                            Text(
                                text = "Sin paradero específico",
                                modifier = Modifier.padding(
                                    top = 12.dp
                                )
                            )
                        }

                        stops.forEach { stop ->

                            Row(
                                modifier = Modifier.fillMaxWidth()
                            ) {

                                RadioButton(
                                    selected =
                                        selectedStopId == stop.id,

                                    onClick = {
                                        selectedStopId = stop.id
                                    }
                                )

                                Text(
                                    text = stop.name,
                                    modifier = Modifier.padding(
                                        top = 12.dp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // =================================================
            // DESCRIPCIÓN
            // =================================================

            item {

                OutlinedTextField(
                    value = description,

                    onValueChange = {
                        description = it
                    },

                    modifier = Modifier.fillMaxWidth(),

                    label = {
                        Text("Describe el problema")
                    },

                    placeholder = {
                        Text(
                            "Ejemplo: el bus lleva 20 minutos de retraso"
                        )
                    },

                    minLines = 4,
                    maxLines = 6
                )
            }

            // =================================================
            // ESPACIO ANTES DEL HISTORIAL
            // =================================================

            item {

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "Mis reportes Mis reportes",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            // =================================================
            // SIN REPORTES
            // =================================================

            if (reports.isEmpty()) {

                item {

                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {

                            Text(
                                text =
                                    "Todavía no has enviado reportes."
                            )

                            Spacer(
                                modifier = Modifier.height(4.dp)
                            )

                            Text(
                                text =
                                    "Tus reportes aparecerán aquí después de enviarlos.",
                                style =
                                    MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

            } else {

                // =============================================
                // LISTA DE REPORTES
                // =============================================

                items(
                    items = reports,
                    key = { report ->
                        report.id
                    }
                ) { report ->

                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {

                            Text(
                                text =
                                    "Reportar problema ${report.type}",

                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(6.dp)
                            )

                            Text(
                                text =
                                    report.description
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(6.dp)
                            )

                            if (
                                report.stopName != null
                            ) {

                                Text(
                                    text =
                                        "Paradero relacionado ${report.stopName}"
                                )


                                Spacer(
                                    modifier =
                                        Modifier.height(4.dp)
                                )
                            }

                            val date =
                                SimpleDateFormat(
                                    "dd/MM/yyyy HH:mm",
                                    Locale.getDefault()
                                ).format(
                                    Date(
                                        report.timestamp
                                    )
                                )

                            Text(
                                text =
                                    "Fecha $date"
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(4.dp)
                            )

                            Text(
                                text =
                                    "Estado: ${report.status}"
                            )
                        }
                    }
                }
            }
        }

        // =====================================================
        // BOTÓN ENVIAR
        // =====================================================

        Button(
            onClick = {

                val selectedStop =
                    stops.firstOrNull {
                        it.id == selectedStopId
                    }

                onSaveReport(
                    selectedType,
                    description.trim(),
                    selectedStop?.id,
                    selectedStop?.name,
                    null,
                    null,
                    attachedPhotoUri
                )

                description = ""
                selectedType = "Retraso"
                selectedStopId = null
            },

            enabled =
                description.trim().isNotEmpty(),

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                text = "Reportar problema Enviar reporte"
            )
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        // =====================================================
        // BOTÓN VOLVER
        // =====================================================

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                text = "Volver"
            )
        }
    }
}