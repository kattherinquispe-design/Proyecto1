package com.ejemplo.smartbus.data

import com.ejemplo.smartbus.data.model.RouteSchedule

object ScheduleRepository {

    val schedules: List<RouteSchedule> = listOf(

        RouteSchedule(
            routeId = 1,
            startTimeMinutes = 5 * 60 + 30,
            endTimeMinutes = 22 * 60 + 30,
            frequencyMinutes = 10,
            status = "Servicio normal"
        ),

        RouteSchedule(
            routeId = 2,
            startTimeMinutes = 6 * 60,
            endTimeMinutes = 22 * 60,
            frequencyMinutes = 15,
            status = "Servicio normal"
        )
    )

    fun getSchedule(routeId: Int): RouteSchedule? {
        return schedules.firstOrNull {
            it.routeId == routeId
        }
    }

    fun getNextDepartureMinutes(
        routeId: Int,
        currentMinutes: Int
    ): Int? {

        val schedule =
            getSchedule(routeId)
                ?: return null

        if (
            currentMinutes <
            schedule.startTimeMinutes
        ) {
            return schedule.startTimeMinutes -
                    currentMinutes
        }

        if (
            currentMinutes >
            schedule.endTimeMinutes
        ) {
            return null
        }

        val elapsed =
            currentMinutes -
                    schedule.startTimeMinutes

        val remainder =
            elapsed %
                    schedule.frequencyMinutes

        return if (remainder == 0) {
            0
        } else {
            schedule.frequencyMinutes -
                    remainder
        }
    }

    fun formatTime(totalMinutes: Int): String {

        val hours =
            totalMinutes / 60

        val minutes =
            totalMinutes % 60

        return String.format(
            "%02d:%02d",
            hours,
            minutes
        )
    }
}