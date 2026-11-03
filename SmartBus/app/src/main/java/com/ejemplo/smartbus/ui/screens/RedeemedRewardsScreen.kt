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
import com.ejemplo.smartbus.ui.theme.*
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

    val currentList = remember(redeemed, refreshTrigger) {
        pointsRepository?.getRedeemedRewards() ?: redeemed
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
                fontSize = 22.sp,
                color = SmartBusText
            )

            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = SmartBusText
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (currentList.isEmpty()) {
                "Sin canjes registrados"
            } else {
                "${currentList.size} canje(s) realizados • Toca un canje para ver su QR"
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
                    text = "Canjea una recompensa desde la pestaña Premios.",
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

                                        // Badge de Estado de 1 solo uso
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (reward.isUsed) Color(0xFFFEF2F2) else Color(0xFFECFDF5)
                                        ) {
                                            Text(
                                                text = if (reward.isUsed) "YA USADO" else "DISPONIBLE",
                                                color = if (reward.isUsed) Color(0xFFDC2626) else Color(0xFF059669),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = "Canjeado por ${reward.points} puntos",
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
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF334155),
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

    // MODAL DEL CÓDIGO QR PARA VALIDAR EN CAJA
    selectedRewardForQr?.let { reward ->
        val qrBitmap = remember(reward.code) { QrCodeGenerator.generate(reward.code, 480) }

        Dialog(onDismissRequest = { selectedRewardForQr = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                            text = "Voucher de Canje",
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

                    Text(
                        text = reward.name,
                        fontFamily = SmartBusDisplay,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppPalette.primary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // CONTENEDOR DEL CÓDIGO QR CON SELLO
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (reward.isUsed) Color(0xFFF1F5F9) else Color.White)
                            .border(
                                width = 2.dp,
                                color = if (reward.isUsed) Color(0xFFEF4444) else AppPalette.primarySoft,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(12.dp),
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
                                        text = "¡YA FUE USADO!",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = reward.code,
                        fontFamily = SmartBusBody,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!reward.isUsed) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFECFDF5),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "● ACTIVO PARA CANJE EN CAJA / BUS",
                                color = Color(0xFF047857),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                pointsRepository?.markRedemptionAsUsed(reward.code)
                                refreshTrigger++
                                selectedRewardForQr = selectedRewardForQr?.copy(isUsed = true, usedAt = System.currentTimeMillis())
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                        ) {
                            Text(
                                text = "Confirmar uso en caja (1 solo uso)",
                                fontFamily = SmartBusBody,
                                fontSize = 12.sp,
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
                                text = "Consumido el $timeStr • No reutilizable",
                                color = Color(0xFFB91C1C),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
