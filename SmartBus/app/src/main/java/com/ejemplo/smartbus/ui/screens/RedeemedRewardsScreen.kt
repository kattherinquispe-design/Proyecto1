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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.ejemplo.smartbus.data.RedeemedReward
import com.ejemplo.smartbus.ui.theme.SmartBusBlue
import com.ejemplo.smartbus.ui.theme.SmartBusGreen
import com.ejemplo.smartbus.ui.theme.SmartBusGreenLight
import com.ejemplo.smartbus.ui.theme.SmartBusText
import com.ejemplo.smartbus.ui.theme.SmartBusTextLight
import com.ejemplo.smartbus.ui.theme.SmartBusTextSecondary

@Composable
fun RedeemedRewardsScreen(
    redeemed: List<RedeemedReward>,
    onBack: () -> Unit
) {

    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "Recompensas canjeadas",
                style = MaterialTheme.typography.headlineMedium
            )

            IconButton(onClick = onBack) {

                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver"
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (redeemed.isEmpty()) {
                "Sin canjes registrados"
            } else {
                "${redeemed.size} canjes realizados"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = SmartBusTextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (redeemed.isEmpty()) {

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
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Canjea una recompensa desde la pestaña Premios.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SmartBusTextSecondary
                )
            }

        } else {

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                items(redeemed) { reward ->

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

                        Column(modifier = Modifier.padding(15.dp)) {

                            Row(verticalAlignment =
                                Alignment.CenterVertically) {

                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(
                                            SmartBusGreenLight,
                                            RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {

                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = SmartBusGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {

                                    Text(
                                        text = reward.name,
                                        style = MaterialTheme.typography
                                            .titleSmall,
                                        color = SmartBusText
                                    )

                                    Text(
                                        text = "Canjeado por " +
                                            "${reward.points} puntos",
                                        style = MaterialTheme.typography
                                            .bodySmall,
                                        color = SmartBusTextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Color(0xFFF5F7FC),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(
                                        horizontal = 12.dp,
                                        vertical = 8.dp
                                    ),
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Text(
                                    text = reward.code,
                                    style = MaterialTheme.typography
                                        .bodyMedium,
                                    color = SmartBusBlue,
                                    modifier = Modifier.weight(1f)
                                )

                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription =
                                        "Copiar codigo",
                                    tint = SmartBusTextSecondary,
                                    modifier = Modifier
                                        .size(17.dp)
                                        .clickable {
                                            clipboard.setText(
                                                AnnotatedString(reward.code)
                                            )
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AchievementsScreen(
    points: Int,
    boardingCount: Int,
    rewardsRedeemed: Int,
    onBack: () -> Unit
) {

    val level = (points / 1000) + 1
    val nextLevelAt = level * 1000

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "Mis logros",
                style = MaterialTheme.typography.headlineMedium
            )

            IconButton(onClick = onBack) {

                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            item {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = SmartBusBlue
                    )
                ) {

                    Column(modifier = Modifier.padding(18.dp)) {

                        Text(
                            text = "Nivel $level",
                            style = MaterialTheme.typography
                                .headlineMedium,
                            color = Color.White
                        )

                        Text(
                            text = "$points de $nextLevelAt puntos",
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            item {

                AchievementRow(
                    title = "Primer abordaje",
                    description = "Registraste tu primer viaje",
                    unlocked = boardingCount >= 1
                )
            }

            item {

                AchievementRow(
                    title = "Viajero frecuente",
                    description = "Acumulaste 10 abordajes",
                    unlocked = boardingCount >= 10
                )
            }

            item {

                AchievementRow(
                    title = "Explorador",
                    description = "Completaste 50 abordajes",
                    unlocked = boardingCount >= 50
                )
            }

            item {

                AchievementRow(
                    title = "Primer canje",
                    description = "Canjearte tu primera recompensa",
                    unlocked = rewardsRedeemed >= 1
                )
            }

            item {

                AchievementRow(
                    title = "Nivel 5",
                    description = "Alcanzaste 4000 puntos",
                    unlocked = level >= 5
                )
            }
        }
    }
}

@Composable
private fun AchievementRow(
    title: String,
    description: String,
    unlocked: Boolean
) {

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
                    .size(38.dp)
                    .background(
                        if (unlocked) {
                            SmartBusGreenLight
                        } else {
                            Color(0xFFF1F3F8)
                        },
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = if (unlocked) {
                        Icons.Default.CheckCircle
                    } else {
                        Icons.Default.CardGiftcard
                    },
                    contentDescription = null,
                    tint = if (unlocked) {
                        SmartBusGreen
                    } else {
                        SmartBusTextLight
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (unlocked) {
                        SmartBusText
                    } else {
                        SmartBusTextLight
                    }
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = SmartBusTextSecondary
                )
            }

            Text(
                text = if (unlocked) "Logro" else "Bloqueado",
                style = MaterialTheme.typography.labelSmall,
                color = if (unlocked) {
                    SmartBusGreen
                } else {
                    SmartBusTextLight
                }
            )
        }
    }
}