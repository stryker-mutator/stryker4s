package process.testRunner

import models.TestResult
import utility.LoggingUtility

interface Reporter {
    val logger: LoggingUtility
    val testResults: MutableList<TestResult>

    fun reportProgress(testResult: TestResult, totalMutations: Int)
    fun reportFinished()
}
