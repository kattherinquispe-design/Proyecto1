package com.ejemplo.smartbus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ejemplo.smartbus.data.SessionManager
import com.ejemplo.smartbus.data.SmartBusSession
import com.ejemplo.smartbus.data.SmartBusStatistics
import com.ejemplo.smartbus.data.model.PointTransaction
import com.ejemplo.smartbus.ui.components.SmartBusBottomBar
import com.ejemplo.smartbus.ui.components.SmartBusTab
import com.ejemplo.smartbus.ui.theme.AppPalette
import com.ejemplo.smartbus.ui.theme.SmartBusBackground
import com.ejemplo.smartbus.ui.theme.SmartBusBlue
import com.ejemplo.smartbus.ui.theme.SmartBusBody
import com.ejemplo.smartbus.ui.theme.SmartBusDisplay
import com.ejemplo.smartbus.ui.theme.SmartBusDivider
import com.ejemplo.smartbus.ui.theme.SmartBusGradient
import com.ejemplo.smartbus.ui.theme.SmartBusGreen
import com.ejemplo.smartbus.ui.theme.SmartBusGreenLight
import com.ejemplo.smartbus.ui.theme.SmartBusRed
import com.ejemplo.smartbus.ui.theme.SmartBusRedLight
import com.ejemplo.smartbus.ui.theme.SmartBusText
import com.ejemplo.smartbus.ui.theme.SmartBusTextLight
import com.ejemplo.smartbus.ui.theme.SmartBusTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class HistoryFilter(val label: String) {
    ALL("Todos"),
    POINTS("Puntos"),
    REDEEMS("Canjes")
}

private fun initialsFromSession(session: SmartBusSession?): String {
    val name = session?.name?.trim().orEmpty()
    if (name.isBlank()) return "MG"
    val parts = name.split(" ").filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
        else -> parts.first().take(2).uppercase()
    }
}

