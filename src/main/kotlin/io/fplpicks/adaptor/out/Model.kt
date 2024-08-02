package io.fplpicks.adaptor.out

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FPLFixture(
    val event: Int,
    @SerialName("team_h")
    val teamH: Int,
    @SerialName("team_h_difficulty")
    val teamHDifficulty: Int,
    @SerialName("team_a")
    val teamA: Int,
    @SerialName("team_a_difficulty")
    val teamADifficulty: Int,
)

@Serializable
data class FPLData(
    val events: List<Event>,
    val teams: List<RawTeam>,
    @SerialName("elements")
    val players: List<RawPlayer>
)

@Serializable
data class Event(
    val id: Int,
    val name: String,
    //@SerialName("deadline_time") val deadlineTime: LocalDateTime
)

@Serializable
data class RawTeam(
    val code: Int,
    val draw: Int,
    val form: String?,
    val id: Int,
    val loss: Int,
    val name: String,
    val played: Int,
    val points: Int,
    val position: Int,
    @SerialName("short_name") val shortName: String,
    val strength: Int,
    @SerialName("team_division") val teamDivision: String?,
    val unavailable: Boolean,
    val win: Int,
    @SerialName("strength_overall_home") val strengthOverallHome: Int,
    @SerialName("strength_overall_away") val strengthOverallAway: Int,
    @SerialName("strength_attack_home") val strengthAttackHome: Int,
    @SerialName("strength_attack_away") val strengthAttackAway: Int,
    @SerialName("strength_defence_home") val strengthDefenceHome: Int,
    @SerialName("strength_defence_away") val strengthDefenceAway: Int,
    @SerialName("pulse_id") val pulseId: Int
)

@Serializable
data class RawPlayer(
    @SerialName("chance_of_playing_next_round") val chanceOfPlayingNextRound: Int?,
    @SerialName("chance_of_playing_this_round") val chanceOfPlayingThisRound: Int?,
    val code: Int,
    @SerialName("cost_change_event") val costChangeEvent: Int,
    @SerialName("cost_change_event_fall") val costChangeEventFall: Int,
    @SerialName("cost_change_start") val costChangeStart: Int,
    @SerialName("cost_change_start_fall") val costChangeStartFall: Int,
    @SerialName("dreamteam_count") val dreamteamCount: Int,
    @SerialName("element_type") val elementType: Int,
    @SerialName("ep_next") val epNext: String,
    @SerialName("ep_this") val epThis: String?,
    @SerialName("event_points") val eventPoints: Int,
    @SerialName("first_name") val firstName: String,
    val form: String,
    val id: Int,
    @SerialName("in_dreamteam") val inDreamteam: Boolean,
    val news: String,
    @SerialName("news_added") val newsAdded: String?,
    @SerialName("now_cost") val nowCost: Int,
    val photo: String,
    @SerialName("points_per_game") val pointsPerGame: String,
    @SerialName("second_name") val secondName: String,
    @SerialName("selected_by_percent") val selectedByPercent: String,
    val special: Boolean,
    @SerialName("squad_number") val squadNumber: Int?,
    val status: String,
    val team: Int,
    @SerialName("team_code") val teamCode: Int,
    @SerialName("total_points") val totalPoints: Int,
    @SerialName("transfers_in") val transfersIn: Int,
    @SerialName("transfers_in_event") val transfersInEvent: Int,
    @SerialName("transfers_out") val transfersOut: Int,
    @SerialName("transfers_out_event") val transfersOutEvent: Int,
    @SerialName("value_form") val valueForm: String,
    @SerialName("value_season") val valueSeason: String,
    @SerialName("web_name") val webName: String,
    val minutes: Int,
    @SerialName("goals_scored") val goalsScored: Int,
    val assists: Int,
    @SerialName("clean_sheets") val cleanSheets: Int,
    @SerialName("goals_conceded") val goalsConceded: Int,
    @SerialName("own_goals") val ownGoals: Int,
    @SerialName("penalties_saved") val penaltiesSaved: Int,
    @SerialName("penalties_missed") val penaltiesMissed: Int,
    @SerialName("yellow_cards") val yellowCards: Int,
    @SerialName("red_cards") val redCards: Int,
    val saves: Int,
    val bonus: Int,
    val bps: Int,
    val influence: String,
    val creativity: String,
    val threat: String,
    @SerialName("ict_index") val ictIndex: String,
    val starts: Int,
    @SerialName("expected_goals") val expectedGoals: String,
    @SerialName("expected_assists") val expectedAssists: String,
    @SerialName("expected_goal_involvements") val expectedGoalInvolvements: String,
    @SerialName("expected_goals_conceded") val expectedGoalsConceded: String,
    @SerialName("influence_rank") val influenceRank: Int,
    @SerialName("influence_rank_type") val influenceRankType: Int,
    @SerialName("creativity_rank") val creativityRank: Int,
    @SerialName("creativity_rank_type") val creativityRankType: Int,
    @SerialName("threat_rank") val threatRank: Int,
    @SerialName("threat_rank_type") val threatRankType: Int,
    @SerialName("ict_index_rank") val ictIndexRank: Int,
    @SerialName("ict_index_rank_type") val ictIndexRankType: Int,
    @SerialName("corners_and_indirect_freekicks_order") val cornersAndIndirectFreekicksOrder: Int?,
    @SerialName("corners_and_indirect_freekicks_text") val cornersAndIndirectFreekicksText: String,
    @SerialName("direct_freekicks_order") val directFreekicksOrder: Int?,
    @SerialName("direct_freekicks_text") val directFreekicksText: String,
    @SerialName("penalties_order") val penaltiesOrder: Int?,
    @SerialName("penalties_text") val penaltiesText: String,
    @SerialName("expected_goals_per_90") val expectedGoalsPer90: Double,
    @SerialName("saves_per_90") val savesPer90: Double,
    @SerialName("expected_assists_per_90") val expectedAssistsPer90: Double,
    @SerialName("expected_goal_involvements_per_90") val expectedGoalInvolvementsPer90: Double,
    @SerialName("expected_goals_conceded_per_90") val expectedGoalsConcededPer90: Double,
    @SerialName("goals_conceded_per_90") val goalsConcededPer90: Double,
    @SerialName("now_cost_rank") val nowCostRank: Int,
    @SerialName("now_cost_rank_type") val nowCostRankType: Int,
    @SerialName("form_rank") val formRank: Int,
    @SerialName("form_rank_type") val formRankType: Int,
    @SerialName("points_per_game_rank") val pointsPerGameRank: Int,
    @SerialName("points_per_game_rank_type") val pointsPerGameRankType: Int,
    @SerialName("selected_rank") val selectedRank: Int,
    @SerialName("selected_rank_type") val selectedRankType: Int,
    @SerialName("starts_per_90") val startsPer90: Double,
    @SerialName("clean_sheets_per_90") val cleanSheetsPer90: Double
)