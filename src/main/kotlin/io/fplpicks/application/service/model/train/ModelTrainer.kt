package io.fplpicks.application.service.model.train

import io.fplpicks.application.model.PlayerGameweekData
import io.fplpicks.application.model.PlayerGameweekFeatures
import io.fplpicks.application.service.ModelConfig
import io.fplpicks.application.service.PlayerGameweekDataParser
import io.fplpicks.application.service.TeamDataParser
import smile.data.DataFrame
import smile.data.Tuple
import smile.regression.RandomForest
import smile.validation.CrossValidation
import java.io.File
import java.io.ObjectOutputStream
import java.util.Properties

class ModelTrainer {
    private lateinit var model: RandomForest

    val seasons = listOf("2023-24")
    val gameweekDataDir = "gws"

    fun build() {
        val historicGameweekData = mutableListOf<PlayerGameweekData>()
        seasons.forEach {
            val teams = TeamDataParser().parseFromResource("/$it/teams.csv")
            historicGameweekData += PlayerGameweekDataParser().parseFromResource("/$it/gws/merged_gw.csv", teams)
        }

        val features = FeatureProcessing().createFeatures(historicGameweekData)
        train(features)
    }

    private fun train(features: List<PlayerGameweekFeatures>) {

        val tuples = features.map { feature ->
            Tuple.of(
                ModelConfig.featureValues(feature),
                ModelConfig.schema()
            )
        }

        // Create DataFrame
        val df = DataFrame.of(tuples)

        // Perform cross-validation
        val formula = smile.data.formula.Formula.lhs("points")
        val cv = CrossValidation.regression(5, formula, df, RandomForest::fit)

        println("Cross-validation RMSE: $cv")

        // Train the final model on all data
        //val props = Properties()
        //props.setProperty("smile.random_forest.trees", "500")
        model = RandomForest.fit(formula, df)
        println(model.metrics())

        // Print feature importance
        val importance = model.importance()
        val totalImportance = importance.sum()
        val normalizedImportance = importance.map { it / totalImportance }
        val featureNames = ModelConfig.featureNames()
        featureNames.forEachIndexed { index, name ->
            println("Importance of $name: ${normalizedImportance[index] * 100}%")
        }

        val file = File("build/model/model.ser")
        if (file.parentFile.mkdirs() || file.parentFile.exists()) {
            file.outputStream().use {
                ObjectOutputStream(it).writeObject(model)
            }
        }
    }
}

fun main() {
    ModelTrainer().build()
}