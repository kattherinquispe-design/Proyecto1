package com.ejemplo.smartbus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ejemplo.smartbus.ui.theme.*

data class AchievementItem(
    val id: String,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val isUnlocked: Boolean,
    val progressText: String
)

@Composable
fun AchievementsScreen(
    points: Int,
    boardingCount: Int,
    rewardsRedeemed: Int,
    onBack: () -> Unit
) {
    val achievements = listOf(
        AchievementItem(
            id = "first_trip",
            title = "Primer Recorrido",
            description = "Toma tu primer bus en un paradero oficial de Arequipa",
            iconEmoji = "🚌",
            isUnlocked = boardingCount >= 1,
            progressText = if (boardingCount >= 1) "¡Completado!" else "$boardingCount / 1 viaje"
        ),
        AchievementItem(
            id = "frequent_traveler",
            title = "Viajero Frecuente",
            description = "Realiza al menos 5 abordajes validados con GPS",
            iconEmoji = "⭐",
            isUnlocked = boardingCount >= 5,
            progressText = if (boardingCount >= 5) "¡Completado!" else "$boardingCount / 5 viajes"
        ),
        AchievementItem(
            id = "points_collector",
            title = "Acumulador de Puntos",
            description = "Alcanza 500 puntos en tu cuenta MistiGo",
            iconEmoji = "🏆",
            isUnlocked = points >= 500,
            progressText = if (points >= 500) "¡Completado!" else "$points / 500 pts"
        ),
        AchievementItem(
            id = "first_reward",
            title = "Primer Canje",
            description = "Canjea tu primera recompensa con código QR de 1 solo uso",
            iconEmoji = "🎁",
            isUnlocked = rewardsRedeemed >= 1,
            progressText = if (rewardsRedeemed >= 1) "¡Completado!" else "$rewardsRedeemed / 1 canje"
        ),
        AchievementItem(
            id = "master_misti",
            title = "Maestro de MistiGo",
            description = "Alcanza 1500 puntos y canjea 3 recompensas",
            iconEmoji = "👑",
            isUnlocked = points >= 1500 && rewardsRedeemed >= 3,
            progressText = if (points >= 1500 && rewardsRedeemed >= 3) "¡Completado!" else "$points pts, $rewardsRedeemed canjes"
        )
    )

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
                text = "Logros y Medallas",
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
            text = "Viaja en transporte público y desbloquea insignias exclusivas",
            fontFamily = SmartBusBody,
            fontSize = 12.sp,
            color = SmartBusTextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(achievements, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (item.isUnlocked) AppPalette.primarySoft else Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.iconEmoji,
                                fontSize = 22.sp
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.title,
                                    fontFamily = SmartBusDisplay,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = SmartBusText
                                )

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (item.isUnlocked) Color(0xFFECFDF5) else Color(0xFFF8FAFC)
                                ) {
                                    Text(
                                        text = item.progressText,
                                        fontFamily = SmartBusBody,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.isUnlocked) Color(0xFF047857) else Color(0xFF64748B),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            Text(
                                text = item.description,
                                fontFamily = SmartBusBody,
                                fontSize = 12.sp,
                                color = SmartBusTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
