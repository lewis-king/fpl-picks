package io.fplpicks.application.model

data class PlayerGameweekFeatures(
    val position: Int,
    val isHome: Boolean,
    val opponentStrength: Int,
    val lastOpponentStrength: Int,
    val last3GamesOpponentStrengthAvg: Double,
    val last5GamesOpponentStrengthAvg: Double,
    val lastGamePoints: Int,
    val last2GamesPointsAvg: Double,
    val last3GamesPointsAvg: Double,
    val last5GamesPointsAvg: Double,
    val seasonAvgPointsToDate: Double,
    val seasonPointsStdDev: Double,
    val seasonAvgMinutesPlayed: Double,
    val points: Double,
 )