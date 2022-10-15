package process.testRunner

import io.circe.JsonObject
import models.Configuration
import models.Result
import models.TestResult
import mutationtesting.MutationTestResult
import mutationtesting.Thresholds
import mutationtesting.FileResult
import mutationtesting.MutantResult
import mutationtesting.MutantStatus
import scala.Option
import scala.Tuple2
import scala.collection.mutable.ListBuffer
import utility.LoggingUtility
import mutationtesting.circe.mutationTestResultCodec
import utility.FileUtility
import java.io.IOException

class HTMLReporter: Reporter {
    override val logger = LoggingUtility()
    override val testResults = mutableListOf<TestResult>()

    private val indexHtml: String =
        """<!DOCTYPE html>
       <html lang="en">
       <head>
         <meta charset="UTF-8">
         <meta name="viewport" content="width=device-width, initial-scale=1.0">
         <script defer src="https://www.unpkg.com/mutation-testing-elements"></script>
       </head>
       <body>
         <mutation-test-report-app title-postfix="Stryker4k report">
           Your browser doesn't support <a href="https://caniuse.com/#search=custom%20elements">custom elements</a>.
           Please use a latest version of an evergreen browser (Firefox, Chrome, Safari, Opera, etc).
         </mutation-test-report-app>
         <script>
           const app = document.getElementsByTagName('mutation-test-report-app').item(0);
           function updateTheme() {
             document.body.style.backgroundColor = app.themeBackgroundColor;
           }
           app.addEventListener('theme-changed', updateTheme);
           updateTheme();
         </script>
         <script src="report.js"></script>
       </body>
       </html>"""

    override fun reportProgress(testResult: TestResult, totalMutations: Int) {
        testResults.add(testResult)
    }

    override fun reportFinished() {
        val jsonReport = generateJsonReport()

        val reportsPath = "${Configuration.sourcePath}/build/reports/stryker4k-reports"
        val indexLocation = "$reportsPath/index.html"
        val reportLocation = "$reportsPath/report.js"

        try {
            FileUtility.createDir(reportsPath)
            FileUtility.writeFile(indexLocation, indexHtml)
            FileUtility.writeFile(
                reportLocation,
                "document.querySelector('mutation-test-report-app').report = $jsonReport"
            )
            logger.info { "Wrote HTML report to: $indexLocation" }
        } catch (e: IOException) {
            logger.info { "Failed to write HTML report." }
            println(e)
        }
    }

    private fun generateJsonReport(): String? {
        val report = MutationTestResult<JsonObject>(
            Option.apply("https://git.io/mutation-testing-schema"),
            "1",
            Thresholds(DEFAULT_UPPER_THRESHOLD, DEFAULT_LOWER_THRESHOLD),
            Option.apply(Configuration.sourcePath),
            toFileResults(testResults),
            Option.apply(null),
            Option.apply(null),
            Option.apply(null),
            Option.apply(null),
            Option.apply(null)
        )

        return mutationTestResultCodec().apply(report).noSpaces()
    }

    private fun toFileResults(testResults: List<TestResult>): scala.collection.immutable.Map<String, FileResult> {
        val groupedResults = testResults.groupBy { it.mutation.mutable.sourceFile }
        val groupedResultsAsFileResult = groupedResults.mapValues { FileResult(
            it.key.originalText,
            toScalaSeq(it.value.map { testResult -> MutantResult(
                testResult.mutation.id.toString(),
                testResult.mutation.mutatorName,
                testResult.mutation.getText(),
                testResult.mutation.mutable.location,
                toMutantStatus(testResult.result),
                Option.apply(""),
                Option.apply(""),
                Option.apply(ListBuffer<String>().toSeq()),
                Option.apply(ListBuffer<String>().toSeq()),
                Option.apply(0),
                Option.apply(false)
            )}),
            "java"
        ) }
        val fileResults = groupedResultsAsFileResult.mapKeys { it.key.path }

        return toScalaMap(fileResults)
    }

    private fun toScalaSeq(list: List<MutantResult>): scala.collection.immutable.Seq<MutantResult> {
        val scalaSeq = ListBuffer<MutantResult>()
        list.forEach { scalaSeq.append(it) }

        return scalaSeq.toSeq()
    }

    private fun toScalaMap(map: Map<String, FileResult>): scala.collection.immutable.Map<String, FileResult> {
        val scalaMap = scala.collection.mutable.HashMap<String, FileResult>()

        map.forEach { scalaMap.addOne(Tuple2(it.key, it.value)) }

        return scalaMap.toMap(null)
    }

    private fun toMutantStatus(result: Result): MutantStatus = when(result) {
        Result.Killed -> MutantStatus.`Killed$`.`MODULE$`
        Result.Survived -> MutantStatus.`Survived$`.`MODULE$`
        else -> MutantStatus.`Survived$`.`MODULE$`
    }

    companion object {
        private const val DEFAULT_UPPER_THRESHOLD = 80
        private const val DEFAULT_LOWER_THRESHOLD = 60
    }
}