@Composable
fun HistoryScreen(
    history: List<PointTransaction>,
    statistics: SmartBusStatistics,
    points: Int,
    currentTab: SmartBusTab,
    onTabChange: (SmartBusTab) -> Unit,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
    onHelpClick: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember(context) { SessionManager.get(context) }
    val session by sessionManager.session.collectAsState()

    var filter by remember { mutableStateOf(HistoryFilter.POINTS) }

    val filtered = remember(history, filter) {
        when (filter) {
            HistoryFilter.ALL -> history
            HistoryFilter.POINTS -> history.filter { it.points > 0 }
            HistoryFilter.REDEEMS -> history.filter { it.points < 0 }
        }
    }

    val hasAvailablePoints = points > 0
    val showEmptyState = when (filter) {
        HistoryFilter.POINTS -> !hasAvailablePoints && filtered.isEmpty()
        HistoryFilter.ALL -> !hasAvailablePoints && filtered.isEmpty()
        HistoryFilter.REDEEMS -> filtered.isEmpty()
    }

    Scaffold(
        containerColor = SmartBusBackground,
        bottomBar = {
            SmartBusBottomBar(
                selectedTab = currentTab,
                onTabSelected = onTabChange
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Mis Puntos y Ranking",
                        fontFamily = SmartBusDisplay,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = SmartBusText
                    )
                    Text(
                        text = if (hasAvailablePoints) {
                            "Tienes $points puntos disponibles para canjear"
                        } else if (history.isEmpty()) {
                            "Sin movimientos registrados"
                        } else {
                            "${history.size} movimientos registrados"
                        },
                        fontFamily = SmartBusBody,
                        fontSize = 12.sp,
                        color = SmartBusTextSecondary
                    )
                }

                Row {
                    IconButton(onClick = onHelpClick) {
                        Icon(
                            Icons.Default.HelpOutline,
                            contentDescription = "Ayuda"
                        )
                    }

                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Configuración"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            HistorySummaryCard(
                points = points,
                boardingCount = statistics.boardingCount,
                rewardsRedeemed = statistics.rewardsRedeemed
            )

            Spacer(modifier = Modifier.height(14.dp))

            HistoryFilterBar(
                selected = filter,
                onSelect = { filter = it }
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (showEmptyState) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    item {
                        EmptyHistoryCard(filter = filter)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Cuando el usuario tiene puntos disponibles y está en "Puntos" o "Todos",
                    // mostrar el Perfil de Puntos Disponibles con diseño atractivo.
                    if (hasAvailablePoints && (filter == HistoryFilter.POINTS || filter == HistoryFilter.ALL)) {
                        item {
                            AvailablePointsProfileCard(
                                session = session,
                                points = points,
                                boardingCount = statistics.boardingCount,
                                rewardsRedeemed = statistics.rewardsRedeemed,
                                onRedeemClick = { onTabChange(SmartBusTab.REWARDS) }
                            )
                        }
                    }

                    // Si no hay transacciones locales de abordaje pero sí tiene puntos en la nube,
                    // mostrar la tarjeta del saldo acreditado en su cuenta.
                    if (hasAvailablePoints && filtered.isEmpty() && (filter == HistoryFilter.POINTS || filter == HistoryFilter.ALL)) {
                        item {
                            AccountCreditBadgeCard(points = points, userName = session?.name)
                        }
                    }

                    if (filtered.isNotEmpty()) {
                        item {
                            Text(
                                text = when (filter) {
                                    HistoryFilter.POINTS -> "Historial de puntos ganados"
                                    HistoryFilter.REDEEMS -> "Historial de canjes realizados"
                                    HistoryFilter.ALL -> "Movimientos recientes"
                                },
                                fontFamily = SmartBusDisplay,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = SmartBusText,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            )
                        }

                        items(filtered) { transaction ->
                            HistoryRow(transaction)
                        }
                    }

                    // Abajo siempre mostrar la guía bonita de cómo ganar más puntos
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        HowToEarnMoreBeautyCard(
                            onGoToMap = { onTabChange(SmartBusTab.MAP) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta de Perfil de Puntos Disponibles que aparece cuando el usuario tiene puntos (> 0).
 */
@Composable
private fun AvailablePointsProfileCard(
    session: SmartBusSession?,
    points: Int,
    boardingCount: Int,
    rewardsRedeemed: Int,
    onRedeemClick: () -> Unit
) {
    val userName = session?.name?.takeIf { it.isNotBlank() } ?: "Pasajero MistiGo"
    val userEmail = session?.email?.takeIf { it.isNotBlank() } ?: "Cuenta activa verificada"
    val level = (points / 100) + 1
    val nextMilestone = ((points / 100) + 1) * 100
    val progress = ((points % 100).toFloat() / 100f).coerceIn(0.15f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Cabecera del perfil con degradado elegante
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                AppPalette.primaryDark,
                                AppPalette.primary
                            )
                        )
                    )
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.22f))
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initialsFromSession(session),
                            fontFamily = SmartBusDisplay,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = userName,
                                fontFamily = SmartBusDisplay,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verificado",
                                tint = Color(0xFFfde047),
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        Text(
                            text = userEmail,
                            fontFamily = SmartBusBody,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.88f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "⭐ Nivel $level • Viajero VIP MistiGo",
                                fontFamily = SmartBusBody,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Cuerpo con el saldo de puntos disponibles conectado en tiempo real
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SALDO DISPONIBLE EN TU CUENTA",
                            fontFamily = SmartBusBody,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SmartBusTextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "$points",
                                fontFamily = SmartBusDisplay,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 34.sp,
                                color = AppPalette.primary
                            )
                            Text(
                                text = "puntos activos",
                                fontFamily = SmartBusDisplay,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = SmartBusText,
                                modifier = Modifier.padding(bottom = 5.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFECFDF5),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Listos para",
                                fontSize = 10.sp,
                                color = Color(0xFF047857),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "CANJEAR",
                                fontSize = 11.sp,
                                color = Color(0xFF047857),
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = AppPalette.primary,
                    trackColor = AppPalette.primarySoft
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Ganas +20 pts por viaje en paradero",
                        fontFamily = SmartBusBody,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppPalette.primary
                    )
                    Text(
                        text = "Próxima meta: $nextMilestone pts",
                        fontFamily = SmartBusBody,
                        fontSize = 11.sp,
                        color = SmartBusTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3 indicadores rápidos del perfil de puntos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniPointStat(
                        label = "Disponibles",
                        value = "$points pts",
                        bgColor = AppPalette.primarySoft,
                        textColor = AppPalette.primary,
                        modifier = Modifier.weight(1f)
                    )
                    MiniPointStat(
                        label = "Por Viaje",
                        value = "+20 pts",
                        bgColor = Color(0xFFECFDF5),
                        textColor = Color(0xFF059669),
                        modifier = Modifier.weight(1f)
                    )
                    MiniPointStat(
                        label = "Canjes",
                        value = "$rewardsRedeemed",
                        bgColor = Color(0xFFF8FAFC),
                        textColor = Color(0xFF334155),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onRedeemClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppPalette.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Canjear mis $points puntos por premios",
                        fontFamily = SmartBusDisplay,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniPointStat(
    label: String,
    value: String,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontFamily = SmartBusDisplay,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            color = textColor
        )
        Text(
            text = label,
            fontFamily = SmartBusBody,
            fontSize = 11.sp,
            color = SmartBusTextSecondary
        )
    }
}

@Composable
private fun AccountCreditBadgeCard(
    points: Int,
    userName: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(SmartBusGreenLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SmartBusGreen,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Puntos acreditados en cuenta MistiGo",
                    fontFamily = SmartBusDisplay,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = SmartBusText
                )
                Text(
                    text = "Sincronizado con el perfil de ${userName?.takeIf { it.isNotBlank() } ?: "pasajero"}",
                    fontFamily = SmartBusBody,
                    fontSize = 12.sp,
                    color = SmartBusTextSecondary
                )
            }

            Text(
                text = "+$points pts",
                fontFamily = SmartBusDisplay,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = SmartBusGreen
            )
        }
    }
}

