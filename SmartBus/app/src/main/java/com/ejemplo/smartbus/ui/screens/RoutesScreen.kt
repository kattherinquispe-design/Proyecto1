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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ejemplo.smartbus.data.BusEtaCalculator
import com.ejemplo.smartbus.data.BusLiveRepository
import com.ejemplo.smartbus.data.ScheduleRepository
import com.ejemplo.smartbus.data.model.BusRoute
import java.util.Calendar


@Composable
fun RoutesScreen(
    routes: List<BusRoute>,
    onViewRoute: (BusRoute) -> Unit,
    onBack: () -> Unit
) {

    val buses by
    BusLiveRepository.buses.collectAsState()


    // =========================================================
    // HORA ACTUAL
    // =========================================================

    val calendar =
        Calendar.getInstance()


    val currentMinutes =
        calendar.get(
            Calendar.HOUR_OF_DAY
        ) * 60 +
                calendar.get(
                    Calendar.MINUTE
                )


    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
    ) {

        Text(
            text =
                "Ruta Rutas SmartBus",

            style =
                MaterialTheme
                    .typography
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

            items(routes) { route ->

                val routeBuses =
                    buses.filter {
                        it.routeId == route.id
                    }


                val schedule =
                    ScheduleRepository
                        .getSchedule(route.id)


                val firstStopId =
                    route.stopIds.firstOrNull()


                val firstStop =
                    firstStopId?.let { id ->

                        com.ejemplo.smartbus
                            .data.BusStopRepository
                            .stops
                            .firstOrNull {
                                it.id == id
                            }
                    }


                val closestBus =
                    if (firstStop != null) {

                        BusEtaCalculator
                            .findClosestBus(
                                routeBuses,
                                firstStop
                            )

                    } else {

                        null
                    }


                val eta =
                    if (
                        closestBus != null &&
                        firstStop != null
                    ) {

                        BusEtaCalculator
                            .calculateEtaMinutes(
                                closestBus,
                                firstStop
                            )

                    } else {

                        null
                    }


                Card(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier =
                            Modifier.padding(16.dp)
                    ) {

                        // =====================================
                        // RUTA
                        // =====================================

                        Text(
                            text =
                                "Ruta Ruta ${route.code}",

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
                                route.name
                        )


                        Text(
                            text =
                                "Paradero relacionado ${route.origin} → ${route.destination}"
                        )


                        Spacer(
                            modifier =
                                Modifier.height(10.dp)
                        )


                        // =====================================
                        // HORARIOS
                        // =====================================

                        if (
                            schedule != null
                        ) {

                            Text(
                                text =
                                    "Fecha Horario: ${
                                        ScheduleRepository
                                            .formatTime(
                                                schedule.startTimeMinutes
                                            )
                                    } - ${
                                        ScheduleRepository
                                            .formatTime(
                                                schedule.endTimeMinutes
                                            )
                                    }"
                            )


                            Text(
                                text =
                                    "Cada Cada ${
                                        schedule.frequencyMinutes
                                    } minutos"
                            )


                            Text(
                                text =
                                    "[ACTIVO] ${schedule.status}"
                            )
                        }


                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )


                        // =====================================
                        // ETA
                        // =====================================

                        if (
                            eta != null &&
                            closestBus != null
                        ) {

                            Text(
                                text =
                                    if (
                                        eta == 0
                                    ) {

                                        "Proximo bus: esta llegando"

                                    } else {

                                        "Proximo bus al origen Próximo bus al origen: $eta min"
                                    },

                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium
                            )


                            Text(
                                text =
                                    "Ruta ${closestBus.code}"
                            )

                        } else {

                            Text(
                                text =
                                    "Proximo bus al origen ETA no disponible"
                            )
                        }


                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )


                        // =====================================
                        // PARADEROS
                        // =====================================

                        Text(
                            text =
                                "Paradero relacionado Paraderos: ${route.stopIds.size}"
                        )


                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )


                        // =====================================
                        // BUSES EN VIVO DEL PROTOTIPO
                        // =====================================

                        Text(
                            text =
                                "Ruta Buses activos: ${routeBuses.size}"
                        )


                        Spacer(
                            modifier =
                                Modifier.height(6.dp)
                        )


                        routeBuses.forEach { bus ->

                            Column(
                                modifier =
                                    Modifier.fillMaxWidth()
                            ) {

                                Text(
                                    text =
                                        "Ruta ${bus.code}"
                                )


                                Text(
                                    text =
                                        "Paradero relacionado ${bus.direction}"
                                )


                                Text(
                                    text =
                                        "Velocidad ${bus.speedKmh} km/h"
                                )


                                Text(
                                    text =
                                        "[ACTIVO] ${bus.status}"
                                )
                            }


                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )
                        }


                        // =====================================
                        // MAPA
                        // =====================================

                        Button(
                            onClick = {

                                onViewRoute(
                                    route
                                )
                            },

                            modifier =
                                Modifier.fillMaxWidth()
                        ) {

                            Text(
                                text =
                                    "Ver recorrido Ver recorrido"
                            )
                        }
                    }
                }
            }
        }


        Spacer(
            modifier =
                Modifier.height(12.dp)
        )


        OutlinedButton(

            onClick =
                onBack,

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                text =
                    "Volver"
            )
        }
    }
}