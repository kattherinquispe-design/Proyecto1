package com.ejemplo.smartbus.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ejemplo.smartbus.data.model.PointTransaction
import com.ejemplo.smartbus.ui.components.SmartBusBottomBar
import com.ejemplo.smartbus.ui.components.SmartBusTab
import com.ejemplo.smartbus.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    history: List<PointTransaction>,
    currentTab: SmartBusTab,
    onTabChange: (SmartBusTab) -> Unit,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
    onHelpClick: () -> Unit
) {

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
                .padding(18.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text = "Historial de puntos",
                    style = MaterialTheme.typography
                        .headlineMedium
                )

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

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (history.isEmpty()) {
                    "Sin movimientos registrados"
                } else {
                    "${history.size} movimientos"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = SmartBusTextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (history.isEmpty()) {

                EmptyHistoryCard()

            } else {

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    items(history) { transaction ->

                        HistoryRow(transaction)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryCard() {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            tint = SmartBusTextLight,
            modifier = Modifier.size(52.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Todavía no tienes movimientos.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Sube a un bus en un paradero para empezar a " +
                "acumular puntos.",
            style = MaterialTheme.typography.bodySmall,
            color = SmartBusTextSecondary
        )
    }
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