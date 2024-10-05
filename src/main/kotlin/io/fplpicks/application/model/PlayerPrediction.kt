package io.fplpicks.application.model

data class PlayerPrediction(
    val name: String,
    val commonName: String,
    val image: String,
    val position: String,
    val team: String,
    val value: Double,
    var predictedPointsThisGW: Double,
    val predictedPointsGWPlus1: Double,
    val predictedPointsGWPlus2: Double,
    val predictedPointsGWPlus3: Double,
    val predictedPointsGWPlus4: Double,
    val pointsPerValue: Double,
    val pointsTotalUpcomingGWs: Double,
    val pointsAvgUpcomingGWs: Double
)

fun PlayerPrediction.calculateWeightedScore(): Double {
    val weights = listOf(1.5, 0.4, 0.3, 0.2, 0.1)
    val points = listOf(
        predictedPointsThisGW,
        predictedPointsGWPlus1,
        predictedPointsGWPlus2,
        predictedPointsGWPlus3,
        predictedPointsGWPlus4
    )

    return points.zip(weights).sumOf { (point, weight) -> point * weight } +
            (pointsPerValue * 0.5) // Consider points per value as a tiebreaker
}