package io.fplpicks.application.service.model.predict

import io.fplpicks.application.model.PlayerPrediction
import kotlin.random.Random

class SquadSelectorOptimiser(private val playerPredictions: List<PlayerPrediction>,
                             private val populationSize: Int = 1000,
                             private val budget: Double = 1000.0,
                             private val generations: Int = 100) {

    private val random = Random.Default
    private val possibleFormations = listOf(
        Formation(3, 5, 2),
        Formation(3, 4, 3),
        Formation(4, 4, 2),
        Formation(4, 3, 3),
        Formation(5, 4, 1),
        Formation(5, 3, 2)
    )

    fun selectBestSquad(): Squad {
        var population = initialisePopulation()

        repeat(generations) {
            population = evolvePopulation(population)
        }

        return population.maxBy { it.weightedTotalPredictedPoints }
    }

    private fun initialisePopulation(): List<Squad> {
        return (1..populationSize).map { createRandomSquad() }
    }

    private fun createRandomSquad(): Squad {
        var squad: Squad
        do {
            val formation = possibleFormations.random(random)
            val players = mutableListOf<PlayerPrediction>()
            val positionCounts = mutableMapOf(
                "GKP" to 2,
                "DEF" to 5,
                "MID" to 5,
                "FWD" to 3
            )

            while (players.size < 15) {
                val player = playerPredictions.random(random)
                if (positionCounts[player.position]!! > 0 && !players.contains(player)) {
                    players.add(player)
                    positionCounts[player.position] = positionCounts[player.position]!! - 1
                }
            }

            val (startingPlayers, benchPlayers) = assignPlayersToPositions(players, formation)
            squad = createSquad(startingPlayers, benchPlayers, formation)
        } while (!squad.isValid)
        return squad
    }

    private fun assignPlayersToPositions(
        players: List<PlayerPrediction>,
        formation: Formation
    ): Pair<List<PlayerPrediction>, List<PlayerPrediction>> {
        val startingPlayers = mutableListOf<PlayerPrediction>()
        val benchPlayers = players.toMutableList()

        // Assign GK
        startingPlayers.add(benchPlayers.first { it.position == "GKP" })
        benchPlayers.removeAll { it.position == "GKP" && it in startingPlayers }

        // Assign outfield players
        listOf(
            "DEF" to formation.defenders,
            "MID" to formation.midfielders,
            "FWD" to formation.forwards
        ).forEach { (position, count) ->
            val positionPlayers = benchPlayers.filter { it.position == position }
                .sortedByDescending { it.pointsTotalUpcomingGWs }
                .take(count)
            startingPlayers.addAll(positionPlayers)
            benchPlayers.addAll(positionPlayers)
        }
        return startingPlayers to benchPlayers
    }

    private fun createSquad(
        startingPlayers: List<PlayerPrediction>,
        benchPlayers: List<PlayerPrediction>,
        formation: Formation
    ): Squad {
        val totalCost = startingPlayers.sumOf { it.value } + benchPlayers.sumOf { it.value }
        val weightedTotalPredictedPoints = calculateWeightedTotalPredictedPoints(startingPlayers, benchPlayers)
        return Squad(startingPlayers, benchPlayers, formation, totalCost, weightedTotalPredictedPoints)
    }

    private fun calculateWeightedTotalPredictedPoints(
        startingPlayers: List<PlayerPrediction>,
        benchPlayers: List<PlayerPrediction>
    ): Double {
        return startingPlayers.sumOf { it.pointsTotalUpcomingGWs } + benchPlayers.sumOf { it.pointsTotalUpcomingGWs * Squad.BENCH_WEIGHT }
    }

    private fun evolvePopulation(population: List<Squad>): List<Squad> {
        val newPopulation = mutableListOf<Squad>()
        val eliteSize = populationSize / 10

        // Elitism
        newPopulation.addAll(population.sortedByDescending { it.weightedTotalPredictedPoints }.take(eliteSize))

        while (newPopulation.size < populationSize) {
            val parent1 = selectParent(population)
            val parent2 = selectParent(population)
            val child = crossover(parent1, parent2)
            val mutatedChild = mutate(child)
            newPopulation.add(mutatedChild)
        }
        return newPopulation
    }

    private fun selectParent(population: List<Squad>): Squad {
        val tournamentSize = 5
        return (1..tournamentSize)
            .map { population.random(random) }
            .filter { it.isValid }
            .maxByOrNull { it.weightedTotalPredictedPoints }
            ?: createRandomSquad() // Fallback to creating a new valid squad if no valid ones found
    }

    private fun crossover(parent1: Squad, parent2: Squad): Squad {
        var child: Squad
        do {
            val formation = if (random.nextBoolean()) parent1.formation else parent2.formation
            val allPlayers = (parent1.startingPlayers + parent1.benchPlayers +
                    parent2.startingPlayers + parent2.benchPlayers).distinct()

            val (startingPlayers, benchPlayers) = assignPlayersToPositions(allPlayers.shuffled(random).take(15), formation)
            child = createSquad(startingPlayers, benchPlayers, formation)
        } while (!child.isValid)
        return child
    }

    private fun mutate(squad: Squad): Squad {
        if (random.nextDouble() > 0.1) return squad // 10% chance of mutation

        var mutatedSquad: Squad
        do {
            val allSquadPlayers = squad.startingPlayers + squad.benchPlayers
            val positionToMutate = listOf("GKP", "DEF", "MID", "FWD").random(random)
            val playerToReplace = allSquadPlayers.filter { it.position == positionToMutate }.random(random)
            val newPlayer = playerPredictions.filter { it.position == positionToMutate && it !in allSquadPlayers }.random(random)

            val newPlayers = allSquadPlayers.map { if (it == playerToReplace) newPlayer else it }
            val (startingPlayers, benchPlayers) = assignPlayersToPositions(newPlayers, squad.formation)

            mutatedSquad = createSquad(startingPlayers, benchPlayers, squad.formation)
        } while (!mutatedSquad.isValid)
        return mutatedSquad
    }
}

data class Squad(
    val startingPlayers: List<PlayerPrediction>,
    val benchPlayers: List<PlayerPrediction>,
    val formation: Formation,
    val totalCost: Double,
    val weightedTotalPredictedPoints: Double
) {
    val isValid: Boolean
        get() = startingPlayers.size == 11 &&
                benchPlayers.size == 4 &&
                startingPlayers.count { it.position == "GKP" } == 1 &&
                startingPlayers.count { it.position == "DEF" } == formation.defenders &&
                startingPlayers.count { it.position == "MID" } == formation.midfielders &&
                startingPlayers.count { it.position == "FWD" } == formation.forwards &&
                startingPlayers.count { it.position == "GKP" } + benchPlayers.count { it.position == "GKP" } == 2 &&
                startingPlayers.count { it.position == "DEF" } + benchPlayers.count { it.position == "DEF" } == 5 &&
                startingPlayers.count { it.position == "MID" } + benchPlayers.count { it.position == "MID" } == 5 &&
                startingPlayers.count { it.position == "FWD" } + benchPlayers.count { it.position == "FWD" } == 3 &&
                totalCost <= 1000

    companion object {
        const val BENCH_WEIGHT = 1.0 / 9.0
    }
}

data class Formation(
    val defenders: Int,
    val midfielders: Int,
    val forwards: Int
)
