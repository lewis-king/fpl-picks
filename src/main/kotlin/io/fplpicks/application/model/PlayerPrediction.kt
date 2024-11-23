package io.fplpicks.application.model

data class PlayerPrediction(
    val name: String,
    val commonName: String,
    val image: String,
    val position: String,
    val team: String,
    val value: Double,
    var predictedPointsThisGW: Double,
    var predictedPointsGWPlus1: Double,
    var predictedPointsGWPlus2: Double,
    var predictedPointsGWPlus3: Double,
    var predictedPointsGWPlus4: Double,
    val pointsPerValue: Double,
    var pointsTotalUpcomingGWs: Double,
    var pointsAvgUpcomingGWs: Double
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