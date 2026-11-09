package com.ejemplo.smartbus.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.ejemplo.smartbus.data.RedeemedReward
import com.ejemplo.smartbus.data.model.Reward
import com.ejemplo.smartbus.ui.components.QrCodeGenerator
import com.ejemplo.smartbus.ui.components.SmartBusBottomBar
import com.ejemplo.smartbus.ui.components.SmartBusTab
import com.ejemplo.smartbus.ui.components.VoucherScannerValidatorModal
import com.ejemplo.smartbus.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RewardsScreen(
    rewards: List<Reward>,
    points: Int,
    onRedeem: (Reward) -> Unit,
    onBack: () -> Unit,
    redeemedCode: String? = null,
    redeemedRewardIds: Set<Int> = emptySet(),
    recentVouchers: List<RedeemedReward> = emptyList(),
    onTabSelected: (SmartBusTab) -> Unit = {},
    onMarkAsUsed: ((String) -> Boolean)? = null
) {
    val clipboard = LocalClipboardManager.current
    var selectedRewardForRedeem by remember { mutableStateOf<Reward?>(null) }
    var lastClickedReward by remember { mutableStateOf<Reward?>(null) }
    var activeVoucherReward by remember { mutableStateOf<Pair<Reward, String>?>(null) }
    var voucherIsUsed by remember { mutableStateOf(false) }
    var voucherUsedAt by remember { mutableStateOf<Long?>(null) }
    var showScannerModal by remember { mutableStateOf(false) }

    // Si viene un código único recién canjeado, abrir automáticamente su QR
    LaunchedEffect(redeemedCode) {
        if (!redeemedCode.isNullOrBlank()) {
            val idFromCode = redeemedCode.split("-").getOrNull(1)?.toIntOrNull()
                ?: redeemedCode.split("-").getOrNull(2)?.toIntOrNull()
            val matched = lastClickedReward
                ?: rewards.firstOrNull { it.id == idFromCode }
                ?: rewards.firstOrNull()
            if (matched != null) {
                activeVoucherReward = Pair(matched, redeemedCode)
                voucherIsUsed = false
                voucherUsedAt = null
            }
        }
    }

    // Escuchar en tiempo real en Firebase Firestore si el código único activo fue confirmado por el escáner
    val activeCode = activeVoucherReward?.second
    DisposableEffect(activeCode) {
        if (activeCode.isNullOrBlank()) {
            return@DisposableEffect onDispose { }
        }
        val listener = try {
            FirebaseFirestore.getInstance()
                .collection("canjes")
                .document(activeCode)
                .addSnapshotListener { snap, _ ->
                    if (snap != null && snap.exists() && snap.getString("status") == "USADO") {
                        voucherIsUsed = true
                        voucherUsedAt = snap.getLong("usedAt") ?: System.currentTimeMillis()
                        onMarkAsUsed?.invoke(activeCode)
                    }
                }
        } catch (_: Exception) {
            null
        }
        onDispose {
            listener?.remove()
        }
    }

    Scaffold(
        containerColor = SmartBusBackground,
        bottomBar = {
            SmartBusBottomBar(
                selectedTab = SmartBusTab.REWARDS,
                onTabSelected = { tab -> onTabSelected(tab) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // =======================================================
            // 1. ENCABEZADO, ESCÁNER Y BALANCE DE PUNTOS DINÁMICO
            // =======================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Recompensas",
                        fontFamily = SmartBusDisplay,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = SmartBusText
                    )
                    Text(
                        text = "Canjea tus puntos cuantas veces quieras",
                        fontFamily = SmartBusBody,
                        fontSize = 12.sp,
                        color = SmartBusTextSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = { showScannerModal = true },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F172A)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Escanear QR",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Escáner",
                                fontFamily = SmartBusBody,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = AppPalette.primarySoft,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppPalette.primary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = AppPalette.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "$points pts",
                                fontFamily = SmartBusDisplay,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = AppPalette.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =======================================================
            // 2. GRILLA DE RECOMPENSAS (SIEMPRE DISPONIBLES MIENTRAS HAYA PUNTOS)
            // =======================================================
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(rewards, key = { it.id }) { reward ->
                    val canAfford = points >= reward.points && reward.isAvailable && reward.stock > 0

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(115.dp)
                                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                                    .background(Color(0xFFE2E8F0))
                            ) {
                                if (reward.imageUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = reward.imageUrl,
                                        contentDescription = reward.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(AppPalette.primarySoft, AppPalette.primary.copy(alpha = 0.2f))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = AppPalette.primary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.65f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "${reward.points} pts",
                                        fontFamily = SmartBusDisplay,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = reward.name,
                                    fontFamily = SmartBusDisplay,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF1E293B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = reward.description,
                                    fontFamily = SmartBusBody,
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 14.sp,
                                    modifier = Modifier.height(28.dp)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // BOTÓN DE CANJE SIEMPRE HABILITADO MIENTRAS TENGA PUNTOS
                                Button(
                                    onClick = {
                                        if (canAfford) {
                                            selectedRewardForRedeem = reward
                                        }
                                    },
                                    enabled = canAfford,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AppPalette.primary,
                                        disabledContainerColor = Color(0xFFF1F5F9)
                                    )
                                ) {
                                    Text(
                                        text = if (canAfford) {
                                            "Canjear"
                                        } else {
                                            "Faltan ${(reward.points - points).coerceAtLeast(0)} pts"
                                        },
                                        fontFamily = SmartBusBody,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (canAfford) Color.White else Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // =======================================================
    // 3. DIÁLOGO DE CONFIRMACIÓN ANTES DE DESCONTAR PUNTOS
    // =======================================================
    selectedRewardForRedeem?.let { reward ->
        AlertDialog(
            onDismissRequest = { selectedRewardForRedeem = null },
            title = {
                Text(
                    text = "Confirmar canje",
                    fontFamily = SmartBusDisplay,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "¿Deseas canjear «${reward.name}» por ${reward.points} puntos?",
                        fontFamily = SmartBusBody,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "🔒 Se descontarán ${reward.points} puntos de tu saldo y se generará un código QR único de un solo uso.",
                        fontFamily = SmartBusBody,
                        fontSize = 12.sp,
                        color = SmartBusTextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toRedeem = reward
                        lastClickedReward = toRedeem
                        selectedRewardForRedeem = null
                        onRedeem(toRedeem)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppPalette.primary)
                ) {
                    Text("Sí, canjear", fontFamily = SmartBusBody, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedRewardForRedeem = null }) {
                    Text("Cancelar", fontFamily = SmartBusBody)
                }
            }
        )
    }

    // =======================================================
    // 4. MODAL DEL VOUCHER QR DE SEGURIDAD (CÓDIGO ÚNICO + NOMBRE ABAJO + BOTÓN CONFIRMADO)
    // =======================================================
    activeVoucherReward?.let { (reward, code) ->
        val qrPayload = remember(code, reward.name, reward.points) {
            QrCodeGenerator.buildVoucherPayload(code, reward.name, reward.points)
        }
        val qrBitmap = remember(qrPayload) { QrCodeGenerator.generate(qrPayload, 512) }

        Dialog(onDismissRequest = { activeVoucherReward = null }) {
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
                            onClick = { activeVoucherReward = null },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // CONTENEDOR DEL CÓDIGO QR
                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (voucherIsUsed) Color(0xFFF1F5F9) else Color.White)
                            .border(
                                width = 2.dp,
                                color = if (voucherIsUsed) Color(0xFFEF4444) else AppPalette.primarySoft,
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
                        } else {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = null,
                                modifier = Modifier.size(100.dp),
                                tint = Color.Gray
                            )
                        }

                        if (voucherIsUsed) {
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                clipboard.setText(AnnotatedString(code))
                            }
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = code,
                                    fontFamily = SmartBusDisplay,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0F172A)
                                )
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copiar",
                                    tint = AppPalette.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
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

                    // 3. BOTÓN DE CONFIRMADO PARA VALIDAR QUE FUE USADO 1 SOLA VEZ
                    if (!voucherIsUsed) {
                        Button(
                            onClick = {
                                voucherIsUsed = true
                                voucherUsedAt = System.currentTimeMillis()
                                onMarkAsUsed?.invoke(code)
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
                            val timeStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                .format(Date(voucherUsedAt ?: System.currentTimeMillis()))
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

    // =======================================================
    // 5. MODAL DEL ESCÁNER DE CÓDIGOS ÚNICOS
    // =======================================================
    if (showScannerModal) {
        VoucherScannerValidatorModal(
            initialCode = activeVoucherReward?.second.orEmpty(),
            initialRewardName = activeVoucherReward?.first?.name.orEmpty(),
            initialPoints = activeVoucherReward?.first?.points ?: 0,
            recentVouchers = recentVouchers,
            onConfirmCodeUsed = { confirmedCode ->
                onMarkAsUsed?.invoke(confirmedCode)
                if (activeVoucherReward?.second == confirmedCode) {
                    voucherIsUsed = true
                    voucherUsedAt = System.currentTimeMillis()
                }
            },
            onDismiss = { showScannerModal = false }
        )
    }
}
