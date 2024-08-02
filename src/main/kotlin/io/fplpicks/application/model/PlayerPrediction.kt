package io.fplpicks.application.model

data class PlayerPrediction(
    val name: String,
    val position: String,
    val value: Double,
    val predictedPointsThisGW: Double,
    val predictedPointsGWPlus1: Double,
    val predictedPointsGWPlus2: Double,
    val predictedPointsGWPlus3: Double,
    val predictedPointsGWPlus4: Double,
    val pointsPerValue: Double,
    val pointsTotalUpcomingGWs: Double,
    val pointsAvgUpcomingGWs: Double,
    val team: String
)
