package com.ejemplo.smartbus.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ejemplo.smartbus.data.driver.DriverRequirements
import com.ejemplo.smartbus.data.driver.DriverStep
import com.ejemplo.smartbus.data.model.DriverDocument
import com.ejemplo.smartbus.data.model.DriverDocumentStatus
import com.ejemplo.smartbus.data.model.DriverDocumentType
import com.ejemplo.smartbus.ui.theme.SmartBusBackground
import com.ejemplo.smartbus.ui.theme.SmartBusBlue
import com.ejemplo.smartbus.ui.theme.SmartBusDivider
import com.ejemplo.smartbus.ui.theme.SmartBusGreen
import com.ejemplo.smartbus.ui.theme.SmartBusRed
import com.ejemplo.smartbus.ui.theme.SmartBusText
import com.ejemplo.smartbus.ui.theme.SmartBusTextSecondary
import com.ejemplo.smartbus.ui.theme.SmartBusYellow

/**
 * Checklist de documentos del conductor.
 *
 * Cada punto es un requisito real de las plataformas de transporte.
 * Esta pantalla es la que explica por que se pide cada cosa, para que
 * el conductor entienda que esta defendiendo su cuenta y no una foto
 * mas.
 */
@Composable
fun DriverDocumentsScreen(
    documents: List<DriverDocument>,
    approvedTypes: Set<DriverDocumentType>,
    onUpload: (DriverDocumentType) -> Unit,
    onDelete: (DriverDocumentType) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {

    val byType = remember(documents) {
        documents.associateBy { it.type }
    }

    val completed = remember(approvedTypes) {
        DriverRequirements.required.count { it in approvedTypes }
    }

    val total = DriverRequirements.required.size

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SmartBusBackground)
    ) {

        Column(modifier = Modifier.padding(20.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {

                TextButton(onClick = onBack) {
                    Text("Volver")
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "$completed de $total",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (completed == total) SmartBusGreen else SmartBusBlue
                )
            }

            Text(
                text = "Documentos para conducir",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = SmartBusText
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "La plataforma revisa cada documento. Solo con todos " +
                    "aprobados y el SOAT vigente puedes ponerte en servicio.",
                style = MaterialTheme.typography.bodySmall,
                color = SmartBusTextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = {
                    if (total == 0) 0f else completed.toFloat() / total
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (completed == total) SmartBusGreen else SmartBusBlue,
                trackColor = SmartBusDivider
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                bottom = 30.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            DriverRequirements.steps.forEach { step ->

                item(key = "step-${step.title}") {
                    StepHeader(step)
                }

                items(step.items, key = { it.name }) { type ->

                    DocumentRow(
                        type = type,
                        document = byType[type],
                        onUpload = { onUpload(type) },
                        onDelete = { onDelete(type) }
                    )
                }
            }

            item(key = "recommended") {

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Recomendado",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SmartBusTextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                DriverRequirements.recommended.forEach { type ->

                    DocumentRow(
                        type = type,
                        document = byType[type],
                        onUpload = { onUpload(type) },
                        onDelete = { onDelete(type) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item(key = "why") {
                WhyItMattersCard()
            }
        }
    }
}

@Composable
private fun StepHeader(step: DriverStep) {

    Column(modifier = Modifier.padding(top = 8.dp)) {

        Text(
            text = step.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = SmartBusText
        )

        Text(
            text = step.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = SmartBusTextSecondary
        )

        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun DocumentRow(
    type: DriverDocumentType,
    document: DriverDocument?,
    onUpload: () -> Unit,
    onDelete: () -> Unit
) {

    val status = document?.status ?: DriverDocumentStatus.MISSING

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {

        Column(modifier = Modifier.padding(14.dp)) {

            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                StatusBadge(status)

                Column(modifier = Modifier.weight(1f)) {

                    Text(
                        text = type.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = SmartBusText
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = type.explanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = SmartBusTextSecondary
                    )

                    if (document != null && document.note.isNotBlank()) {

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Nota del revisor: ${document.note}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (status == DriverDocumentStatus.REJECTED) {
                                SmartBusRed
                            } else {
                                SmartBusTextSecondary
                            }
                        )
                    }

                    if (document != null && document.expired) {

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Vencido: hay que volverlo a subir.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SmartBusRed
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row {

                        if (document == null) {

                            TextButton(onClick = onUpload) {
                                Icon(
                                    Icons.Default.AddAPhoto,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Subir")
                            }

                        } else if (status == DriverDocumentStatus.REJECTED) {

                            TextButton(onClick = onUpload) {
                                Text("Volver a subir")
                            }

                            TextButton(onClick = onDelete) {
                                Text("Quitar")
                            }

                        } else {

                            Text(
                                text = status.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = statusColor(status)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: DriverDocumentStatus) {

    val (icon, tint) = when (status) {
        DriverDocumentStatus.APPROVED ->
            Icons.Default.Check to SmartBusGreen

        DriverDocumentStatus.REJECTED ->
            Icons.Default.Close to SmartBusRed

        DriverDocumentStatus.UPLOADED,
        DriverDocumentStatus.UNDER_REVIEW ->
            Icons.Default.HourglassEmpty to SmartBusYellow

        DriverDocumentStatus.MISSING ->
            Icons.Default.Info to SmartBusTextSecondary
    }

    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun WhyItMattersCard() {

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = SmartBusBlue.copy(alpha = 0.07f)
        )
    ) {

        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Icon(
                Icons.Default.Shield,
                contentDescription = null,
                tint = SmartBusBlue,
                modifier = Modifier.size(22.dp)
            )

            Column {

                Text(
                    text = "Por que pedimos todo esto",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SmartBusText
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Cada documento protege a los pasajeros y tambien " +
                        "a ti. Con la verificacion de identidad nadie puede " +
                        "conducir usando tu cuenta; con el SOAT y la " +
                        "inspeccion el vehiculo esta asegurado; con los " +
                        "antecedentes filtramos a quien no debe conducir; " +
                        "y con la selfie en vivo impedimos que se use una " +
                        "foto. Si un pasajero sufriera un accidente, la " +
                        "policia y la aseguradora necesitan ver exactamente " +
                        "esto.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SmartBusTextSecondary
                )
            }
        }
    }
}

private fun statusColor(status: DriverDocumentStatus) = when (status) {
    DriverDocumentStatus.APPROVED -> SmartBusGreen
    DriverDocumentStatus.REJECTED -> SmartBusRed
    DriverDocumentStatus.UPLOADED,
    DriverDocumentStatus.UNDER_REVIEW -> SmartBusYellow
    DriverDocumentStatus.MISSING -> SmartBusTextSecondary
}