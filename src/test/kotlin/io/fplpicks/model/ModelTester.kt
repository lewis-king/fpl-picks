package io.fplpicks.model

import io.fplpicks.application.model.PlayerGameweekFeatures
import io.fplpicks.application.service.ModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import smile.data.Tuple
import smile.regression.RandomForest
import java.io.FileInputStream
import java.io.ObjectInputStream
import kotlin.test.Test

class ModelTester {
    val model: RandomForest =
        FileInputStream("build/model/model.ser").use { fis ->
            ObjectInputStream(fis).use { ois ->
                ois.readObject() as RandomForest
            }
        }

    /*
    position = 1
isHome = false
opponentStrength = 4
lastOpponentStrength = 3
last3GamesOpponentStrengthAvg = 3.3333333333333335
last5GamesOpponentStrengthAvg = 3.0
last5GamesCleansSheetsAvg = 0.4
last5GamesGoalsScoredAvg = 0.0
last5GamesAssistsAvg = 0.0
last5GamesBonusPointsAvg = 0.0
lastGamePoints = 1
last2GamesPointsAvg = 1.0
last3GamesPointsAvg = 3.3333333333333335
last5GamesPointsAvg = 4.6
seasonAvgPointsToDate = 3.948717948717949
seasonPointsStdDev = 3.265583687165737
seasonAvgCleanSheets = 0.3333333333333333
seasonAvgGoalsScored = 0.0
seasonAvgAssists = 0.0
seasonAvgBonusPointsAvg = 0.0
seasonAvgMinutesPlayed = 90.0
points = 0.0
     */

    @Test
    fun testModel() {
        /*val features = PlayerGameweekFeatures(
                    position = 1,
                    isHome = false,
                    opponentStrength = 4,
                    lastOpponentStrength = 3,
                    last3GamesOpponentStrengthAvg = 3.3333333333333335,
                    last5GamesOpponentStrengthAvg = 3.0,
                    last5GamesCleansSheetsAvg = 0.4,
                    last5GamesGoalsScoredAvg = 0.0,
                    last5GamesAssistsAvg = 0.0,
                    last5GamesBonusPointsAvg = 0.0,
                    lastGamePoints = 1,
                    last2GamesPointsAvg = 1.0,
                    last3GamesPointsAvg = 3.3333333333333335,
                    last5GamesPointsAvg = 4.6,
                    seasonAvgPointsToDate = 3.948717948717949,
                    seasonPointsStdDev = 3.265583687165737,
                    seasonAvgCleanSheets = 0.3333333333333333,
                    seasonAvgGoalsScored = 0.0,
                    seasonAvgAssists = 0.0,
                    seasonAvgBonusPointsAvg = 0.0,
                    seasonAvgMinutesPlayed = 90.0,
                    points = 0.0
        )
        val modelRequest = Tuple.of(
            ModelConfig.featureValues(features),
            ModelConfig.schema()
        )
        val pointsPredicted = model.predict(modelRequest)
        println(pointsPredicted)*/
    }
}