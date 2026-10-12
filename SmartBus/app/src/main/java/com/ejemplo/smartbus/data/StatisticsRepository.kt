package com.ejemplo.smartbus.data

data class SmartBusStatistics(
    val boardingCount: Int,
    val rewardsRedeemed: Int,
    val stopsUsed: Int
)

class StatisticsRepository(
    private val pointsRepository: PointsRepository
) {

    fun getStatistics(): SmartBusStatistics {

        val history =
            pointsRepository.getHistory()

        val boardingCount =
            history.count {
                it.reason == "Abordaje validado"
            }

        val rewardsRedeemed =
            history.count {
                it.reason == "Canje de recompensa"
            }

        val stopsUsed =
            history
                .filter {
                    it.reason == "Abordaje validado"
                }
                .map {
                    it.stopId
                }
                .distinct()
                .size

        return SmartBusStatistics(
            boardingCount = boardingCount,
            rewardsRedeemed = rewardsRedeemed,
            stopsUsed = stopsUsed
        )
    }
}