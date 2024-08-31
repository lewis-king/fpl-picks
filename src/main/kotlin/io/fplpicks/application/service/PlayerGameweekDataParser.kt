package io.fplpicks.application.service

import io.fplpicks.application.model.PlayerGameweekData
import io.fplpicks.application.model.Team
import kotlinx.datetime.Instant
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL

class PlayerGameweekDataParser {
    fun parseFromURL(url: String, teams: List<Team>): List<PlayerGameweekData> {
        val csvContent = URL(url).openStream().use { stream ->
            BufferedReader(InputStreamReader(stream)).use { reader ->
                reader.readText()
            }
        }
        return parseCSVContent(csvContent, teams)
    }

    fun parseFromResource(resourcePath: String, teams: List<Team>): List<PlayerGameweekData> {
        val inputStream = this::class.java.getResourceAsStream(resourcePath)
        requireNotNull(inputStream) { "Resource not found: $resourcePath" }
        val csvContent = inputStream.use { stream ->
            BufferedReader(InputStreamReader(stream)).use { reader ->
                reader.readText()
            }
        }
        return parseCSVContent(csvContent, teams)
    }

    private fun parseCSVContent(csvContent: String, teams: List<Team>): List<PlayerGameweekData> {
        val csvFormat = CSVFormat.Builder.create(CSVFormat.DEFAULT)
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreSurroundingSpaces(true)
            .setIgnoreHeaderCase(true)
            .setAllowMissingColumnNames(true)
            .build()

        val csvParser = CSVParser.parse(csvContent, csvFormat)

        val teamIdToTeam = teams.associateBy { it.id }
        val teamNameToTeam = teams.associateBy { it.name }

        return csvParser.map { record ->
            PlayerGameweekData(
                name = record["name"] ?: "",
                position = record["position"] ?: "",
                positionId = when (record["position"]) {
                    "GKP" -> 1
                    "DEF" -> 2
                    "MID" -> 3
                    "FWD" -> 4
                    else -> 0
                },
                team = record["team"] ?: "",
                xP = record["xP"]?.toDoubleOrNull() ?: 0.0,
                assists = record["assists"]?.toIntOrNull() ?: 0,
                bonus = record["bonus"]?.toIntOrNull() ?: 0,
                bps = record["bps"]?.toIntOrNull() ?: 0,
                cleanSheets = record["clean_sheets"]?.toIntOrNull() ?: 0,
                creativity = record["creativity"]?.toDoubleOrNull() ?: 0.0,
                element = record["element"]?.toIntOrNull() ?: 0,
                expectedAssists = record["expected_assists"]?.toDoubleOrNull() ?: 0.0,
                expectedGoalInvolvements = record["expected_goal_involvements"]?.toDoubleOrNull() ?: 0.0,
                expectedGoals = record["expected_goals"]?.toDoubleOrNull() ?: 0.0,
                expectedGoalsConceded = record["expected_goals_conceded"]?.toDoubleOrNull() ?: 0.0,
                fixture = record["fixture"]?.toIntOrNull() ?: 0,
                goalsConceded = record["goals_conceded"]?.toIntOrNull() ?: 0,
                goalsScored = record["goals_scored"]?.toIntOrNull() ?: 0,
                ictIndex = record["ict_index"]?.toDoubleOrNull() ?: 0.0,
                influence = record["influence"]?.toDoubleOrNull() ?: 0.0,
                kickoffTime = record["kickoff_time"]?.let { Instant.parse(it) } ?: Instant.DISTANT_PAST,
                minutes = record["minutes"]?.toIntOrNull() ?: 0,
                opponentTeam = record["opponent_team"]?.toIntOrNull() ?: 0,
                //opponentTeamStrength = if (record["was_home"]!!.toBoolean()) teamIdToTeam[record["opponent_team"].toInt()]!!.awayStrengthOverall else teamIdToTeam[record["opponent_team"].toInt()]!!.homeStrengthOverall,
                opponentTeamStrength = teamIdToTeam[record["opponent_team"].toInt()]!!.strength,
                ownGoals = record["own_goals"]?.toIntOrNull() ?: 0,
                penaltiesMissed = record["penalties_missed"]?.toIntOrNull() ?: 0,
                penaltiesSaved = record["penalties_saved"]?.toIntOrNull() ?: 0,
                redCards = record["red_cards"]?.toIntOrNull() ?: 0,
                round = record["round"]?.toIntOrNull() ?: 0,
                saves = record["saves"]?.toIntOrNull() ?: 0,
                selected = record["selected"]?.toIntOrNull() ?: 0,
                starts = record["starts"]?.toIntOrNull() ?: 0,
                teamAScore = record["team_a_score"]?.toIntOrNull() ?: 0,
                teamHScore = record["team_h_score"]?.toIntOrNull() ?: 0,
                threat = record["threat"]?.toDoubleOrNull() ?: 0.0,
                totalPoints = record["total_points"]?.toIntOrNull() ?: 0,
                transfersBalance = record["transfers_balance"]?.toIntOrNull() ?: 0,
                transfersIn = record["transfers_in"]?.toIntOrNull() ?: 0,
                transfersOut = record["transfers_out"]?.toIntOrNull() ?: 0,
                value = record["value"]?.toIntOrNull() ?: 0,
                wasHome = record["was_home"]?.toBoolean() ?: false,
                yellowCards = record["yellow_cards"]?.toIntOrNull() ?: 0,
                GW = record["GW"]?.toIntOrNull() ?: 0
            )
        }.toList()
    }
}