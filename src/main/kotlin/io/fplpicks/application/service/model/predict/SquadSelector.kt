package io.fplpicks.application.service.model.predict

import io.fplpicks.application.model.PlayerPrediction
import kotlin.random.Random

class SquadSelector(
    private val playerPredictions: List<PlayerPrediction>,
    private val budget: Double = 1000.0) {

    private val starting11Budget = 820

    private val formations = listOf(
        mapOf("GKP" to 1, "DEF" to 3, "MID" to 5, "FWD" to 2),
        mapOf("GKP" to 1, "DEF" to 3, "MID" to 4, "FWD" to 3),
        mapOf("GKP" to 1, "DEF" to 4, "MID" to 4, "FWD" to 2),
        mapOf("GKP" to 1, "DEF" to 4, "MID" to 3, "FWD" to 3),
        mapOf("GKP" to 1, "DEF" to 5, "MID" to 4, "FWD" to 1),
        mapOf("GKP" to 1, "DEF" to 5, "MID" to 3, "FWD" to 2)
    )
    private val squadConstraints = mapOf("GKP" to 2, "DEF" to 5, "MID" to 5, "FWD" to 3)
    private val maxPlayersPerTeam = 3

    fun selectOptimalTeam(): Pair<List<PlayerPrediction>, List<PlayerPrediction>> {
        var bestStarting11 = emptyList<PlayerPrediction>()
        var bestBench = emptyList<PlayerPrediction>()
        var bestScore = Double.NEGATIVE_INFINITY

        formations.forEach { formation ->
            val (starting11, bench) = selectTeamForFormation(formation)
            val score = calculateTeamScore(starting11, bench)
            if (score > bestScore) {
                bestStarting11 = starting11
                bestBench = bench
                bestScore = score
            }
        }

        return Pair(bestStarting11, bestBench)
    }

    private fun selectTeamForFormation(formation: Map<String, Int>): Pair<List<PlayerPrediction>, List<PlayerPrediction>> {
        var starting11 = greedySelectionStarting11(formation)
        var bench = greedySelectionBench(starting11)
        var score = calculateTeamScore(starting11, bench)

        // Local search optimization
        repeat(1000) { // Number of iterations
            val (newStarting11, newBench) = localSearch(starting11, bench, formation)
            val newScore = calculateTeamScore(newStarting11, newBench)
            if (newScore > score) {
                starting11 = newStarting11
                bench = newBench
                score = newScore
            }
        }

        return Pair(starting11, bench)
    }

    private fun greedySelectionStarting11(formation: Map<String, Int>): List<PlayerPrediction> {
        val sortedPlayers = playerPredictions.groupBy { it.position }
            .mapValues { it.value.sortedByDescending { player -> player.pointsTotalUpcomingGWs } }

        val selectedTeam = mutableListOf<PlayerPrediction>()

        formation.forEach { (pos, count) ->
            // This no longer works as its picking each position and comparing against the entire starting 11 budget!
            // need to have a think about this
            val eligiblePlayers = sortedPlayers[pos]!!.filter { player ->
                selectedTeam.count { it.team == player.team } < maxPlayersPerTeam &&
                        selectedTeam.sumOf { it.value } + player.value <= starting11Budget
            }
            selectedTeam.addAll(eligiblePlayers.take(count))
        }

            return selectedTeam
    }

    private fun greedySelectionBench(starting11: List<PlayerPrediction>): List<PlayerPrediction> {
        val remainingBudget = budget - starting11.sumOf { it.value }
        val sortedPlayers = playerPredictions.groupBy { it.position }
            .mapValues { it.value.sortedByDescending { player -> player.pointsTotalUpcomingGWs / player.value } }

        val benchPlayers = mutableListOf<PlayerPrediction>()

        squadConstraints.forEach { (pos, totalCount) ->
            val countNeeded = totalCount - starting11.count { it.position == pos }
            val eligiblePlayers = sortedPlayers[pos]!!.filter { player ->
                player !in starting11 &&
                        (starting11 + benchPlayers).count { it.team == player.team } < maxPlayersPerTeam &&
                        benchPlayers.sumOf { it.value } + player.value <= remainingBudget &&
                        player.value <= 45.0 // Assuming 5.0 is a reasonable threshold for low-cost players
            }
            benchPlayers.addAll(eligiblePlayers.take(countNeeded))
        }

        return benchPlayers
    }

    private fun localSearch(currentStarting11: List<PlayerPrediction>, currentBench: List<PlayerPrediction>, formation: Map<String, Int>): Pair<List<PlayerPrediction>, List<PlayerPrediction>> {
        val newStarting11 = currentStarting11.toMutableList()
        val newBench = currentBench.toMutableList()

        // Randomly decide to modify starting 11 or bench
        if (Random.nextBoolean()) {
            // Logic to improve starting 11
            val position = formation.keys.random()
            val playerToRemove = newStarting11.filter { it.position == position }.random()
            newStarting11.remove(playerToRemove)

            val possibleReplacements = playerPredictions.filter {
                it.position == position &&
                        it !in newStarting11 && it !in newBench &&
                        (newStarting11 + newBench).count { p -> p.team == it.team } < maxPlayersPerTeam &&
                        newStarting11.sumOf { p -> p.value } - playerToRemove.value + it.value <= starting11Budget &&
                        it.pointsTotalUpcomingGWs > playerToRemove.pointsTotalUpcomingGWs
            }

            if (possibleReplacements.isNotEmpty()) {
                val replacement = possibleReplacements.maxByOrNull { it.pointsTotalUpcomingGWs }!!
                newStarting11.add(replacement)
                if (playerToRemove.value <= 45.0) {
                    // If removed player is low-cost, consider for bench
                    val benchPlayerToReplace = newBench.filter { it.position == position }.minByOrNull { it.pointsTotalUpcomingGWs }
                    if (benchPlayerToReplace != null && playerToRemove.pointsTotalUpcomingGWs > benchPlayerToReplace.pointsTotalUpcomingGWs) {
                        newBench.remove(benchPlayerToReplace)
                        newBench.add(playerToRemove)
                    }
                }
            } else {
                newStarting11.add(playerToRemove)
            }
        } else {
            // Logic to improve bench
            val benchPositions = newBench.map { it.position }.distinct()
            if (benchPositions.isNotEmpty()) {
                val position = benchPositions.random()
                val playerToRemove = newBench.filter { it.position == position }.random()
                newBench.remove(playerToRemove)

                val totalPositionCount = squadConstraints[position]!!
                val currentPositionCount =
                    newStarting11.count { it.position == position } + newBench.count { it.position == position } + 1 // +1 because we just removed a player

                val possibleReplacements = playerPredictions.filter {
                    it.position == position &&
                            it !in newStarting11 && it !in newBench &&
                            (newStarting11 + newBench).count { p -> p.team == it.team } < maxPlayersPerTeam &&
                            newBench.sumOf { p -> p.value } - playerToRemove.value + it.value <= (budget - newStarting11.sumOf { it.value }) &&
                            it.value <= 45.0 && // Keep bench players low-cost
                            it.pointsTotalUpcomingGWs / it.value > playerToRemove.pointsTotalUpcomingGWs / playerToRemove.value && // Better value
                            currentPositionCount <= totalPositionCount // Ensure we don't exceed the total required for this position
                }

                if (possibleReplacements.isNotEmpty()) {
                    val replacement = possibleReplacements.maxByOrNull { it.pointsTotalUpcomingGWs / it.value }!!
                    newBench.add(replacement)
                } else {
                    newBench.add(playerToRemove)
                }
            }
        }

        return Pair(newStarting11, newBench)
    }

    private fun calculateTeamScore(starting11: List<PlayerPrediction>, bench: List<PlayerPrediction>): Double {
        // Prioritize starting 11 points, but also consider potential bench contributions
        return starting11.sumOf { it.pointsTotalUpcomingGWs } + (bench.sumOf { it.pointsTotalUpcomingGWs } * 0.1)
    }

}