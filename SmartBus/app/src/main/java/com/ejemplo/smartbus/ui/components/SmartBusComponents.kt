package com.ejemplo.smartbus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ejemplo.smartbus.ui.theme.*

@Composable
fun SmartBusSectionTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        modifier = modifier,
        style = MaterialTheme.typography.titleLarge,
        color = SmartBusText
    )
}

@Composable
fun SmartBusBlueButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SmartBusBlue,
            contentColor = Color.White
        )
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SmartBusPointsCard(
    points: Int,
    nextRewardPoints: Int = 1500,
    modifier: Modifier = Modifier
) {

    val progress =
        (points.toFloat() /
                nextRewardPoints.coerceAtLeast(1))
            .coerceIn(0f, 1f)

    val remaining =
        (nextRewardPoints - points)
            .coerceAtLeast(0)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = SmartBusBlue
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            SmartBusYellow
                        ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Star,
                        contentDescription = null,
                        tint = SmartBusBlueDark
                    )
                }

                Spacer(
                    modifier =
                        Modifier.width(12.dp)
                )

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text = "$points puntos",
                        style =
                            MaterialTheme.typography
                                .headlineMedium,
                        color = Color.White
                    )

                    Text(
                        text =
                            "Te faltan $remaining puntos para tu próxima recompensa",
                        style =
                            MaterialTheme.typography
                                .bodySmall,
                        color =
                            Color.White.copy(
                                alpha = 0.9f
                            )
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )

            LinearProgressIndicator(
                progress = { progress },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(
                            RoundedCornerShape(10.dp)
                        ),
                color = Color.White,
                trackColor =
                    Color.White.copy(
                        alpha = 0.25f
                    )
            )
        }
    }
}

@Composable
fun SmartBusStopCard(
    name: String,
    distance: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        SmartBusBlue.copy(
                            alpha = 0.10f
                        )
                    ),
                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = SmartBusBlue
                )
            }

            Spacer(
                modifier =
                    Modifier.width(12.dp)
            )

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text = name,
                    fontWeight =
                        FontWeight.SemiBold,
                    color = SmartBusText
                )

                Text(
                    text = distance,
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                    color =
                        SmartBusTextSecondary
                )
            }

            Icon(
                imageVector =
                    Icons.Default.ArrowForwardIos,
                contentDescription = null,
                modifier =
                    Modifier.size(16.dp),
                tint =
                    SmartBusTextLight
            )
        }
    }
}

@Composable
fun SmartBusBusCard(
    routeName: String,
    destination: String,
    eta: String,
    status: String = "Llegando",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(15.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(
                        RoundedCornerShape(13.dp)
                    )
                    .background(
                        SmartBusBlue.copy(
                            alpha = 0.10f
                        )
                    ),
                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        Icons.Default.DirectionsBus,
                    contentDescription = null,
                    tint = SmartBusBlue,
                    modifier =
                        Modifier.size(28.dp)
                )
            }

            Spacer(
                modifier =
                    Modifier.width(12.dp)
            )

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text = routeName,
                    fontWeight =
                        FontWeight.Bold,
                    color = SmartBusText
                )

                Text(
                    text = destination,
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        SmartBusTextSecondary
                )

                Spacer(
                    modifier =
                        Modifier.height(5.dp)
                )

                Surface(
                    shape =
                        RoundedCornerShape(20.dp),
                    color =
                        SmartBusGreenLight
                ) {

                    Text(
                        text = "● $status",
                        modifier =
                            Modifier.padding(
                                horizontal = 8.dp,
                                vertical = 3.dp
                            ),
                        color =
                            SmartBusGreen,
                        style =
                            MaterialTheme.typography
                                .bodySmall,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }

            Column(
                horizontalAlignment =
                    Alignment.End
            ) {

                Text(
                    text = eta,
                    style =
                        MaterialTheme.typography
                            .titleMedium,
                    fontWeight =
                        FontWeight.Bold,
                    color = SmartBusText
                )

                Text(
                    text = "min",
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                    color =
                        SmartBusTextSecondary
                )
            }
        }
    }
}