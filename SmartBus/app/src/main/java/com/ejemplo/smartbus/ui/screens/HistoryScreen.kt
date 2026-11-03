package com.ejemplo.smartbus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ejemplo.smartbus.data.SmartBusStatistics
import com.ejemplo.smartbus.data.model.PointTransaction
import com.ejemplo.smartbus.ui.components.SmartBusBottomBar
import com.ejemplo.smartbus.ui.components.SmartBusTab
import com.ejemplo.smartbus.ui.theme.SmartBusBackground
import com.ejemplo.smartbus.ui.theme.SmartBusBlue
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

    var filter by remember { mutableStateOf(HistoryFilter.ALL) }

    val filtered = remember(history, filter) {
        when (filter) {
            HistoryFilter.ALL -> history
            HistoryFilter.POINTS -> history.filter { it.points > 0 }
            HistoryFilter.REDEEMS -> history.filter { it.points < 0 }
        }
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
                .padding(18.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Historial",
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

            Spacer(modifier = Modifier.height(14.dp))

            HistorySummaryCard(
                points = points,
                boardingCount = statistics.boardingCount,
                rewardsRedeemed = statistics.rewardsRedeemed
            )

            Spacer(modifier = Modifier.height(16.dp))

            HistoryFilterBar(
                selected = filter,
                onSelect = { filter = it }
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (filtered.isEmpty()) {

                EmptyHistoryCard(filter = filter)

            } else {

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    items(filtered) { transaction ->

                        HistoryRow(transaction)
                    }
                }
            }
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
 * Estado vacio: figura de regalo, mensaje y un cuadrito
 * explicando como ganar puntos.
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
            text = "Aún no tienes historial disponible",
            style = MaterialTheme.typography.titleMedium,
            color = SmartBusText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = when (filter) {
                HistoryFilter.POINTS ->
                    "Cuando ganes puntos aparecerán aquí."
                HistoryFilter.REDEEMS ->
                    "Cuando canjes una recompensa aparecerá aquí."
                HistoryFilter.ALL ->
                    "Sube a un bus en un paradero para empezar."
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
 * Cuadrito con la forma de ganar puntos, solo se muestra
 * cuando no hay historial.
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

            EarnStep(text = "1. Aborda un bus en cualquier paradero.")
            EarnStep(text = "2. El GPS confirma tu viaje automáticamente.")
            EarnStep(text = "3. Suma puntos y canjéalos por premios.")
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
 * Resumen en tarjeta con degradado (estilo de la referencia):
 * puntos acumulados, abordajes y canjes.
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
                        text = "$points puntos",
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
