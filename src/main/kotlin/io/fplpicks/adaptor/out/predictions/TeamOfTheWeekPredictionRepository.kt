package io.fplpicks.adaptor.out.predictions

import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.model.*
import io.fplpicks.application.model.PlayerPrediction
import io.fplpicks.application.port.out.PredictionStore
import io.fplpicks.application.service.model.predict.Formation
import io.fplpicks.application.service.model.predict.Squad
import kotlinx.datetime.Instant

class TeamOfTheWeekPredictionRepository: PredictionStore {
    override suspend fun store(gameweek: String, squad: Squad, timestamp: Instant) {

        val item = mapOf(
            "gameweek" to AttributeValue.S(gameweek),
            "timestamp" to AttributeValue.N(timestamp.toEpochMilliseconds().toString()),
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
                "commonName" to AttributeValue.S(commonName),
                "position" to AttributeValue.S(position),
                "image" to AttributeValue.S(image),
                "team" to AttributeValue.S(team),
                "value" to AttributeValue.N(value.toString()),
                "predictedPointsThisGW" to AttributeValue.N(predictedPointsThisGW.toString()),
                "predictedPointsGWPlus1" to AttributeValue.N(predictedPointsGWPlus1.toString()),
                "predictedPointsGWPlus2" to AttributeValue.N(predictedPointsGWPlus2.toString()),
                "predictedPointsGWPlus3" to AttributeValue.N(predictedPointsGWPlus3.toString()),
                "predictedPointsGWPlus4" to AttributeValue.N(predictedPointsGWPlus4.toString()),
                "pointsPerValue" to AttributeValue.N(pointsPerValue.toString()),
                "pointsTotalUpcomingGWs" to AttributeValue.N(pointsTotalUpcomingGWs.toString()),
                "pointsAvgUpcomingGWs" to AttributeValue.N(pointsAvgUpcomingGWs.toString()),
            )
        )
    }

    override suspend fun retrieve(gameweek: String): Squad? {
        val request = QueryRequest {
            tableName = "fpl-totw"
            keyConditionExpression = "#gw = :gameweek"
            expressionAttributeNames = mapOf("#gw" to "gameweek")
            expressionAttributeValues = mapOf(":gameweek" to AttributeValue.S(gameweek))
            scanIndexForward = false
        }

        val response = DynamoDbClient { region = "eu-west-2" }.use { ddb ->
            ddb.query(request).items?.get(0)
        }

        return response?.let { item ->
            Squad(
                startingPlayers = (item["startingPlayers"] as AttributeValue.L).value.map { it.toPlayerPrediction() },
                benchPlayers = (item["benchPlayers"] as AttributeValue.L).value.map { it.toPlayerPrediction() },
                formation = parseFormation(item["formation"] as AttributeValue.S),
                totalCost = (item["totalCost"] as AttributeValue.N).value.toDouble(),
                weightedTotalPredictedPoints = (item["weightedTotalPredictedPoints"] as AttributeValue.N).value.toDouble(),
                transfersMade = (item["transfersMade"] as? AttributeValue.N)?.value?.toInt() ?: 0,
                availableFreeTransfers = (item["availableFreeTransfers"] as? AttributeValue.N)?.value?.toInt() ?: 1
            )
        }
    }

    private fun AttributeValue.toPlayerPrediction(): PlayerPrediction {
        val map = (this as AttributeValue.M).value
        return PlayerPrediction(
            name = (map["name"] as AttributeValue.S).value,
            commonName = (map["commonName"] as AttributeValue.S).value,
            position = (map["position"] as AttributeValue.S).value,
            image = (map["image"] as AttributeValue.S).value,
            team = (map["team"] as AttributeValue.S).value,
            value = (map["value"] as AttributeValue.N).value.toDouble(),
            predictedPointsThisGW = (map["predictedPointsThisGW"] as AttributeValue.N).value.toDouble(),
            predictedPointsGWPlus1 = (map["predictedPointsGWPlus1"] as AttributeValue.N).value.toDouble(),
            predictedPointsGWPlus2 = (map["predictedPointsGWPlus2"] as AttributeValue.N).value.toDouble(),
            predictedPointsGWPlus3 = (map["predictedPointsGWPlus3"] as AttributeValue.N).value.toDouble(),
            predictedPointsGWPlus4 = (map["predictedPointsGWPlus4"] as AttributeValue.N).value.toDouble(),
            pointsPerValue = (map["pointsPerValue"] as AttributeValue.N).value.toDouble(),
            pointsTotalUpcomingGWs = (map["pointsTotalUpcomingGWs"] as AttributeValue.N).value.toDouble(),
            pointsAvgUpcomingGWs = (map["pointsAvgUpcomingGWs"] as AttributeValue.N).value.toDouble()
        )
    }

    private fun parseFormation(formationString: AttributeValue.S): Formation {
        val (defenders, midfielders, forwards) = formationString.value
            .replace("Formation(", "")
            .replace(")", "")
            .split(", ")
            .map { it.split("=")[1].toInt() }
        return Formation(defenders, midfielders, forwards)
    }
}