package io.fplpicks.application.service.model.predict

import io.fplpicks.adaptor.out.current.FetchFixtureSchedule
import io.fplpicks.adaptor.out.current.FetchRawCurrentFPLData
import io.fplpicks.adaptor.out.predictions.TeamOfTheWeekPredictionRepository
import io.fplpicks.application.model.PlayerGameweekFeatures
import io.fplpicks.application.model.PlayerPrediction
import io.fplpicks.application.port.out.PredictionStore
import io.fplpicks.application.service.ModelConfig
import io.fplpicks.application.service.PlayerGameweekDataParser
import io.fplpicks.application.service.TeamDataParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import smile.data.Tuple
import smile.regression.RandomForest
import java.io.FileInputStream
import java.io.ObjectInputStream
import kotlin.math.sqrt

class ModelPredictor(val fetchFPLData: FetchRawCurrentFPLData, val teamOfTheWeekPredictionStore: PredictionStore) {
    suspend fun allPredictionsForUpcomingGameweek() {

        val fplData = fetchFPLData.fetchCurrentData()
        val lastSeasonTeams = TeamDataParser().parseFromResource("/2023-24/teams.csv")
        val historicGameweekData = PlayerGameweekDataParser().parseFromResource("/2023-24/gws/merged_gw.csv", lastSeasonTeams)

        val form = historicGameweekData.groupBy { it.name }.mapValues { (_, value) -> value.toList() }
        val fixtureInfo = FetchFixtureSchedule().fetchUpcomingFixtures().groupBy { it.event }
        val nextEventKey = fixtureInfo.keys.min()

        val currentTeams = fplData.teams.associateBy { it.id }
        val playerToData = fplData.players.associateBy { "${it.firstName} ${it.secondName}" }

        val features = fplData.players.map {
            val key = "${it.firstName} ${it.secondName}"
            val playersTeam = currentTeams[it.team]
            val playerGameweekFeaturesList = mutableListOf<PlayerGameweekFeatures>()
            (nextEventKey..nextEventKey+4).map { gw ->
                val playersNextOpponent = fixtureInfo[gw]?.find { it.teamH == playersTeam?.id }?.teamHDifficulty ?: fixtureInfo[gw]?.find { it.teamA == playersTeam?.id }?.teamADifficulty
                val playerIsHome = fixtureInfo[gw]?.any { it.teamH == playersTeam?.id }

                val points = form[key]?.map { it.totalPoints } ?: emptyList()
                val averagePointsToDate = points.average()
                val variance = points.map { (it - averagePointsToDate) * (it - averagePointsToDate) }.average()
                val seasonPointsStdDev = sqrt(variance)

                playerGameweekFeaturesList.add(PlayerGameweekFeatures(
                    position = it.elementType,
                    opponentStrength = playersNextOpponent!!,
                    lastGamePoints = form[key]?.last()?.totalPoints ?: 0,
                    lastOpponentStrength = form[key]?.last()?.opponentTeamStrength ?: 0,
                    last2GamesPointsAvg = form[key]?.takeLast(2)?.map { it.totalPoints }?.average() ?: 0.0,
                    last3GamesPointsAvg = form[key]?.takeLast(3)?.map { it.totalPoints }?.average() ?: 0.0,
                    last3GamesOpponentStrengthAvg = form[key]?.takeLast(3)?.map { it.opponentTeamStrength }?.average() ?: 0.0,
                    last5GamesPointsAvg = form[key]?.takeLast(5)?.map { it.totalPoints }?.average() ?: 0.0,
                    last5GamesOpponentStrengthAvg = form[key]?.takeLast(5)?.map { it.opponentTeamStrength }?.average() ?: 0.0,
                    last5GamesCleansSheetsAvg = form[key]?.takeLast(5)?.map { it.cleanSheets }?.average() ?: 0.0,
                    last5GamesGoalsScoredAvg = form[key]?.takeLast(5)?.map { it.goalsScored }?.average() ?: 0.0,
                    last5GamesAssistsAvg = form[key]?.takeLast(5)?.map { it.assists }?.average() ?: 0.0,
                    last5GamesBonusPointsAvg = form[key]?.takeLast(5)?.map { it.assists }?.average() ?: 0.0,
                    seasonAvgPointsToDate = form[key]?.map { it.totalPoints }?.average() ?: 0.0,
                    seasonAvgMinutesPlayed = form[key]?.map { it.minutes }?.average() ?: 0.0,
                    seasonAvgCleanSheets = form[key]?.map { it.cleanSheets }?.average() ?: 0.0,
                    seasonAvgGoalsScored = form[key]?.map { it.goalsScored }?.average() ?: 0.0,
                    seasonAvgAssists = form[key]?.map { it.assists }?.average() ?: 0.0,
                    seasonAvgBonusPointsAvg = form[key]?.map { it.assists }?.average() ?: 0.0,
                    seasonPointsStdDev = seasonPointsStdDev,
                    isHome = playerIsHome ?: false,
                    points = 0.0
                ))
            }

            key to playerGameweekFeaturesList
        }

        println(features.size)

        val model: RandomForest = withContext(Dispatchers.IO) {
            FileInputStream("build/model/model.ser").use { fis ->
                ObjectInputStream(fis).use { ois ->
                    ois.readObject() as RandomForest
                }
            }
        }

        val playerToPrediction = features.map {
            val pointsPredictions = mutableListOf<Double>()
            it.second.map { features ->
                val modelRequest = Tuple.of(
                    ModelConfig.featureValues(features),
                    ModelConfig.schema()
                )
                val pointsPredicted = model.predict(modelRequest)
                pointsPredictions.add(pointsPredicted)
            }

            PlayerPrediction(
                name = "${playerToData[it.first]!!.firstName} ${playerToData[it.first]!!.secondName}",
                commonName = playerToData[it.first]!!.webName,
                image = playerToData[it.first]!!.photo.removeSuffix(".jpg"),
                position = when (playerToData[it.first]!!.elementType) {
                    1 -> "GKP"
                    2 -> "DEF"
                    3 -> "MID"
                    4 -> "FWD"
                    else -> "GKP"
                },
                team = playerToData[it.first]!!.team.toString(),
                value = playerToData[it.first]!!.nowCost.toDouble(),
                predictedPointsThisGW = pointsPredictions[0],
                predictedPointsGWPlus1 = pointsPredictions[1],
                predictedPointsGWPlus2 = pointsPredictions[2],
                predictedPointsGWPlus3 = pointsPredictions[3],
                predictedPointsGWPlus4 = pointsPredictions[4],
                pointsPerValue = (pointsPredictions[0] + pointsPredictions[1] + pointsPredictions[2] + pointsPredictions[3] + pointsPredictions[4]) / playerToData[it.first]!!.nowCost.toDouble(),
                pointsTotalUpcomingGWs = pointsPredictions[0] + pointsPredictions[1] + pointsPredictions[2] + pointsPredictions[3] + pointsPredictions[4],
                pointsAvgUpcomingGWs = (pointsPredictions[0] + pointsPredictions[1] + pointsPredictions[2] + pointsPredictions[3] + pointsPredictions[4]) / 5,
            )
        }

/*        val optimumSquad = SquadSelector(playerToPrediction).selectOptimalTeam()
        println(optimumSquad)
        println("starting 11 value: " + optimumSquad.first.sumOf { it.value })
        println("bench value: " + optimumSquad.second.sumOf { it.value })
        val captainName = optimumSquad.first.maxBy { it.predictedPointsThisGW }.name
        println("captain: $captainName")*/
        println(playerToPrediction.sortedByDescending { it.pointsTotalUpcomingGWs })

        val optimumSquad = SquadSelectorOptimiser(playerToPrediction).selectBestSquad()
        //println(optimumSquad)

        val timestamp = Clock.System.now()
        val gameweek = "2024-$nextEventKey"
        // store squad
        teamOfTheWeekPredictionStore.store(gameweek, optimumSquad, timestamp)
    }
}

suspend fun main() {
    ModelPredictor(FetchRawCurrentFPLData(), TeamOfTheWeekPredictionRepository()).allPredictionsForUpcomingGameweek()
}