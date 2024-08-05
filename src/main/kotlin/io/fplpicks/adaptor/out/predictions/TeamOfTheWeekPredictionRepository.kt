package io.fplpicks.adaptor.out.predictions

import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.PutItemRequest
import io.fplpicks.application.model.PlayerPrediction
import io.fplpicks.application.port.out.PredictionStore
import io.fplpicks.application.service.model.predict.Squad

class TeamOfTheWeekPredictionRepository: PredictionStore {
    override suspend fun store(gameweek: String, squad: Squad) {

        val item = mapOf(
            "gameweek" to AttributeValue.S(gameweek),
            "startingPlayers" to AttributeValue.L(squad.startingPlayers.map { it.toAttributeValue() }),
            "benchPlayers" to AttributeValue.L(squad.benchPlayers.map { it.toAttributeValue() }),
            "formation" to AttributeValue.S(squad.formation.toString()), // Assuming Formation is an enum or has a meaningful toString()
            "totalCost" to AttributeValue.N(squad.totalCost.toString()),
            "weightedTotalPredictedPoints" to AttributeValue.N(squad.weightedTotalPredictedPoints.toString())
        )

        val request = PutItemRequest {
            this.tableName = "fpl-totw"
            this.item = item
        }

        DynamoDbClient { region = "eu-west-2" }.use { ddb ->
            ddb.putItem(request)
        }
    }

    private fun PlayerPrediction.toAttributeValue(): AttributeValue {
        return AttributeValue.M(
            mapOf(
                "name" to AttributeValue.S(name),
                "position" to AttributeValue.S(position),
                "value" to AttributeValue.N(value.toString()),
                "predictedPointsThisGW" to AttributeValue.N(predictedPointsThisGW.toString()),
                "predictedPointsGWPlus1" to AttributeValue.N(predictedPointsGWPlus1.toString()),
                "predictedPointsGWPlus2" to AttributeValue.N(predictedPointsGWPlus2.toString()),
                "predictedPointsGWPlus3" to AttributeValue.N(predictedPointsGWPlus3.toString()),
                "predictedPointsGWPlus4" to AttributeValue.N(predictedPointsGWPlus4.toString()),
                "pointsPerValue" to AttributeValue.N(pointsPerValue.toString()),
                "pointsTotalUpcomingGWs" to AttributeValue.N(pointsTotalUpcomingGWs.toString()),
                "pointsAvgUpcomingGWs" to AttributeValue.N(pointsAvgUpcomingGWs.toString()),
                "team" to AttributeValue.S(team)
            )
        )
    }
}