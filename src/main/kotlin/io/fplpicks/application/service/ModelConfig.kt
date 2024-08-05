package io.fplpicks.application.service

import io.fplpicks.application.model.PlayerGameweekFeatures
import smile.data.Tuple
import smile.data.type.DataTypes
import smile.data.type.StructField
import smile.data.type.StructType

class ModelConfig {
    companion object {
        fun featureNames(): Array<String> = arrayOf(
            "position",
            "isHome",
            "opponentStrength",
            "lastOpponentStrength",
            //"last3GamesOpponentStrengthAvg",
            //"last5GamesOpponentStrengthAvg",
            "lastGamePoints",
            "last2GamesPointsAvg",
            "last3GamesPointsAvg",
            "last5GamesPointsAvg",
            "seasonAvgPointsToDate",
            "seasonAvgMinutesPlayed",
            "seasonPointsStdDev"
        )

        fun featureValues(features: PlayerGameweekFeatures): DoubleArray = doubleArrayOf(
            features.position.toDouble(),
            if (features.isHome) 1.0 else 0.0,
            features.opponentStrength.toDouble(),
            features.lastOpponentStrength.toDouble(),
            //features.last3GamesOpponentStrengthAvg,
            //features.last5GamesOpponentStrengthAvg,
            features.lastGamePoints.toDouble(),
            features.last2GamesPointsAvg,
            features.last3GamesPointsAvg,
            features.last5GamesPointsAvg,
            features.seasonAvgPointsToDate,
            features.seasonAvgMinutesPlayed,
            features.seasonPointsStdDev,
            features.points
        )

        fun schema(): StructType = StructType(
            StructField("position", DataTypes.DoubleType),
            StructField("isHome", DataTypes.DoubleType),
            StructField("opponentStrength", DataTypes.DoubleType),
            StructField("lastOpponentStrength", DataTypes.DoubleType),
            //StructField("last3GamesOpponentStrengthAvg", DataTypes.DoubleType),
            //StructField("last5GamesOpponentStrengthAvg", DataTypes.DoubleType),
            StructField("lastGamePoints", DataTypes.DoubleType),
            StructField("last2GamesPointsAvg", DataTypes.DoubleType),
            StructField("last3GamesPointsAvg", DataTypes.DoubleType),
            StructField("last5GamesPointsAvg", DataTypes.DoubleType),
            StructField("seasonAvgPointsToDate", DataTypes.DoubleType),
            StructField("seasonAvgMinutesPlayed", DataTypes.DoubleType),
            StructField("seasonPointsStdDev", DataTypes.DoubleType),
            StructField("points", DataTypes.DoubleType)
        )
    }
}