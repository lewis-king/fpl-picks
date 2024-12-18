package io.fplpicks.application.model

data class PlayerGameweekFeatures(
    val position: Int,
    val isHome: Boolean,
    val opponentStrength: Int,
    val opponentDifficulty: Int,
    val homeVsOpponent: Double,
    val lastOpponentStrength: Int,
    val last3GamesOpponentStrengthAvg: Double,
    val last5GamesOpponentStrengthAvg: Double,
    val last5GamesCleansSheetsAvg: Double,
    val last5GamesGoalsScoredAvg: Double,
    val last5GamesAssistsAvg: Double,
    val last5GamesBonusPointsAvg: Double,
    val last5GamesFormTrend: Double,
    val lastGamePoints: Int,
    val lastGameXP: Double,
    //val last2GamesPointsAvg: Double,
    val last3GamesPointsAvg: Double,
    val last3GamesXPAvg: Double,
    val last5GamesPointsAvg: Double,
    val last5GamesXPAvg: Double,
    val last5GamesPointsExponentialMovingAverage: Double,
    val last5GamesXPExponentialMovingAverage: Double,
    val seasonAvgPointsToDate: Double,
    val seasonAvgXPToDate: Double,
    val seasonPointsStdDev: Double,
    //val seasonAvgCleanSheets: Double,
    //val seasonAvgGoalsScored: Double,
    //val seasonAvgAssists: Double,
    val seasonAvgBonusPointsAvg: Double,
    val seasonAvgMinutesPlayed: Double,
    val seasonConsistencyScore: Double,
    val homeAwayPointsDiff: Double,
    val points: Double,
 )