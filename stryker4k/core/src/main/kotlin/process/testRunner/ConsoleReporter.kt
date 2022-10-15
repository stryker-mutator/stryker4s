package process.testRunner

import models.Result
import models.TestResult
import utility.LoggingUtility

class ConsoleReporter: Reporter {
    override val logger = LoggingUtility()
    override val testResults = mutableListOf<TestResult>()

    override fun reportProgress(testResult: TestResult, totalMutations: Int) {
        testResults.add(testResult)
        logger.info { "Running mutants: ${testResult.mutation.id}/$totalMutations done" }
    }

    override fun reportFinished() {
        val totalKilled = numOfMutants(testResults, Result.Killed)
        val totalSurvived = numOfMutants(testResults, Result.Survived)
        val totalScore = calculateScore(testResults.size, totalKilled)
        val files = testResults.groupBy { it.mutation.mutable.sourceFile.path
                .replace("\\", "/")
                .substringAfter("/src/main/kotlin/")
        }
        val maxPathLength = calcMaxPathLength(files.keys.toList())
        val fileNameSpacing = "-".repeat(maxPathLength)

        logger.info { "Mutation testing finished. The results are:" }
        logger.info {"--$fileNameSpacing------------------------------------"}
        logger.info {"| ${" ".repeat(maxPathLength)} | % score | # killed | # survived |"}
        logger.info {"|-$fileNameSpacing-|---------|----------|------------|"}
        logger.info { generateRow(maxPathLength, totalScore, totalKilled, totalSurvived) }

        files.forEach {
            val numKilled = numOfMutants(it.value, Result.Killed)
            val numSurvived = numOfMutants(it.value, Result.Survived)
            val fileScore = calculateScore(it.value.size, numKilled)

            logger.info { generateRow(maxPathLength, fileScore, numKilled, numSurvived, it.key) }
        }

        logger.info { "--$fileNameSpacing------------------------------------" }
    }

    fun calcMaxPathLength(files: List<String>): Int {
        val maxPathLength = files.maxByOrNull { it.length }?.length ?: MIN_NAME_LENGTH

        return if (maxPathLength < MIN_NAME_LENGTH) MIN_NAME_LENGTH else maxPathLength
    }

    private fun generateRow(
        maxPathLength: Int,
        score: String,
        numKilled: Int,
        numSurvived: Int,
        path: String = "All files"): String =
            "| $path ${" ".repeat(maxPathLength - path.length)}" +
            "| $score ${" ".repeat(MAX_SCORE_LENGTH - score.length)}" +
            "| $numKilled ${" ".repeat(MAX_KILLED_LENGTH - numKilled.toString().length)}" +
            "| $numSurvived ${" ".repeat(MAX_SURVIVED_LENGTH - numSurvived.toString().length)}|"

    private fun numOfMutants(testResults: List<TestResult>, result: Result): Int =
        testResults.filter { it.result == result }.size

    private fun calculateScore(total: Int, killed: Int): String {
        val unformattedScore = if (total > MIN_SCORE) killed.toDouble() / total.toDouble() * MAX_SCORE else MIN_SCORE
        return "%.2f".format(unformattedScore) + "%"
    }

    companion object {
        private const val MIN_NAME_LENGTH = 9
        private const val MAX_SCORE_LENGTH = 7
        private const val MAX_KILLED_LENGTH = 8
        private const val MAX_SURVIVED_LENGTH = 10
        private const val MIN_SCORE = 0.0
        private const val MAX_SCORE = 100.0
    }
}
