package io.fplpicks.application.service.model.train

import io.fplpicks.application.model.PlayerGameweekData
import io.fplpicks.application.model.PlayerGameweekFeatures
import io.fplpicks.application.service.Statistics
import io.fplpicks.application.service.model.predict.Point
import kotlin.math.pow
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

        val pointedPoints = previousGameweeks.takeLast(5).mapIndexed { index, points ->
            Point(index.toDouble(), points.totalPoints.toDouble())
        }

        val sumX = pointedPoints.sumOf { it.x }
        val sumY = pointedPoints.sumOf { it.y }
        val sumXY = pointedPoints.sumOf { it.x * it.y }
        val sumXSquare = pointedPoints.sumOf { it.x.pow(2) }

        // Calculate the slope of the line of best fit
        val formTrend = (pointedPoints.size * sumXY - sumX * sumY) / (pointedPoints.size * sumXSquare - sumX.pow(2))

        val homeGames = previousGameweeks.filter { it.wasHome }
        val awayGames = previousGameweeks.filter { !it.wasHome }

        val homeAvg = homeGames.sumOf { it.totalPoints } / homeGames.size.toDouble()
        val awayAvg = awayGames.sumOf { it.totalPoints } / awayGames.size.toDouble()
        val homeAwayDiff = homeAvg - awayAvg

        val seasonConsistencyScore = 1 - ((seasonPointsStdDev / previousGameweeks.map { it.totalPoints }.average()))

        return PlayerGameweekFeatures(
            position = currentGameweek.positionId,
            opponentStrength = currentGameweek.opponentTeamStrength,
            opponentDifficulty = currentGameweek.opponentTeamStrength * 2,
            homeVsOpponent = if (currentGameweek.wasHome) currentGameweek.opponentTeamStrength * 2.0 else currentGameweek.opponentTeamStrength.toDouble(),
            lastGamePoints = previousGameweeks.takeLast(1).firstOrNull()?.totalPoints ?: 0,
            lastGameXP = previousGameweeks.takeLast(1).firstOrNull()?.xP ?: 0.0,
            lastOpponentStrength = previousGameweeks.takeLast(1).firstOrNull()?.opponentTeamStrength ?: 0,
            //last2GamesPointsAvg = previousGameweeks.takeLast(2).map { it.totalPoints }.average(),
            last3GamesPointsAvg = previousGameweeks.takeLast(3).map { it.totalPoints }.average(),
            last3GamesXPAvg = previousGameweeks.takeLast(3).map { it.xP }.average(),
            last3GamesOpponentStrengthAvg = previousGameweeks.takeLast(3).map { it.opponentTeamStrength }.average(),
            last5GamesPointsAvg = previousGameweeks.takeLast(5).map { it.totalPoints }.average(),
            last5GamesXPAvg = previousGameweeks.takeLast(5).map { it.xP }.average(),
            last5GamesPointsExponentialMovingAverage = Statistics.calculateExponentialMovingAverage(previousGameweeks.takeLast(5)),
            last5GamesXPExponentialMovingAverage = Statistics.calculateExponentialMovingAverage(previousGameweeks.takeLast(5)),
            last5GamesOpponentStrengthAvg = previousGameweeks.takeLast(5).map { it.opponentTeamStrength }.average(),
            last5GamesCleansSheetsAvg = previousGameweeks.takeLast(5).map { it.cleanSheets }.average(),
            last5GamesGoalsScoredAvg = previousGameweeks.takeLast(5).map { it.goalsScored }.average(),
            last5GamesAssistsAvg = previousGameweeks.takeLast(5).map { it.assists }.average(),
            last5GamesBonusPointsAvg = previousGameweeks.takeLast(5).map { it.bonus }.average(),
            last5GamesFormTrend = formTrend,
            seasonAvgPointsToDate = averagePointsToDate,
            seasonAvgXPToDate = previousGameweeks.map { it.xP }.average(),
            seasonAvgMinutesPlayed = previousGameweeks.map { it.minutes }.average(),
            seasonPointsStdDev = seasonPointsStdDev,
            //seasonAvgCleanSheets = previousGameweeks.map { it.cleanSheets }.average(),
            //seasonAvgGoalsScored = previousGameweeks.map { it.goalsScored }.average(),
            //seasonAvgAssists = previousGameweeks.map { it.assists }.average(),
            seasonAvgBonusPointsAvg = previousGameweeks.map { it.bonus }.average(),
            isHome = currentGameweek.wasHome,
            homeAwayPointsDiff = homeAwayDiff,
            seasonConsistencyScore = seasonConsistencyScore,
            points = currentGameweek.totalPoints.toDouble()
        )
    }
}