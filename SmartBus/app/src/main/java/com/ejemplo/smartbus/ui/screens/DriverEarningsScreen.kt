package com.ejemplo.smartbus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ejemplo.smartbus.data.model.DriverTrip
import com.ejemplo.smartbus.ui.theme.SmartBusBackground
import com.ejemplo.smartbus.ui.theme.SmartBusBlue
import com.ejemplo.smartbus.ui.theme.SmartBusGreen
import com.ejemplo.smartbus.ui.theme.SmartBusText
import com.ejemplo.smartbus.ui.theme.SmartBusTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Historial de viajes y dinero del conductor.
 */
@Composable
fun DriverEarningsScreen(
    trips: List<DriverTrip>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {

    val total = trips.sumOf { it.fare }

    val todayTotal = todayTrips(trips).sumOf { it.fare }

    val formatter = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SmartBusBackground)
    ) {

        Column(modifier = Modifier.padding(20.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {

                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = SmartBusBlue,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(onClick = onBack)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Ganancias",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = SmartBusText
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {

                EarningsBox(
                    label = "Hoy",
                    value = "S/ ${"%.2f".format(todayTotal)}",
                    tint = SmartBusBlue,
                    modifier = Modifier.weight(1f)
                )

                EarningsBox(
                    label = "Total",
                    value = "S/ ${"%.2f".format(total)}",
                    tint = SmartBusGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (trips.isEmpty()) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Todavia no tienes viajes",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SmartBusText
                )
                Text(
                    text = "Cuando termines tu primer viaje aparecera aqui " +
                        "con su horario y su tarifa.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SmartBusTextSecondary
                )
            }

            return
        }

        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                bottom = 30.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            items(trips, key = { it.id }) { trip ->
                TripRow(trip, formatter)
            }
        }
    }
}

@Composable
private fun EarningsBox(
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {

        Column(modifier = Modifier.padding(16.dp)) {

            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = SmartBusTextSecondary
            )

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = tint
            )
        }
    }
}

@Composable
private fun TripRow(
    trip: DriverTrip,
    formatter: SimpleDateFormat
) {

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = SmartBusBlue,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {

                Text(
                    text = "${trip.originName} - ${trip.destinationName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = SmartBusText
                )

                Text(
                    text = buildString {
                        append(formatter.format(Date(trip.startedAt)))
                        append(" - ")
                        append(
                            "${trip.distanceMeters.toInt() / 1000.0} km".let {
                                if (trip.distanceMeters < 1000f) "0 km" else it
                            }
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = SmartBusTextSecondary
                )
            }

            Text(
                text = "S/ ${"%.2f".format(trip.fare)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = SmartBusGreen
            )
        }
    }
}