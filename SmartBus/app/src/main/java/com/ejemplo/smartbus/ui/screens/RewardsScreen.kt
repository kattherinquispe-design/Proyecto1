package com.ejemplo.smartbus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ejemplo.smartbus.data.model.Reward
import com.ejemplo.smartbus.ui.components.SmartBusBottomBar
import com.ejemplo.smartbus.ui.components.SmartBusTab
import com.ejemplo.smartbus.ui.theme.*

@Composable
fun RewardsScreen(
    rewards: List<Reward>,
    points: Int,
    onRedeem: (Reward) -> Unit,
    onBack: () -> Unit,
    redeemedCode: String? = null,
    redeemedRewardIds: Set<Int> = emptySet(),
    onTabSelected: (SmartBusTab) -> Unit = {}
) {

    Scaffold(
        containerColor =
            SmartBusBackground,

        bottomBar = {
            SmartBusBottomBar(
                selectedTab =
                    SmartBusTab.REWARDS,
                onTabSelected = { tab -> onTabSelected(tab) }
            )
        }
    ) { paddingValues ->

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
        ) {

            Text(
                text = "Recompensas",
                style =
                    MaterialTheme.typography
                        .headlineMedium,
                color = SmartBusText
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text(
                text = "Puntos $points puntos disponibles",
                color = SmartBusBlue,
                style =
                    MaterialTheme.typography
                        .titleMedium
            )

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            LazyVerticalGrid(
                columns =
                    GridCells.Fixed(2),
                modifier =
                    Modifier.weight(1f),
                contentPadding =
                    PaddingValues(
                        bottom = 16.dp
                    ),
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp),
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                items(rewards) { reward ->
                    val alreadyRedeemed = reward.id in redeemedRewardIds

                    Card(
                        modifier =
                            Modifier.fillMaxWidth(),
                        shape =
                            RoundedCornerShape(16.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    Color.White
                            )
                    ) {

                        Column(
                            modifier =
                                Modifier.padding(10.dp)
                        ) {

                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(85.dp)
                                        .clip(
                                            RoundedCornerShape(
                                                12.dp
                                            )
                                        )
                                        .background(
                                            SmartBusBlue
                                                .copy(
                                                    alpha =
                                                        0.08f
                                                )
                                        ),
                                contentAlignment =
                                    androidx.compose
                                        .ui
                                        .Alignment
                                        .Center
                            ) {

                                Text(
                                    text = when {
                                        reward.name
                                            .contains(
                                                "comida",
                                                true
                                            ) -> "Bebida"

                                        reward.name
                                            .contains(
                                                "descuento",
                                                true
                                            ) -> "Premio"

                                        else -> "Puntos"
                                    },
                                    style =
                                        MaterialTheme
                                            .typography
                                            .displaySmall
                                )
                            }

                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )

                            Text(
                                text =
                                    reward.name,
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium
                            )

                            Text(
                                text =
                                    "Puntos ${reward.points} puntos",
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall,
                                color =
                                    SmartBusTextSecondary
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )

                            Button(
                                onClick = {
                                    onRedeem(
                                        reward
                                    )
                                },
                                enabled =
                                    reward.id !in redeemedRewardIds &&
                                        points >= reward.points &&
                                        reward.isAvailable &&
                                        reward.stock > 0,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(38.dp),
                                contentPadding =
                                    PaddingValues(0.dp),
                                shape =
                                    RoundedCornerShape(
                                        10.dp
                                    ),
                                colors =
                                    ButtonDefaults
                                        .buttonColors(
                                            containerColor =
                                                SmartBusBlue
                                        )
                            ) {

                                Text(
                                    text = if (alreadyRedeemed) "Canjeado" else "Canjear",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodySmall
                                )
                            }
                        }
                    }
                }
            }
            redeemedCode?.let { code ->
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SmartBusBlue.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Tu código de canje", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(code, style = MaterialTheme.typography.headlineSmall)
                        Text("Muéstrale este código o QR en cafetería para validar", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}