package io.fplpicks.application.service.model.predict

import io.fplpicks.adaptor.out.current.FetchFixtureSchedule
import io.fplpicks.adaptor.out.current.FetchRawCurrentFPLData
import io.fplpicks.adaptor.out.lineups.FetchPredictedLineups
import io.fplpicks.adaptor.out.predictions.TeamOfTheWeekPredictionRepository
import io.fplpicks.application.model.PlayerGameweekFeatures
import io.fplpicks.application.model.PlayerPrediction
import io.fplpicks.application.port.out.PredictionStore
import io.fplpicks.application.service.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import smile.data.Tuple
import smile.regression.RandomForest
import java.io.FileInputStream
import java.io.ObjectInputStream
import java.text.Normalizer
import java.util.regex.Pattern
import kotlin.math.pow
import kotlin.math.sqrt

class ModelPredictor(val fetchFPLData: FetchRawCurrentFPLData, val teamOfTheWeekPredictionStore: PredictionStore) {
    suspend fun allPredictionsForUpcomingGameweek() {

        val fplData = fetchFPLData.fetchCurrentData()
        val lastSeasonTeams = TeamDataParser().parseFromResource("/2023-24/teams.csv")
        val thisSeasonTeams = TeamDataParser().parseFromResource("/2024-25/teams.csv")
        val historicGameweekData = PlayerGameweekDataParser().parseFromResource("/2023-24/gws/merged_gw.csv", lastSeasonTeams) + PlayerGameweekDataParser().parseFromURL("https://raw.githubusercontent.com/vaastav/Fantasy-Premier-League/master/data/2024-25/gws/merged_gw.csv", thisSeasonTeams)

        val form = historicGameweekData.groupBy { it.name }.mapValues { (_, value) -> value.toList() }.filter { it.value.size >= 5 }
        val fixtureInfo = FetchFixtureSchedule().fetchUpcomingFixtures().groupBy { it.event }
        val nextEventKey = fixtureInfo.keys.min()

        val currentTeams = fplData.teams.associateBy { it.id }
        val playerToData = fplData.players.associateBy { "${it.firstName} ${it.secondName}" }

        val playersWithEnoughHistoricData = fplData.players.filter { form["${it.firstName} ${it.secondName}"] != null }

        val features = playersWithEnoughHistoricData.map {
            val key = "${it.firstName} ${it.secondName}"
            val playersTeam = currentTeams[it.team]
            val playerGameweekFeaturesList = mutableListOf<PlayerGameweekFeatures>()
            (nextEventKey..nextEventKey+4).map { gw ->
                //old playerOpponentStrength
                //val playersNextOpponent = fixtureInfo[gw]?.find { it.teamH == playersTeam?.id }?.teamHDifficulty ?: fixtureInfo[gw]?.find { it.teamA == playersTeam?.id }?.teamADifficulty
                val playersNextOpponentName = fixtureInfo[gw]?.find { it.teamH == playersTeam?.id }?.teamA ?: fixtureInfo[gw]?.find { it.teamA == playersTeam?.id }?.teamH
                val playersNextOpponent = TeamStrength.teamToStrength[playersNextOpponentName]
                val playerIsHome = fixtureInfo[gw]?.any { it.teamH == playersTeam?.id }

                val points = form[key]?.map { it.totalPoints } ?: emptyList()
                val averagePointsToDate = points.average()
                val variance = points.map { (it - averagePointsToDate) * (it - averagePointsToDate) }.average()
                val seasonPointsStdDev = sqrt(variance)

                val pointedPoints = form[key]?.takeLast(5)?.mapIndexed { index, points ->
                    Point(index.toDouble(), points.totalPoints.toDouble())
                } ?: emptyList()

                val sumX = pointedPoints.sumOf { it.x }
                val sumY = pointedPoints.sumOf { it.y }
                val sumXY = pointedPoints.sumOf { it.x * it.y }
                val sumXSquare = pointedPoints.sumOf { it.x.pow(2) }

                // Calculate the slope of the line of best fit
                val formTrend = (pointedPoints.size * sumXY - sumX * sumY) / (pointedPoints.size * sumXSquare - sumX.pow(2))

                val homeGames = form[key]?.filter { it.wasHome } ?: emptyList()
                val awayGames = form[key]?.filter { !it.wasHome } ?: emptyList()

                val homeAvg = homeGames.sumOf { it.totalPoints } / homeGames.size.toDouble()
                val awayAvg = awayGames.sumOf { it.totalPoints } / awayGames.size.toDouble()
                val homeAwayDiff = homeAvg - awayAvg

                var seasonConsistencyScore = 0.0
                if (form[key] != null) {
                    seasonConsistencyScore = 1 - ((seasonPointsStdDev / form[key]!!.map { it.totalPoints }.average()))
                }

                playerGameweekFeaturesList.add(PlayerGameweekFeatures(
                    position = it.elementType,
                    opponentStrength = playersNextOpponent!!,
                    opponentDifficulty = form[key]?.last()?.opponentTeamStrength!! * 2,
                    homeVsOpponent = if (form[key]?.last()?.wasHome!!) form[key]?.last()?.opponentTeamStrength!! * 2.0 else form[key]?.last()?.opponentTeamStrength!!.toDouble(),
                    lastGamePoints = form[key]?.last()?.totalPoints ?: 0,
                    lastGameXP = form[key]?.last()?.xP ?: 0.0,
                    lastOpponentStrength = form[key]?.last()?.opponentTeamStrength ?: 0,
                    //last2GamesPointsAvg = form[key]?.takeLast(2)?.map { it.totalPoints }?.average() ?: 0.0,
                    last3GamesPointsAvg = form[key]?.takeLast(3)?.map { it.totalPoints }?.average() ?: 0.0,
                    last3GamesXPAvg = form[key]?.takeLast(3)?.map { it.xP }?.average() ?: 0.0,
                    last3GamesOpponentStrengthAvg = form[key]?.takeLast(3)?.map { it.opponentTeamStrength }?.average() ?: 0.0,
                    last5GamesPointsAvg = form[key]?.takeLast(5)?.map { it.totalPoints }?.average() ?: 0.0,
                    last5GamesXPAvg = form[key]?.takeLast(5)?.map { it.xP }?.average() ?: 0.0,
                    last5GamesPointsExponentialMovingAverage = Statistics.calculateExponentialMovingAverage(form[key]?.takeLast(5)),
                    last5GamesXPExponentialMovingAverage = Statistics.calculateExponentialMovingAverage(form[key]?.takeLast(5)),
                    last5GamesOpponentStrengthAvg = form[key]?.takeLast(5)?.map { it.opponentTeamStrength }?.average() ?: 0.0,
                    last5GamesCleansSheetsAvg = form[key]?.takeLast(5)?.map { it.cleanSheets }?.average() ?: 0.0,
                    last5GamesGoalsScoredAvg = form[key]?.takeLast(5)?.map { it.goalsScored }?.average() ?: 0.0,
                    last5GamesAssistsAvg = form[key]?.takeLast(5)?.map { it.assists }?.average() ?: 0.0,
                    last5GamesBonusPointsAvg = form[key]?.takeLast(5)?.map { it.assists }?.average() ?: 0.0,
                    last5GamesFormTrend = formTrend,
                    seasonAvgPointsToDate = form[key]?.map { it.totalPoints }?.average() ?: 0.0,
                    seasonAvgXPToDate = form[key]?.map { it.xP }?.average() ?: 0.0,
                    seasonAvgMinutesPlayed = form[key]?.map { it.minutes }?.average() ?: 0.0,
                    //seasonAvgCleanSheets = form[key]?.map { it.cleanSheets }?.average() ?: 0.0,
                    //seasonAvgGoalsScored = form[key]?.map { it.goalsScored }?.average() ?: 0.0,
                    //seasonAvgAssists = form[key]?.map { it.assists }?.average() ?: 0.0,
                    seasonAvgBonusPointsAvg = form[key]?.map { it.assists }?.average() ?: 0.0,
                    seasonPointsStdDev = seasonPointsStdDev,
                    isHome = playerIsHome ?: false,
                    homeAwayPointsDiff = homeAwayDiff,
                    seasonConsistencyScore = seasonConsistencyScore,
                    points = 0.0
                ))
            }
            key to playerGameweekFeaturesList
        }.filter { it.second.sumOf { it.seasonAvgMinutesPlayed } > 15.0 }

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
                commonName = sanitiseNames[playerToData[it.first]!!.webName] ?: playerToData[it.first]!!.webName.removeAccents(),
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

        val playerToPredictionMap = playerToPrediction.associateBy { it.name }

/*        val optimumSquad = SquadSelector(playerToPrediction).selectOptimalTeam()
        println(optimumSquad)
        println("starting 11 value: " + optimumSquad.first.sumOf { it.value })
        println("bench value: " + optimumSquad.second.sumOf { it.value })
        val captainName = optimumSquad.first.maxBy { it.predictedPointsThisGW }.name
        println("captain: $captainName")*/
        println(playerToPrediction.sortedByDescending { it.pointsTotalUpcomingGWs })

        val expectedLineups = FetchPredictedLineups().fetch()
        val currentSquad = teamOfTheWeekPredictionStore.retrieve(gameweek = "2024-${nextEventKey - 1}")
        // we've got last week's squad which includes the predictions they had from last week but now we must replace the predictions with the new predictions.
        val currentSquadStartingPlayersNewPredictions = currentSquad?.startingPlayers?.map { playerToPredictionMap[it.name]!! } ?: emptyList()
        val currentSquadBenchPlayersNewPredictions = currentSquad?.benchPlayers?.map { playerToPredictionMap[it.name]!! } ?: emptyList()
        val currentSquadNewPredictions = currentSquad?.copy(startingPlayers = currentSquadStartingPlayersNewPredictions, benchPlayers = currentSquadBenchPlayersNewPredictions)
        for (player in currentSquadNewPredictions!!.startingPlayers) {
            if (expectedLineups[player.commonName] == "OUT" || expectedLineups[player.commonName] == "SUSPENDED") {
                player.predictedPointsThisGW = 0.0
            }
        }
        for (player in currentSquadNewPredictions!!.benchPlayers) {
            if (expectedLineups[player.commonName] == "OUT" || expectedLineups[player.commonName] == "SUSPENDED") {
                player.predictedPointsThisGW = 0.0
            }
        }

        val optimumSquad = SquadSelectorOptimiser(playerPredictions = playerToPrediction, expectedLineups = expectedLineups).optimizeSquadForGameweek(currentSquad = currentSquadNewPredictions, availableFreeTransfers = 2)
        println(optimumSquad)

        val timestamp = Clock.System.now()
        val gameweek = "2024-$nextEventKey"
        // store squad
        //teamOfTheWeekPredictionStore.store(gameweek, optimumSquad.squad, timestamp)
    }
}

suspend fun main() {
    ModelPredictor(FetchRawCurrentFPLData(), TeamOfTheWeekPredictionRepository()).allPredictionsForUpcomingGameweek()
}

val sanitiseNames = mapOf("Bruno G." to "Guimaraes")

fun String.removeAccents(): String {
    val normalizedString = Normalizer.normalize(this, Normalizer.Form.NFD)
    val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
    return pattern.matcher(normalizedString).replaceAll("")
}

data class Point(val x: Double, val y: Double)
