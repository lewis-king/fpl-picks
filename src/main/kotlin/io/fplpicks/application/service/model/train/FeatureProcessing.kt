package io.fplpicks.application.service.model.train

import io.fplpicks.application.model.PlayerGameweekData
import io.fplpicks.application.model.PlayerGameweekFeatures
import kotlin.math.sqrt

class FeatureProcessing {

    fun createFeatures(gameweekData: List<PlayerGameweekData>): List<PlayerGameweekFeatures> {
        return gameweekData.groupBy { it.name }
            .flatMap { (playerName, playerGameweeks) ->
                playerGameweeks.mapIndexed { index, currentGameweek ->
                    createFeature(
                        playerName = playerName,
                        previousGameweeks = playerGameweeks.take(index),
                        currentGameweek = currentGameweek,
                        futureGameweeks = playerGameweeks.drop(index + 1)
                    )
                }
            }
    }

    private fun createFeature(playerName: String,
                              previousGameweeks: List<PlayerGameweekData>,
                              currentGameweek: PlayerGameweekData,
                              futureGameweeks: List<PlayerGameweekData>): PlayerGameweekFeatures {

        val points = previousGameweeks.map { it.totalPoints }
        val averagePointsToDate = points.average()
        val variance = points.map { (it - averagePointsToDate) * (it - averagePointsToDate) }.average()
        val seasonPointsStdDev = sqrt(variance)

        return PlayerGameweekFeatures(
            position = currentGameweek.positionId,
            opponentStrength = currentGameweek.opponentTeamStrength,
            lastGamePoints = previousGameweeks.takeLast(1).firstOrNull()?.totalPoints ?: 0,
            lastOpponentStrength = previousGameweeks.takeLast(1).firstOrNull()?.opponentTeamStrength ?: 0,
            last2GamesPointsAvg = previousGameweeks.takeLast(2).map { it.totalPoints }.average(),
            last3GamesPointsAvg = previousGameweeks.takeLast(3).map { it.totalPoints }.average(),
            last3GamesOpponentStrengthAvg = previousGameweeks.takeLast(3).map { it.opponentTeamStrength }.average(),
            last5GamesPointsAvg = previousGameweeks.takeLast(5).map { it.totalPoints }.average(),
            last5GamesOpponentStrengthAvg = previousGameweeks.takeLast(5).map { it.opponentTeamStrength }.average(),
            last5GamesCleansSheetsAvg = previousGameweeks.takeLast(5).map { it.cleanSheets }.average(),
            last5GamesGoalsScoredAvg = previousGameweeks.takeLast(5).map { it.goalsScored }.average(),
            last5GamesAssistsAvg = previousGameweeks.takeLast(5).map { it.assists }.average(),
            last5GamesBonusPointsAvg = previousGameweeks.takeLast(5).map { it.bonus }.average(),
            seasonAvgPointsToDate = averagePointsToDate,
            seasonAvgMinutesPlayed = previousGameweeks.map { it.minutes }.average(),
            seasonPointsStdDev = seasonPointsStdDev,
            seasonAvgCleanSheets = previousGameweeks.map { it.cleanSheets }.average(),
            seasonAvgGoalsScored = previousGameweeks.map { it.goalsScored }.average(),
            seasonAvgAssists = previousGameweeks.map { it.assists }.average(),
            seasonAvgBonusPointsAvg = previousGameweeks.map { it.bonus }.average(),
            isHome = currentGameweek.wasHome,
            points = currentGameweek.totalPoints.toDouble()
        )
    }
}