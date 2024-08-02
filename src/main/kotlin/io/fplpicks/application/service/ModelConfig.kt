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
            "opponentStrength",
            "lastGamePoints",
            "lastOpponentStrength",
            "last2GamesPointsAvg",
            "last3GamesPointsAvg",
            "last3GamesOpponentStrengthAvg",
            "last5GamesPointsAvg",
            "last5GamesOpponentStrengthAvg",
            "seasonAvgPointsToDate",
            "seasonAvgMinutesPlayed",
            "seasonPointsStdDev",
            "isHome"
        )

        fun featureValues(features: PlayerGameweekFeatures): DoubleArray = doubleArrayOf(
            features.position.toDouble(),
            features.opponentStrength.toDouble(),
            features.lastGamePoints.toDouble(),
            features.lastOpponentStrength.toDouble(),
            features.last2GamesPointsAvg,
            features.last3GamesPointsAvg,
            features.last3GamesOpponentStrengthAvg,
            features.last5GamesPointsAvg,
            features.last5GamesOpponentStrengthAvg,
            features.seasonAvgPointsToDate,
            features.seasonAvgMinutesPlayed,
            features.seasonPointsStdDev,
            if (features.isHome) 1.0 else 0.0,
            features.points
        )

        fun schema(): StructType = StructType(
            StructField("position", DataTypes.DoubleType),
            StructField("opponentStrength", DataTypes.DoubleType),
            StructField("lastGamePoints", DataTypes.DoubleType),
            StructField("lastOpponentStrength", DataTypes.DoubleType),
            StructField("last2GamesPointsAvg", DataTypes.DoubleType),
            StructField("last3GamesPointsAvg", DataTypes.DoubleType),
            StructField("last3GamesOpponentStrengthAvg", DataTypes.DoubleType),
            StructField("last5GamesPointsAvg", DataTypes.DoubleType),
            StructField("last5GamesOpponentStrengthAvg", DataTypes.DoubleType),
            StructField("seasonAvgPointsToDate", DataTypes.DoubleType),
            StructField("seasonAvgMinutesPlayed", DataTypes.DoubleType),
            StructField("seasonPointsStdDev", DataTypes.DoubleType),
            StructField("isHome", DataTypes.DoubleType),
            StructField("points", DataTypes.DoubleType)
        )
    }
}