/**
 * Tarjeta atractiva en la parte inferior con indicaciones claras de cómo ganar más puntos.
 */
@Composable
private fun HowToEarnMoreBeautyCard(
    onGoToMap: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppPalette.primarySoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = AppPalette.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "¿Cómo ganar más puntos?",
                        fontFamily = SmartBusDisplay,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = SmartBusText
                    )
                    Text(
                        text = "Sigue estos pasos y suma +20 puntos en cada viaje",
                        fontFamily = SmartBusBody,
                        fontSize = 12.sp,
                        color = SmartBusTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            EarnGuideRow(
                icon = Icons.Default.LocationOn,
                badge = "+20 pts",
                title = "1. Acércate a un paradero oficial",
                subtitle = "Cuando estés en el radio del paradero recibirás una notificación con sonido."
            )

            Spacer(modifier = Modifier.height(10.dp))

            EarnGuideRow(
                icon = Icons.Default.DirectionsBus,
                badge = "Automático",
                title = "2. Aborda tu bus en ruta",
                subtitle = "El GPS detecta tu viaje en el bus y acredita +20 puntos al instante en tu cuenta."
            )

            Spacer(modifier = Modifier.height(10.dp))

            EarnGuideRow(
                icon = Icons.Default.NotificationsActive,
                badge = "En vivo",
                title = "3. Recibe aviso y acumula sin límites",
                subtitle = "Toca la notificación de puntos ganados para ver tu saldo actualizado."
            )

            Spacer(modifier = Modifier.height(10.dp))

            EarnGuideRow(
                icon = Icons.Default.QrCode2,
                badge = "QR Único",
                title = "4. Canjea premios cuantas veces quieras",
                subtitle = "Cada canje descuenta los puntos del premio y genera un código QR único de 1 solo uso."
            )

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                onClick = onGoToMap,
                shape = RoundedCornerShape(12.dp),
                color = AppPalette.primarySoft,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = AppPalette.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ver paraderos cercanos en el Mapa para ganar +20 pts",
                        fontFamily = SmartBusBody,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = AppPalette.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun EarnGuideRow(
    icon: ImageVector,
    badge: String,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF8FAFC))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(AppPalette.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppPalette.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontFamily = SmartBusDisplay,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = SmartBusText,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFECFDF5)
                ) {
                    Text(
                        text = badge,
                        fontFamily = SmartBusBody,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = Color(0xFF059669),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontFamily = SmartBusBody,
                fontSize = 11.sp,
                color = SmartBusTextSecondary,
                lineHeight = 15.sp
            )
        }
    }
}

