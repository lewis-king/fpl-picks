package io.fplpicks.application.model

data class PlayerGameweekFeatures(
    val position: Int,
    val opponentStrength: Int,
    val lastGamePoints: Int,
    val lastOpponentStrength: Int,
    val last2GamesPointsAvg: Double,
    val last3GamesPointsAvg: Double,
    val last3GamesOpponentStrengthAvg: Double,
    val last5GamesPointsAvg: Double,
    val last5GamesOpponentStrengthAvg: Double,
    val seasonAvgPointsToDate: Double,
    val seasonPointsStdDev: Double,
    val seasonAvgMinutesPlayed: Double,
    val isHome: Boolean,
    val points: Double,
 )