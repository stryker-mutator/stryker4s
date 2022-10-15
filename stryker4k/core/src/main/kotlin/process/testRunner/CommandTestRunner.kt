package process.testRunner

import models.Mutation
import models.Configuration
import models.TestResult
import models.Result
import utility.LoggingUtility
import utility.ProcessUtility
import java.io.IOException

object CommandTestRunner {
    val logger = LoggingUtility()

    fun run(mutations: List<Mutation>, tempDirPath: String) {
        mutations.forEach { mutation ->
            try {
                val exitValue = ProcessUtility.runCommand(
                    Configuration.command,
                    tempDirPath,
                    "ACTIVE_MUTATION",
                    mutation.id.toString()
                )

                val testResult = TestResult(mutation, if (exitValue == 1) Result.Killed else Result.Survived)
                Configuration.reporters.forEach { it.reportProgress(testResult, mutations.size) }
            } catch (e: IOException) {
                logger.info { "Failed to run tests for mutation number: ${mutation.id}. Ignoring this mutation..." }
                println(e)
            }
        }

        Configuration.reporters.forEach { it.reportFinished() }
    }
}
