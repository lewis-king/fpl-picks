package io.fplpicks.adaptor.`in`

import io.fplpicks.adaptor.out.current.FetchRawCurrentFPLData
import io.fplpicks.adaptor.out.toModel
import kotlinx.coroutines.runBlocking

/**
 * This will be swapped out for a scheduler when ready
 */
fun main() {
    runBlocking {
        val fplData = FetchRawCurrentFPLData().fetchCurrentData()
        //val teams = fplData.teams.toModel()
        //val players = fplData.players.toModel()


    }
}