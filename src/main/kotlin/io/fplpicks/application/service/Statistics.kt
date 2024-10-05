package io.fplpicks.application.service

import io.fplpicks.application.model.PlayerGameweekData

class Statistics {
    companion object {
        fun calculateExponentialMovingAverage(data: List<PlayerGameweekData>?, alpha: Double = 0.3): Double {
            if (data?.isEmpty() == true) {
                return 0.0
            }
            val points = data!!.map { it.xP }
            var ema = points.first().toDouble()
            for (point in points.drop(1)) {
                ema = alpha * point + (1 - alpha) * ema
            }
            return ema
        }
    }
}