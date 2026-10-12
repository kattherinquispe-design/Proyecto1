package com.ejemplo.smartbus.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ejemplo.smartbus.data.model.BusStop


@Composable
fun BusStopsScreen(
    stops: List<BusStop>,
    onViewLocation: (BusStop) -> Unit,
    onBack: () -> Unit
) {

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
    ) {

        Text(
            text = "Paraderos SmartBus",
            style =
                MaterialTheme.typography
                    .headlineMedium
        )


        Spacer(
            modifier =
                Modifier.height(16.dp)
        )


        LazyColumn(
            modifier =
                Modifier.weight(1f),

            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            items(stops) { stop ->

                Card(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier =
                            Modifier.padding(16.dp)
                    ) {

                        Text(
                            text =
                                "Paradero relacionado ${stop.name}",

                            style =
                                MaterialTheme
                                    .typography
                                    .titleLarge
                        )


                        Spacer(
                            modifier =
                                Modifier.height(6.dp)
                        )


                        Text(
                            text =
                                "Latitud: ${stop.latitude}"
                        )


                        Text(
                            text =
                                "Longitud: ${stop.longitude}"
                        )


                        Text(
                            text =
                                "Radio: ${
                                    stop.radiusMeters
                                        .toInt()
                                } metros"
                        )


                        Spacer(
                            modifier =
                                Modifier.height(10.dp)
                        )


                        Button(
                            onClick = {
                                onViewLocation(stop)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Ver recorrido Ver ubicación")
                        }
                    }
                }
            }
        }


        OutlinedButton(
            onClick = onBack,

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text("Volver")
        }
    }
}