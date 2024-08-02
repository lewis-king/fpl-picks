package io.fplpicks.adaptor.out.current

import io.fplpicks.adaptor.out.FPLFixture
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class FetchFixtureSchedule {
    //replace this with Koin for DI
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }
    //Replace this with Koin for DI
    private val url = "https://fantasy.premierleague.com/api/fixtures/"

    suspend fun fetchUpcomingFixtures(): List<FPLFixture> {
        val response: HttpResponse = client.get(url)
        val fplFixtures = response.body<List<FPLFixture>>()
        return fplFixtures
    }
}