package io.fplpicks.adaptor.out

import io.fplpicks.application.model.Player
import io.fplpicks.application.model.Position
import io.fplpicks.application.model.Team

/*fun List<RawTeam>.toModel(): List<Team> {
    return this.map {
        it.toModel()
    }
}*/

/*fun List<RawPlayer>.toModel(): List<Player> {
    return this.map {
        it.toPlayer()
    }
}*/

fun RawTeam.toModel(): Team {
    return Team(
        code = this.code,
        id = this.id,
        name = this.name,
        strength = this.strength,
        homeStrengthOverall = this.strengthOverallHome,
        awayStrengthOverall = this.strengthOverallAway
    )
}

fun RawPlayer.toPlayer(): Player {
    return Player(
        id = this.id,
        name = this.webName,
        position = when (this.elementType) {
            1 -> Position.GKP
            2 -> Position.DEF
            3 -> Position.MID
            4 -> Position.FWD
            else -> Position.GKP
        },
    )
}