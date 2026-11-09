package com.ejemplo.smartbus.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ejemplo.smartbus.data.PointsRepository
import com.ejemplo.smartbus.data.RedeemedReward
import com.ejemplo.smartbus.ui.components.QrCodeGenerator
import com.ejemplo.smartbus.ui.components.VoucherScannerValidatorModal
import com.ejemplo.smartbus.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RedeemedRewardsScreen(
    redeemed: List<RedeemedReward>,
    onBack: () -> Unit,
    pointsRepository: PointsRepository? = null
) {
    val clipboard = LocalClipboardManager.current
    var selectedRewardForQr by remember { mutableStateOf<RedeemedReward?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var showScannerModal by remember { mutableStateOf(false) }

    val currentList = remember(redeemed, refreshTrigger) {
        pointsRepository?.getRedeemedRewards() ?: redeemed
    }

    // Escuchar en tiempo real en Firestore si el código seleccionado fue confirmado desde un escáner
    val selectedCode = selectedRewardForQr?.code
    DisposableEffect(selectedCode) {
        if (selectedCode.isNullOrBlank()) {
            return@DisposableEffect onDispose { }
        }
        val listener = try {
            FirebaseFirestore.getInstance()
                .collection("canjes")
                .document(selectedCode)
                .addSnapshotListener { snap, _ ->
                    if (snap != null && snap.exists() && snap.getString("status") == "USADO") {
                        val usedTime = snap.getLong("usedAt") ?: System.currentTimeMillis()
                        pointsRepository?.markRedemptionAsUsed(selectedCode)
                        refreshTrigger++
                        selectedRewardForQr = selectedRewardForQr?.copy(isUsed = true, usedAt = usedTime)
                    }
                }
        } catch (_: Exception) {
            null
        }
        onDispose {
            listener?.remove()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SmartBusBackground)
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recompensas canjeadas",
                fontFamily = SmartBusDisplay,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = SmartBusText,
                modifier = Modifier.weight(1f)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    onClick = { showScannerModal = true },
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F172A)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Escáner",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Escáner",
                            fontFamily = SmartBusBody,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }

                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = SmartBusText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (currentList.isEmpty()) {
                "Sin códigos únicos generados todavía"
            } else {
                "${currentList.size} código(s) único(s) • Toca cualquiera para mostrar su QR"
            },
            fontFamily = SmartBusBody,
            fontSize = 12.sp,
            color = SmartBusTextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (currentList.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CardGiftcard,
                    contentDescription = null,
                    tint = SmartBusTextLight,
                    modifier = Modifier.size(52.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Todavía no has canjeado recompensas.",
                    fontFamily = SmartBusDisplay,
                    fontSize = 16.sp,
                    color = SmartBusText
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Canjea cualquier premio desde la pestaña Premios.",
                    fontFamily = SmartBusBody,
                    fontSize = 13.sp,
                    color = SmartBusTextSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(currentList, key = { it.code }) { reward ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedRewardForQr = reward },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(15.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (reward.isUsed) Color(0xFFFEF2F2) else Color(0xFFECFDF5)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (reward.isUsed) Icons.Default.CheckCircle else Icons.Default.QrCode2,
                                        contentDescription = null,
                                        tint = if (reward.isUsed) Color(0xFFEF4444) else Color(0xFF10B981),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = reward.name,
                                            fontFamily = SmartBusDisplay,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = SmartBusText
                                        )

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (reward.isUsed) Color(0xFFFEF2F2) else Color(0xFFECFDF5)
                                        ) {
                                            Text(
                                                text = if (reward.isUsed) "CÓDIGO USADO" else "DISPONIBLE",
                                                color = if (reward.isUsed) Color(0xFFDC2626) else Color(0xFF059669),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = "Descontado: ${reward.points} puntos",
                                        fontFamily = SmartBusBody,
                                        fontSize = 12.sp,
                                        color = SmartBusTextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF8FAFC))
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = reward.code,
                                    fontFamily = SmartBusBody,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.weight(1f)
                                )

                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copiar código",
                                    tint = AppPalette.primary,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable {
                                            clipboard.setText(AnnotatedString(reward.code))
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // MODAL DEL CÓDIGO QR ÚNICO
    selectedRewardForQr?.let { reward ->
        val qrPayload = remember(reward.code, reward.name, reward.points) {
            QrCodeGenerator.buildVoucherPayload(reward.code, reward.name, reward.points)
        }
        val qrBitmap = remember(qrPayload) { QrCodeGenerator.generate(qrPayload, 512) }

        Dialog(onDismissRequest = { selectedRewardForQr = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                        Text(
                            text = "Código Único de Canje",
                            fontFamily = SmartBusDisplay,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = SmartBusText
                        )
                        IconButton(
                            onClick = { selectedRewardForQr = null },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (reward.isUsed) Color(0xFFF1F5F9) else Color.White)
                            .border(
                                width = 2.dp,
                                color = if (reward.isUsed) Color(0xFFEF4444) else AppPalette.primarySoft,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap,
                                contentDescription = "QR de canje",
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        if (reward.isUsed) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White.copy(alpha = 0.88f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFEF4444),
                                    shadowElevation = 4.dp
                                ) {
                                    Text(
                                        text = "¡CÓDIGO YA USADO!",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 1. ARRIBA: EL CÓDIGO ÚNICO
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "CÓDIGO ÚNICO",
                                fontFamily = SmartBusBody,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = reward.code,
                                fontFamily = SmartBusDisplay,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. DEBAJO DEL CÓDIGO: EL NOMBRE DEL PREMIO POR EL QUE CANJEÓ
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AppPalette.primarySoft,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppPalette.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "CANJEADO POR EL PREMIO",
                                fontFamily = SmartBusBody,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppPalette.primaryDark
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = reward.name,
                                fontFamily = SmartBusDisplay,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AppPalette.primary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "-${reward.points} puntos descontados",
                                fontFamily = SmartBusBody,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. BOTÓN DE CONFIRMADO PARA VALIDAR 1 SOLO USO
                    if (!reward.isUsed) {
                        Button(
                            onClick = {
                                val now = System.currentTimeMillis()
                                pointsRepository?.markRedemptionAsUsed(reward.code)
                                refreshTrigger++
                                selectedRewardForQr = selectedRewardForQr?.copy(isUsed = true, usedAt = now)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Confirmado (Validar 1 solo uso)",
                                fontFamily = SmartBusBody,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFEF2F2),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
                        ) {
                            val timeStr = if (reward.usedAt != null) {
                                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(reward.usedAt))
                            } else {
                                "Previamente consumido"
                            }
                            Text(
                                text = "🔒 Confirmado y bloqueado el $timeStr • 1 solo uso",
                                color = Color(0xFFB91C1C),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    if (showScannerModal) {
        VoucherScannerValidatorModal(
            recentVouchers = currentList,
            onConfirmCodeUsed = { confirmedCode ->
                pointsRepository?.markRedemptionAsUsed(confirmedCode)
                refreshTrigger++
            },
            onDismiss = { showScannerModal = false }
        )
    }
}
