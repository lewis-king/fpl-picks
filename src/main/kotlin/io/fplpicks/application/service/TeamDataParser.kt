package io.fplpicks.application.service

import io.fplpicks.application.model.Team
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import java.io.InputStreamReader

class TeamDataParser {
    fun parseFromResource(resourcePath: String): List<Team> {
        val inputStream = this.javaClass.getResourceAsStream(resourcePath)
        requireNotNull(inputStream) { "Resource not found: $resourcePath" }

        val reader = InputStreamReader(inputStream)
        val csvFormat = CSVFormat.Builder.create(CSVFormat.DEFAULT)
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreSurroundingSpaces(true)
            .setIgnoreHeaderCase(true)
            .setAllowMissingColumnNames(true)
            .build()

        val csvParser = CSVParser(reader, csvFormat)

        return csvParser.map { record ->
            Team(
                code = record["code"].toInt(),
                id = record["id"].toInt(),
                name = record["name"],
                strength = record["strength"].toInt(),
                homeStrengthOverall = record["strength_overall_home"].toInt(),
                awayStrengthOverall = record["strength_overall_away"].toInt()
            )
        }.toList()
    }
}