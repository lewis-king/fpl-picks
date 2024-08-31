package io.fplpicks.application.port.out

import io.fplpicks.application.service.model.predict.Squad
import kotlinx.datetime.Instant

interface PredictionStore {
    suspend fun store(gameweek: String, squad: Squad, timestamp: Instant)
    suspend fun retrieve(gameweek: String): Squad?
}