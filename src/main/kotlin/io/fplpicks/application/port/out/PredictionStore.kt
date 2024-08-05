package io.fplpicks.application.port.out

import io.fplpicks.application.service.model.predict.Squad

interface PredictionStore {
    suspend fun store(gameweek: String, squad: Squad)
}