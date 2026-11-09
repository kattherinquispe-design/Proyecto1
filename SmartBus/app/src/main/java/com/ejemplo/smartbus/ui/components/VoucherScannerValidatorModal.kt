package com.ejemplo.smartbus.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ejemplo.smartbus.data.RedeemedReward
import com.ejemplo.smartbus.ui.theme.AppPalette
import com.ejemplo.smartbus.ui.theme.SmartBusBody
import com.ejemplo.smartbus.ui.theme.SmartBusDisplay
import com.ejemplo.smartbus.ui.theme.SmartBusText
import com.ejemplo.smartbus.ui.theme.SmartBusTextSecondary
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modal de Escáner y Verificación de Código Único de Canje.
 * Muestra a la persona que escanea:
 * 1. El código único arriba.
 * 2. Debajo del código, el nombre del premio por el que canjeó.
 * 3. Un botón de "Confirmado" para validar que fue usado una sola vez y bloquear ese código único.
 */
@Composable
fun VoucherScannerValidatorModal(
    initialCode: String = "",
    initialRewardName: String = "",
    initialPoints: Int = 0,
    recentVouchers: List<RedeemedReward> = emptyList(),
    onConfirmCodeUsed: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var scannedCode by remember { mutableStateOf(initialCode.trim()) }
    var rewardName by remember { mutableStateOf(initialRewardName.trim()) }
    var pointsCost by remember { mutableIntStateOf(initialPoints) }
    var isUsed by remember { mutableStateOf(false) }
    var usedAtMillis by remember { mutableStateOf<Long?>(null) }
    var loading by remember { mutableStateOf(false) }
    var manualInput by remember { mutableStateOf("") }

    // Sincronizar en tiempo real con Firebase Firestore ('canjes/{code}')
    DisposableEffect(scannedCode) {
        val clean = scannedCode.trim()
        if (clean.isBlank()) {
            return@DisposableEffect onDispose { }
        }
        loading = true
        val localMatch = recentVouchers.firstOrNull { it.code.equals(clean, ignoreCase = true) }
        if (localMatch != null) {
            if (rewardName.isBlank()) rewardName = localMatch.name
            if (pointsCost == 0) pointsCost = localMatch.points
            if (localMatch.isUsed) {
                isUsed = true
                usedAtMillis = localMatch.usedAt
            }
        }

        val registration = try {
            FirebaseFirestore.getInstance()
                .collection("canjes")
                .document(clean)
                .addSnapshotListener { snap, _ ->
                    loading = false
                    if (snap != null && snap.exists()) {
                        val cloudName = snap.getString("rewardName")
                        val cloudPts = snap.getLong("pointsCost")?.toInt()
                        val cloudStatus = snap.getString("status")
                        val cloudUsedAt = snap.getLong("usedAt")
                        if (!cloudName.isNullOrBlank()) rewardName = cloudName
                        if (cloudPts != null && cloudPts > 0) pointsCost = cloudPts
                        if (cloudStatus == "USADO") {
                            isUsed = true
                            usedAtMillis = cloudUsedAt
                        }
                    } else if (rewardName.isBlank()) {
                        rewardName = "Premio MistiGo Canjeado"
                    }
                }
        } catch (_: Exception) {
            loading = false
            null
        }

        onDispose {
            registration?.remove()
        }
    }

    fun launchCameraScanner() {
        try {
            val scanner = GmsBarcodeScanning.getClient(context)
            scanner.startScan()
                .addOnSuccessListener { barcode ->
                    val raw = barcode.rawValue.orEmpty()
                    val parsed = QrCodeGenerator.parseVoucherPayload(raw)
                    scannedCode = parsed.code
                    rewardName = parsed.rewardName.orEmpty()
                    pointsCost = parsed.points ?: 0
                    isUsed = false
                    usedAtMillis = null
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        context,
                        "Selecciona o ingresa el código único abajo (${e.localizedMessage ?: "Cámara"})",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } catch (_: Exception) {
            Toast.makeText(
                context,
                "Selecciona un código abajo para verificar",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = AppPalette.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Escáner de Canje",
                            fontFamily = SmartBusDisplay,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = SmartBusText
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Botón para abrir la cámara lectora de QR
                OutlinedButton(
                    onClick = { launchCameraScanner() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Escanear QR con Cámara",
                        fontFamily = SmartBusBody,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                if (scannedCode.isBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = manualInput,
                        onValueChange = { manualInput = it },
                        label = { Text("O escribe / pega el código único") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (manualInput.isNotBlank()) {
                                val parsed = QrCodeGenerator.parseVoucherPayload(manualInput)
                                scannedCode = parsed.code
                                rewardName = parsed.rewardName.orEmpty()
                                pointsCost = parsed.points ?: 0
                            }
                        },
                        enabled = manualInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppPalette.primary)
                    ) {
                        Text("Verificar Código Único", fontWeight = FontWeight.Bold)
                    }

                    if (recentVouchers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Últimos códigos generados para probar escaneo:",
                            fontFamily = SmartBusBody,
                            fontSize = 11.sp,
                            color = SmartBusTextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        recentVouchers.take(3).forEach { v ->
                            Surface(
                                onClick = {
                                    scannedCode = v.code
                                    rewardName = v.name
                                    pointsCost = v.points
                                    isUsed = v.isUsed
                                    usedAtMillis = v.usedAt
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Text(
                                        text = v.code,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = SmartBusText
                                    )
                                    Text(
                                        text = "${v.name} (${if (v.isUsed) "USADO" else "DISPONIBLE"})",
                                        fontSize = 11.sp,
                                        color = if (v.isUsed) Color(0xFFDC2626) else Color(0xFF059669)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(14.dp))

                    // =======================================================
                    // 1. ARRIBA: EL CÓDIGO ÚNICO ESCANEADO
                    // =======================================================
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.5.dp, Color(0xFFCBD5E1), RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "CÓDIGO ÚNICO DE CANJE",
                            fontFamily = SmartBusBody,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = scannedCode,
                            fontFamily = SmartBusDisplay,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // =======================================================
                    // 2. ABAJO DEL CÓDIGO: EL NOMBRE DEL PREMIO POR EL QUE CANJEÓ
                    // =======================================================
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppPalette.primarySoft)
                            .border(1.dp, AppPalette.primary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "PREMIO POR EL QUE CANJEÓ",
                            fontFamily = SmartBusBody,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppPalette.primaryDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = rewardName.ifBlank { "Premio MistiGo" },
                            fontFamily = SmartBusDisplay,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AppPalette.primary,
                            textAlign = TextAlign.Center
                        )
                        if (pointsCost > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Puntos descontados: $pointsCost pts",
                                fontFamily = SmartBusBody,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // =======================================================
                    // 3. BOTÓN DE "CONFIRMADO" PARA VALIDAR QUE FUE USADO 1 SOLA VEZ
                    // =======================================================
                    if (!isUsed) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFECFDF5),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "● CÓDIGO VÁLIDO — DISPONIBLE (1 SOLO USO)",
                                color = Color(0xFF047857),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val now = System.currentTimeMillis()
                                isUsed = true
                                usedAtMillis = now
                                onConfirmCodeUsed(scannedCode)
                                try {
                                    FirebaseFirestore.getInstance()
                                        .collection("canjes")
                                        .document(scannedCode)
                                        .set(
                                            mapOf(
                                                "code" to scannedCode,
                                                "rewardName" to rewardName.ifBlank { "Premio MistiGo" },
                                                "pointsCost" to pointsCost,
                                                "status" to "USADO",
                                                "usedAt" to now
                                            ),
                                            SetOptions.merge()
                                        )
                                } catch (_: Exception) {
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Confirmado (Validar 1 solo uso)",
                                fontFamily = SmartBusDisplay,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFEF2F2),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEF4444))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "CÓDIGO ÚNICO YA USADO Y BLOQUEADO",
                                        color = Color(0xFFDC2626),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp
                                    )
                                }
                                val timeStr = usedAtMillis?.let {
                                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(it))
                                } ?: "Confirmado"
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Validado el $timeStr • No puede volver a usarse",
                                    color = Color(0xFFB91C1C),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
