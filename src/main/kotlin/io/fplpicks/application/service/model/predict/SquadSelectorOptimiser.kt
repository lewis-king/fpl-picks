package io.fplpicks.application.service.model.predict

import io.fplpicks.application.model.PlayerPrediction
import io.fplpicks.application.model.calculateWeightedScore
import java.io.InputStreamReader
import kotlin.random.Random

val playerBlocklist = {}.javaClass.getResourceAsStream("/player_selection_blocklist.txt")
    ?.let { InputStreamReader(it) }?.readLines()?.associateBy { it }

class SquadSelectorOptimiser(
    private val playerPredictions: List<PlayerPrediction>,
    private val populationSize: Int = 1000,
    private val budget: Double = 1000.0,
    private val generations: Int = 100,
    private val expectedLineups: Map<String, String>,
    private val lookAheadWeeks: Int = 3
) {


    private val MONEY_IN_BANK = 0.0
    private val random = Random.Default

    private val maxFreeTransfers: Int = 5
    private val freeTransferValue: Double

    init {
        freeTransferValue = 0.0 //calculateFreeTransferValue()
    }

    private fun calculateFreeTransferValue(): Double {
        val positionGroups = playerPredictions.groupBy { it.position }
        val avgDifference = positionGroups.map { (_, players) ->
            val sortedPlayers = players.sortedByDescending { it.calculateWeightedScore() }
            val topPlayersAvg = sortedPlayers.take((players.size * 0.2).toInt()).map { it.pointsTotalUpcomingGWs }.average()
            val overallAvg = players.map { it.calculateWeightedScore() }.average()
            topPlayersAvg - overallAvg
        }.average()

        return (avgDifference * 0.5) / 2  // Adjust this factor as needed
    }

    fun optimizeSquadForGameweek(
        currentSquad: Squad?,
        availableFreeTransfers: Int = 1
    ): OptimizationResult {
        return if (currentSquad == null) {
            val bestSquad = selectBestSquad()
            OptimizationResult(
                squad = bestSquad,
                transferStrategy = TransferStrategy(bestSquad, emptyList()),
                reasoning = "Initial squad selection"
            )
        } else {
            findOptimalTransferStrategy(currentSquad, availableFreeTransfers, lookAheadWeeks)
        }
    }

    private fun findOptimalTransferStrategy(initialSquad: Squad, initialFreeTransfers: Int, weeksToLookAhead: Int): OptimizationResult {
        var bestStrategy = TransferStrategy(initialSquad, emptyList())
        var bestScore = calculateMultiWeekScore(bestStrategy, weeksToLookAhead)
        var bestReasoning = "No transfers made"

        val possibleStrategies = generatePossibleStrategies(initialSquad, initialFreeTransfers, weeksToLookAhead)

        for (strategy in possibleStrategies) {
            val score = calculateMultiWeekScore(strategy, weeksToLookAhead)
            if (score > bestScore) {
                bestStrategy = strategy
                bestScore = score
                bestReasoning = generateReasoning(strategy, weeksToLookAhead)
            }
        }

        val optimalSquad = applyTransfers(initialSquad, bestStrategy.transfers.firstOrNull() ?: emptyList())
        val sortedOptimalSquad = optimizeSquad(optimalSquad)
        return OptimizationResult(sortedOptimalSquad, bestStrategy, bestReasoning)
    }

    private fun generateReasoning(strategy: TransferStrategy, weeksToLookAhead: Int): String {
        val totalTransfers = strategy.transfers.sumOf { it.size }
        if (totalTransfers == 0) {
            return "No transfers were made as the current squad is optimal for the next $weeksToLookAhead weeks."
        }

        val weeklyTransfers = strategy.transfers.mapIndexed { index, transfers ->
            if (transfers.isNotEmpty()) "Week ${index + 1}: ${transfers.size} transfer(s)" else null
        }.filterNotNull()

        return "Optimal strategy: " + weeklyTransfers.joinToString(", ") +
                ". This strategy maximizes expected points over the next $weeksToLookAhead weeks."
    }

    private fun generatePossibleStrategies(initialSquad: Squad, initialFreeTransfers: Int, weeksToLookAhead: Int): List<TransferStrategy> {
        val strategies = mutableListOf<TransferStrategy>()

        fun generateStrategiesRecursive(currentWeek: Int, accumulatedTransfers: Int, currentStrategy: List<List<Transfer>>) {
            if (currentWeek == weeksToLookAhead) {
                strategies.add(TransferStrategy(initialSquad, currentStrategy))
                return
            }

            val maxTransfersThisWeek = minOf(accumulatedTransfers, 5)

            for (numTransfers in 0..maxTransfersThisWeek) {
                val transfers = generateTransfers(initialSquad, numTransfers)
                val newAccumulatedTransfers = if (numTransfers == 0) minOf(accumulatedTransfers + 1, 5) else 1
                generateStrategiesRecursive(currentWeek + 1, newAccumulatedTransfers, currentStrategy + listOf(transfers))
            }
        }

        generateStrategiesRecursive(0, initialFreeTransfers, emptyList())

        return strategies
    }

    private fun generateTransfers(squad: Squad, numTransfers: Int): List<Transfer> {
        val allPlayers = squad.startingPlayers + squad.benchPlayers
        val playersToTransferOut = allPlayers
            .filter { expectedLineups[it.commonName] != "STARTING" }
            .sortedBy { it.calculateWeightedScore() }
            .take(numTransfers)

        val transfers = mutableListOf<Transfer>()
        var currentSquad = squad
        var remainingBudget = MONEY_IN_BANK + playersToTransferOut.sumOf { it.value }

        for (playerOut in playersToTransferOut) {
            val possibleReplacements = findPossibleReplacements(playerOut, currentSquad, remainingBudget)

            if (possibleReplacements.isNotEmpty()) {
                val playerIn = possibleReplacements.first() // Already sorted by weighted score
                transfers.add(Transfer(playerOut, playerIn))

                // Update the current squad and remaining budget
                currentSquad = makeTransfer(currentSquad, playerOut, playerIn)
                remainingBudget -= playerIn.value
            }
        }

        return transfers
    }

    private fun applyTransfers(squad: Squad, transfers: List<Transfer>): Squad {
        var currentSquad = squad
        for (transfer in transfers) {
            currentSquad = makeTransfer(currentSquad, transfer.playerOut, transfer.playerIn)
        }
        return currentSquad
    }

    private fun makeTransfer(squad: Squad, oldPlayer: PlayerPrediction, newPlayer: PlayerPrediction): Squad {
        val newPlayers = (squad.startingPlayers + squad.benchPlayers).map {
            if (it == oldPlayer) newPlayer else it
        }
        val (newStarting, newBench) = assignPlayersToPositions(newPlayers, squad.formation)
        val newSquad = createSquad(newStarting, newBench, squad.formation)

        return if (newSquad.isValid) newSquad else squad
    }

    private fun calculateSquadScore(squad: Squad): Double {
        // This is a simple scoring function. You might want to make this more sophisticated.
        return squad.weightedTotalPredictedPoints
    }

    private fun findPossibleReplacements(
        player: PlayerPrediction,
        squad: Squad,
        remainingBudget: Double
    ): List<PlayerPrediction> {
        return playerPredictions.filter { replacement ->
            replacement.position == player.position &&
                    replacement !in (squad.startingPlayers + squad.benchPlayers) &&
                    replacement.value <= remainingBudget &&
                    playerBlocklist?.get(replacement.name) == null &&
                    expectedLineups[replacement.commonName] == "STARTING"
        }.sortedByDescending { it.calculateWeightedScore() }
    }

    private fun calculateMultiWeekScore(strategy: TransferStrategy, weeksToLookAhead: Int): Double {
        var totalScore = 0.0
        var currentSquad = strategy.initialSquad
        var unusedTransfers = 0

        for (week in 0 until weeksToLookAhead) {
            val transfers = strategy.transfers.getOrNull(week) ?: emptyList()
            unusedTransfers = minOf(unusedTransfers + 1, 5) - transfers.size
            currentSquad = applyTransfers(currentSquad, transfers)
            currentSquad = optimizeSquad(currentSquad)
            // Use pointsAvgUpcomingGWs as a proxy for the expected points in a given week
            for (player in currentSquad.startingPlayers) {
                if (expectedLineups[player.commonName] == "OUT" || expectedLineups[player.commonName] == "SUSPENDED" || expectedLineups[player.commonName] == "QUES") {
                    player.predictedPointsThisGW = 0.0
                }
            }
            for (player in currentSquad.benchPlayers) {
                if (expectedLineups[player.commonName] == "OUT" || expectedLineups[player.commonName] == "SUSPENDED" || expectedLineups[player.commonName] == "QUES") {
                    player.predictedPointsThisGW = 0.0
                }
            }
            totalScore += currentSquad.startingPlayers.sumOf { it.pointsAvgUpcomingGWs } +
                    currentSquad.benchPlayers.sumOf { it.pointsAvgUpcomingGWs * Squad.BENCH_PLAYER_CHANCE_OF_PLAYING * Squad.BENCH_PLAYER_CHANCE_OF_BEING_REQUIRED } +
                    unusedTransfers * freeTransferValue
        }

        return totalScore
    }

    private fun optimizeSquad(squad: Squad): Squad {
        val allPlayers = (squad.startingPlayers + squad.benchPlayers).sortedWith(
            compareBy<PlayerPrediction> { player ->
                when (expectedLineups[player.commonName]) {
                    "STARTING" -> 0
                    "QUES" -> 1
                    "OUT" -> 2
                    "SUSPENDED" -> 2
                    else -> 3 // For players not in the expectedLineups map
                }
            }.thenByDescending { it.predictedPointsThisGW })

        val playersByPosition = allPlayers.groupBy { it.position }

        fun List<PlayerPrediction>.takeOrElse(n: Int) = take(n).let {
            if (it.size < n) it + List(n - it.size) { squad.startingPlayers.first() }
            else it
        }

        fun createFormation(def: Int, mid: Int, fwd: Int): List<PlayerPrediction> =
            playersByPosition["GKP"].orEmpty().takeOrElse(formationConstraints.gk) +
                    playersByPosition["DEF"].orEmpty().takeOrElse(def) +
                    playersByPosition["MID"].orEmpty().takeOrElse(mid) +
                    playersByPosition["FWD"].orEmpty().takeOrElse(fwd)

        val validFormations = sequence {
            for (def in formationConstraints.def) {
                for (mid in formationConstraints.mid) {
                    for (fwd in formationConstraints.fwd) {
                        if (1 + def + mid + fwd == 11) {
                            yield(createFormation(def, mid, fwd))
                        }
                    }
                }
            }
        }

        val bestFormation = validFormations
            .maxByOrNull { formation -> formation.sumOf { it.predictedPointsThisGW } }
            ?: throw IllegalStateException("No valid formation possible")

        val startingPlayers = bestFormation.filter { it.name != "Placeholder" }
        val benchPlayers = allPlayers.filter { it !in startingPlayers }

        return squad.copy(startingPlayers = startingPlayers, benchPlayers = benchPlayers)
    }

    private fun selectBestSquad(): Squad {
        var population = initialisePopulation()

        repeat(generations) {
            population = evolvePopulation(population)
        }

        val topSquad = population.maxBy { it.weightedTotalPredictedPoints }
        val sortedBenchPlayers = topSquad.benchPlayers.sortedByDescending { it.predictedPointsThisGW }
        return topSquad.copy(benchPlayers = sortedBenchPlayers)
    }

    private fun initialisePopulation(): List<Squad> {
        return (1..populationSize).map { createRandomSquad() }
    }

    // SEED SQUAD FROM SCRATCH
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
                val player = playerPredictions
                    .filter { expectedLineups[it.commonName] != null && expectedLineups[it.commonName] == "STARTING" }
                    .random(random)
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
                .sortedByDescending { it.calculateWeightedScore() }
                .take(count)
            startingPlayers.addAll(positionPlayers)
            benchPlayers.removeAll(positionPlayers)
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
        return startingPlayers.sumOf { it.calculateWeightedScore() } + benchPlayers.sumOf { Squad.BENCH_PLAYER_CHANCE_OF_PLAYING * it.calculateWeightedScore() * Squad.BENCH_PLAYER_CHANCE_OF_BEING_REQUIRED * Squad.DISCOUNT_FACTOR }
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

            // Build crossover squad
            val crossover = allPlayers.shuffled()
            val validCrossover = positionCounts.entries.flatMap { (position, count) ->
                crossover.filter { it.position == position }.take(count)
            }

            val (startingPlayers, benchPlayers) = assignPlayersToPositions(validCrossover, formation)
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
            val newPlayer = playerPredictions
                .filter { expectedLineups[it.commonName] != null && expectedLineups[it.commonName] == "STARTING" }
                .filter { it.position == positionToMutate && it !in allSquadPlayers }.random(random)

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
    val weightedTotalPredictedPoints: Double,
    val transfersMade: Int = 0,
    val availableFreeTransfers: Int = 1
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
                (startingPlayers + benchPlayers).groupingBy { it.team }.eachCount().values.find { it > 3 } == null &&
                (startingPlayers + benchPlayers).groupingBy { it.name }.eachCount().values.find { it > 1 } == null &&
                totalCost <= 1000 &&
                startingPlayers.none { playerBlocklist?.get(it.name) != null } &&
                benchPlayers.none { playerBlocklist?.get(it.name) != null }

    companion object {
        const val BENCH_WEIGHT = 1.0 / 9.0
        const val BENCH_PLAYER_CHANCE_OF_PLAYING = 0.8
        const val BENCH_PLAYER_CHANCE_OF_BEING_REQUIRED = 0.2
        const val DISCOUNT_FACTOR = 0.25
    }
}

private val possibleFormations = listOf(
    Formation(3, 5, 2),
    Formation(3, 4, 3),
    Formation(4, 4, 2),
    Formation(4, 3, 3),
    Formation(5, 4, 1),
    Formation(5, 3, 2)
)
private val positionCounts = mutableMapOf(
    "GKP" to 2,
    "DEF" to 5,
    "MID" to 5,
    "FWD" to 3
)

data class Formation(val defenders: Int, val midfielders: Int, val forwards: Int)

data class FormationConstraints(val gk: Int, val def: IntRange, val mid: IntRange, val fwd: IntRange)

val formationConstraints: FormationConstraints = FormationConstraints(1, 3..5, 2..5, 1..3)


data class OptimizationResult(
    val squad: Squad,
    val transferStrategy: TransferStrategy,
    val reasoning: String
)

data class TransferStrategy(
    val initialSquad: Squad,
    val transfers: List<List<Transfer>>
)

data class Transfer(
    val playerOut: PlayerPrediction,
    val playerIn: PlayerPrediction
)