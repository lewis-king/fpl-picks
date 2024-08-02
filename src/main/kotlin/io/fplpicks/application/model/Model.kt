package io.fplpicks.application.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime

data class Player(
    val id: Int,
    val name: String,
    val position: Position,

)

data class Team(
    // This is a unique identifier as ids for teams can swap between seasons
    val code: Int,
    val id: Int,
    val name: String,
    val strength: Int,
    val homeStrengthOverall: Int,
    val awayStrengthOverall: Int
)

data class GameWeekPerformance(
    val playerId: Int,
    val gameWeek: Int
)

data class PlayerFeatures(
    val playerId: Int,
    val gameWeek: Int,
    val recentFormPoints: Double,
    val upcomingFixtureDifficulty: Double,
    // This will consider whether a player has additional or missing fixtures coming up (weighted towards closer fixtures)
    val upcomingFixtureUtilisation: Double,
    val transferValue: Double,
    val pointsPerValue: Double
)

enum class Position {
    GKP, DEF, MID, FWD
}

data class PlayerGameweekData(
    val name: String,
    val position: String,
    val positionId: Int,
    val team: String,
    val xP: Double,
    val assists: Int,
    val bonus: Int,
    val bps: Int,
    val cleanSheets: Int,
    val creativity: Double,
    val element: Int,
    val expectedAssists: Double,
    val expectedGoalInvolvements: Double,
    val expectedGoals: Double,
    val expectedGoalsConceded: Double,
    val fixture: Int,
    val goalsConceded: Int,
    val goalsScored: Int,
    val ictIndex: Double,
    val influence: Double,
    val kickoffTime: Instant,
    val minutes: Int,
    val opponentTeam: Int,
    val opponentTeamStrength: Int,
    val ownGoals: Int,
    val penaltiesMissed: Int,
    val penaltiesSaved: Int,
    val redCards: Int,
    val round: Int,
    val saves: Int,
    val selected: Int,
    val starts: Int,
    val teamAScore: Int,
    val teamHScore: Int,
    val threat: Double,
    val totalPoints: Int,
    val transfersBalance: Int,
    val transfersIn: Int,
    val transfersOut: Int,
    val value: Int,
    val wasHome: Boolean,
    val yellowCards: Int,
    val GW: Int
)