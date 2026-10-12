package com.ejemplo.smartbus.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ejemplo.smartbus.data.model.BusStop
import kotlin.math.roundToInt

@Composable
fun BusStopStatusCard(
    nearestStop: BusStop?,
    distanceMeters: Float?,
    isInsideStop: Boolean
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            if (isInsideStop && nearestStop != null) {

                Text(
                    text = "Ruta ¡Estás en un paradero!",
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = nearestStop.name,
                    style = MaterialTheme.typography.titleMedium
                )

                if (distanceMeters != null) {
                    Text(
                        text = "Distancia: ${distanceMeters.roundToInt()} m"
                    )
                }

                Text(
                    text = "Puntos Permaneces dentro del área del paradero."
                )

            } else if (nearestStop != null) {

                Text(
                    text = "Paradero relacionado Paradero más cercano",
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = nearestStop.name,
                    style = MaterialTheme.typography.titleMedium
                )

                if (distanceMeters != null) {

                    Text(
                        text = "Distancia: ${distanceMeters.roundToInt()} m"
                    )

                    Text(
                        text = "Acércate al paradero para estar dentro del área."
                    )
                }

            } else {

                Text(
                    text = "Paradero relacionado No hay paraderos disponibles",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}