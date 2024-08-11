package io.fplpicks.adaptor.out.lineups

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.jsoup.Jsoup

class FetchPredictedLineups {
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
    private val url = "https://www.rotowire.com/soccer/lineups.php"

    suspend fun fetch(): Map<String, String> {
        val response: HttpResponse = client.get(url)
        val document = Jsoup.parse(response.bodyAsText())
        // Select all elements with class "lineup__player"
        val playerElements = document.select(".lineup__player")


        // Extract player names and their status
        val playerInfo = playerElements.mapNotNull { element ->
            val playerLink = element.selectFirst("a[title]")
            val playerName = playerLink?.attr("title")

            if (playerName != null) {
                val injurySpan = playerLink.nextElementSibling()
                val status = when {
                    injurySpan == null -> "STARTING"
                    injurySpan.hasClass("lineup__inj") -> {
                        when (injurySpan.text().trim().uppercase()) {
                            "QUES" -> "QUESTIONABLE"
                            "OUT" -> "OUT"
                            "SUS" -> "SUSPENDED"
                            else -> "QUESTIONABLE" // Default to QUESTIONABLE if unknown status
                        }
                    }
                    else -> "STARTING" // If there's a span but it's not lineup__inj
                }
                val playerNameElements = playerName.split(" ")
                playerNameElements[playerNameElements.size - 1] to status
            } else {
                null
            }
        }

        // Create a map from the list of pairs
        return playerInfo.toMap()
    }
}