/**
 * Los tres botones que filtran la lista: Todos, Puntos y Canjes.
 */
@Composable
private fun HistoryFilterBar(
    selected: HistoryFilter,
    onSelect: (HistoryFilter) -> Unit
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        HistoryFilter.entries.forEach { filter ->

            val isSelected = filter == selected

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSelected) {
                            SmartBusGradient
                        } else {
                            androidx.compose.ui.graphics.SolidColor(
                                Color.White
                            )
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) {
                            Color.Transparent
                        } else {
                            SmartBusDivider
                        },
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onSelect(filter) },
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = filter.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isSelected) {
                        Color.White
                    } else {
                        SmartBusTextSecondary
                    }
                )
            }
        }
    }
}

/**
 * Estado vacio (solo aparece cuando el usuario tiene 0 puntos y no tiene movimientos).
 */
@Composable
private fun EmptyHistoryCard(filter: HistoryFilter) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(SmartBusBlue.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = "🎁",
                fontSize = 46.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Aún no tienes puntos disponibles",
            style = MaterialTheme.typography.titleMedium,
            color = SmartBusText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = when (filter) {
                HistoryFilter.POINTS ->
                    "Cuando ganes puntos en tus viajes aparecerá aquí tu perfil de puntos."
                HistoryFilter.REDEEMS ->
                    "Cuando canjes una recompensa aparecerá aquí."
                HistoryFilter.ALL ->
                    "Sube a un bus en un paradero para empezar a ganar +20 puntos."
            },
            style = MaterialTheme.typography.bodySmall,
            color = SmartBusTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        HowToEarnCard()
    }
}

/**
 * Cuadrito con la forma de ganar puntos cuando no hay puntos aún.
 */
@Composable
private fun HowToEarnCard() {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {

        Column(modifier = Modifier.padding(16.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(SmartBusBlue.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = SmartBusBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "¿Cómo gano puntos?",
                    style = MaterialTheme.typography.titleSmall,
                    color = SmartBusText
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            EarnStep(text = "1. Aborda un bus en cualquier paradero oficial.")
            EarnStep(text = "2. El GPS confirma tu viaje automáticamente (+20 pts).")
            EarnStep(text = "3. Suma puntos y canjéalos por premios cuantas veces quieras.")
        }
    }
}

@Composable
private fun EarnStep(text: String) {

    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = SmartBusTextSecondary,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

@Composable
private fun HistoryRow(transaction: PointTransaction) {

    val isPositive = transaction.points > 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isPositive) {
                            SmartBusGreenLight
                        } else {
                            SmartBusRedLight
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = if (isPositive) {
                        Icons.Default.ArrowUpward
                    } else {
                        Icons.Default.ArrowDownward
                    },
                    contentDescription = null,
                    tint = if (isPositive) {
                        SmartBusGreen
                    } else {
                        SmartBusRed
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {

                Text(
                    text = transaction.reason,
                    style = MaterialTheme.typography.titleSmall
                )

                if (transaction.stopName.isNotBlank()) {

                    Text(
                        text = transaction.stopName,
                        style = MaterialTheme.typography.bodySmall,
                        color = SmartBusTextSecondary
                    )
                }

                Text(
                    text = SimpleDateFormat(
                        "dd/MM/yyyy HH:mm",
                        Locale.getDefault()
                    ).format(
                        Date(transaction.timestamp)
                    ),
                    style = MaterialTheme.typography
                        .bodySmall,
                    color = SmartBusTextLight
                )
            }

            Text(
                text = (if (isPositive) "+" else "") +
                    "${transaction.points}",
                style = MaterialTheme.typography.titleMedium,
                color = if (isPositive) {
                    SmartBusGreen
                } else {
                    SmartBusRed
                }
            )
        }
    }
}

/**
 * Resumen en tarjeta con degradado:
 * puntos disponibles, abordajes y canjes.
 */
@Composable
private fun HistorySummaryCard(
    points: Int,
    boardingCount: Int,
    rewardsRedeemed: Int
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SmartBusGradient)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = null,
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "$points puntos disponibles",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )

                    Text(
                        text = "$boardingCount abordajes · " +
                            "$rewardsRedeemed canjes",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}
