package io.fplpicks.application.service.model.train

import io.fplpicks.application.model.PlayerGameweekData
import io.fplpicks.application.model.PlayerGameweekFeatures
import io.fplpicks.application.service.ModelConfig
import io.fplpicks.application.service.PlayerGameweekDataParser
import io.fplpicks.application.service.TeamDataParser
import smile.data.DataFrame
import smile.data.Tuple
import smile.data.formula.Formula
import smile.regression.RandomForest
import smile.validation.CrossValidation
import smile.validation.metric.RMSE
import java.io.File
import java.io.ObjectOutputStream
import java.io.PrintWriter
import java.util.Properties
import kotlin.math.sqrt

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

        val formula = Formula.lhs("points")

        // Define hyperparameters to tune
        val numTreesOptions = listOf(100, 200, 500)
        val maxDepthOptions = listOf(10, 20, 30)
        val maxNodesOptions = listOf(256, 512, 1024)

        var bestModel: RandomForest? = null
        var bestRMSE = Double.MAX_VALUE

        // Perform grid search
        for (numTrees in numTreesOptions) {
            for (maxDepth in maxDepthOptions) {
                for (maxNodes in maxNodesOptions) {
                    val props = Properties().apply {
                        setProperty("smile.random.forest.trees", numTrees.toString())
                        setProperty("smile.random.forest.max.depth", maxDepth.toString())
                        setProperty("smile.random.forest.max.nodes", maxNodes.toString())
                    }

                    // Perform cross-validation
                    val cv = CrossValidation.regression(5, formula, df) { f, d ->
                        RandomForest.fit(f, d, props)
                    }

                    val rmse = cv.avg.rmse
                    println("RMSE for trees=$numTrees, depth=$maxDepth, nodes=$maxNodes: $rmse")

                    if (rmse < bestRMSE) {
                        bestRMSE = rmse
                        bestModel = RandomForest.fit(formula, df, props)
                    }
                }
            }
        }

        println("Best Cross-validation RMSE: $bestRMSE")

        // Print best model metrics
        println(bestModel?.metrics())

        // Print feature importance for the best model
        val importance = bestModel?.importance() ?: doubleArrayOf()
        val totalImportance = importance.sum()
        val normalizedImportance = importance.map { it / totalImportance }
        val featureNames = ModelConfig.featureNames()

        // Save the best model
        val file = File("build/model/model.ser")
        if (file.parentFile.mkdirs() || file.parentFile.exists()) {
            file.outputStream().use {
                ObjectOutputStream(it).writeObject(bestModel)
            }
        }
        // Save the best model's RMSE
        val metaFile = File("build/model/meta.txt")
        metaFile.outputStream().use { outputStream ->
            PrintWriter(outputStream).use { writer ->
                writer.println(bestModel?.metrics())
                featureNames.forEachIndexed { index, name ->
                    println("Importance of $name: ${normalizedImportance[index] * 100}%")
                }
            }
        }

        println("Model saved to ${file.absolutePath}")
    }
}

fun main() {
    ModelTrainer().build()
}