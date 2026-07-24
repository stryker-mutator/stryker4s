package stryker4s.report

import fansi.Color.*
import fs2.Stream
import mutationtesting.*
import stryker4s.config.Config
import stryker4s.testkit.{LogMatchers, Stryker4sIOSuite}

import scala.concurrent.duration.*

class ConsoleReporterTest extends Stryker4sIOSuite with LogMatchers {

  implicit val config: Config = Config.default

  test("mutantTested should log progress") {
    val sut = new ConsoleReporter()

    mutantTestedStream(2)
      .through(sut.mutantTested)
      .compile
      .drain
      .assertLoggedInfo("Tested mutant 1/2 (50%)")
      .assertLoggedInfo("Tested mutant 2/2 (100%)")
  }

  test("mutantTested should round decimal numbers") {
    val sut = new ConsoleReporter()

    mutantTestedStream(3)
      .through(sut.mutantTested)
      .compile
      .drain
      .assertLoggedInfo("Tested mutant 1/3 (33%)")
      .assertLoggedInfo("Tested mutant 3/3 (100%)")
  }
  def mutantTestedStream(size: Int) = Stream.constant(()).take(size.toLong).as(MutantTestedEvent(size))

  test("reportFinishedRun should report killed mutants as debug") {
    implicit val config: Config = Config.default
    val sut = new ConsoleReporter()
    val results = MutationTestResult(
      thresholds = mutationtesting.Thresholds(80, 60),
      files = Map(
        "stryker4s.scala" -> FileResult(
          source = "<!=",
          mutants = Seq(
            MutantResult("0", "BinaryOperator", "==", Location(Position(1, 2), Position(1, 4)), MutantStatus.Killed)
          )
        )
      )
    )
    val metrics = Metrics.calculateMetrics(results)
    sut
      .onRunFinished(FinishedRunEvent(results, metrics, 15.seconds, config.baseDir))
      .assertLoggedInfo("Mutation run finished! Took 15 seconds")
      .assertLoggedInfo(s"Total mutants: ${Cyan("1")}, detected: ${Green("1")}, undetected: ${Red("0")}")
      .assertLoggedDebug(s"""Detected mutants:
                            |0. [${Magenta("Killed")}] [${LightGray("BinaryOperator")}]
                            |${Blue("stryker4s.scala")}:${Yellow("1")}:${Yellow("2")}
                            |${Red("-\t!=")}
                            |${Green("+\t==")}
                            |""".stripMargin)

  }

  test("reportFinishedRun should report a finished run with multiple mutants") {
    implicit val config: Config = Config.default
    val sut = new ConsoleReporter()
    val results = MutationTestResult(
      thresholds = mutationtesting.Thresholds(80, 60),
      files = Map(
        "stryker4s.scala" -> FileResult(
          source = "<!=",
          mutants = Seq(
            MutantResult("0", "BinaryOperator", ">", Location(Position(1, 1), Position(1, 2)), MutantStatus.Survived),
            MutantResult("1", "BinaryOperator", "==", Location(Position(1, 2), Position(1, 4)), MutantStatus.Killed)
          )
        ),
        "subPath/stryker4s.scala" -> FileResult(
          source = "1",
          mutants = Seq(
            MutantResult("2", "BinaryOperator", "0", Location(Position(1, 1), Position(1, 2)), MutantStatus.Survived)
          )
        )
      )
    )
    val metrics = Metrics.calculateMetrics(results)
    sut
      .onRunFinished(FinishedRunEvent(results, metrics, 1.minute, config.baseDir))
      .assertLoggedInfo("Mutation run finished! Took 1 minute")
      .assertLoggedInfo(s"Total mutants: ${Cyan("3")}, detected: ${Green("1")}, undetected: ${Red("2")}")
      .assertLoggedInfo(s"""Undetected mutants:
                           |0. [${Magenta("Survived")}] [${LightGray("BinaryOperator")}]
                           |${Blue("stryker4s.scala")}:${Yellow("1")}:${Yellow("1")}
                           |${Red("-\t<")}
                           |${Green("+\t>")}
                           |
                           |2. [${Magenta("Survived")}] [${LightGray("BinaryOperator")}]
                           |${Blue("subPath/stryker4s.scala")}:${Yellow("1")}:${Yellow("1")}
                           |${Red("-\t1")}
                           |${Green("+\t0")}
                           |""".stripMargin)
  }

  test("reportFinishedRun should log mutants sorted by id") {
    implicit val config: Config = Config.default
    val sut = new ConsoleReporter()
    val results = MutationTestResult(
      thresholds = mutationtesting.Thresholds(80, 60),
      files = Map(
        "subPath/stryker4s.scala" -> FileResult(
          source = "1",
          mutants = Seq(
            MutantResult("2", "BinaryOperator", "0", Location(Position(1, 1), Position(1, 2)), MutantStatus.Survived)
          )
        ),
        "stryker4s.scala" -> FileResult(
          source = "<!=",
          mutants = Seq(
            MutantResult(
              "1",
              "BinaryOperator",
              "==",
              Location(Position(1, 2), Position(1, 4)),
              MutantStatus.Survived
            ),
            MutantResult("0", "BinaryOperator", ">", Location(Position(1, 1), Position(1, 2)), MutantStatus.Survived)
          )
        )
      )
    )
    sut
      .onRunFinished(FinishedRunEvent(results, Metrics.calculateMetrics(results), 15.seconds, config.baseDir))
      .assertLoggedInfo(s"Total mutants: ${Cyan("3")}, detected: ${Green("0")}, undetected: ${Red("3")}")
      .assertLoggedInfo(s"""Undetected mutants:
                           |0. [${Magenta("Survived")}] [${LightGray("BinaryOperator")}]
                           |${Blue("stryker4s.scala")}:${Yellow("1")}:${Yellow("1")}
                           |${Red("-\t<")}
                           |${Green("+\t>")}
                           |
                           |1. [${Magenta("Survived")}] [${LightGray("BinaryOperator")}]
                           |${Blue("stryker4s.scala")}:${Yellow("1")}:${Yellow("2")}
                           |${Red("-\t!=")}
                           |${Green("+\t==")}
                           |
                           |2. [${Magenta("Survived")}] [${LightGray("BinaryOperator")}]
                           |${Blue("subPath/stryker4s.scala")}:${Yellow("1")}:${Yellow("1")}
                           |${Red("-\t1")}
                           |${Green("+\t0")}
                           |""".stripMargin)
  }

  test("reportFinishedRun should report two line mutants properly") {
    implicit val config: Config = Config.default
    val sut = new ConsoleReporter()
    val results = MutationTestResult(
      thresholds = mutationtesting.Thresholds(80, 60),
      files = Map(
        "stryker4s.scala" -> FileResult(
          source = "foo\nbar\nbaz",
          mutants = Seq(
            MutantResult(
              "0",
              "StringLiteral",
              "qux\nfoo",
              Location(Position(2, 1), Position(3, 4)),
              MutantStatus.Survived
            )
          )
        )
      )
    )
    val metrics = Metrics.calculateMetrics(results)
    sut
      .onRunFinished(FinishedRunEvent(results, metrics, 15.seconds, config.baseDir))
      .assertLoggedInfo(s"""Undetected mutants:
                           |0. [${Magenta("Survived")}] [${LightGray("StringLiteral")}]
                           |${Blue("stryker4s.scala")}:${Yellow("2")}:${Yellow("1")}
                           |${Red("-\tbar\n\tbaz")}
                           |${Green("+\tqux\n\tfoo")}
                           |""".stripMargin)
  }

  test("reportFinishedRun should report multiline mutants properly") {
    implicit val config: Config = Config.default
    val sut = new ConsoleReporter()
    val results = MutationTestResult(
      thresholds = mutationtesting.Thresholds(80, 60),
      files = Map(
        "stryker4s.scala" -> FileResult(
          source = "foo\nbar\nbaz",
          mutants = Seq(
            MutantResult(
              "0",
              "StringLiteral",
              "ux\nqux\nfoo",
              Location(Position(1, 2), Position(3, 4)),
              MutantStatus.Survived
            )
          )
        )
      )
    )
    val metrics = Metrics.calculateMetrics(results)
    sut
      .onRunFinished(FinishedRunEvent(results, metrics, 15.seconds, config.baseDir))
      .assertLoggedInfo(s"Total mutants: ${Cyan("1")}, detected: ${Green("0")}, undetected: ${Red("1")}")
      .assertLoggedInfo(s"""Undetected mutants:
                           |0. [${Magenta("Survived")}] [${LightGray("StringLiteral")}]
                           |${Blue("stryker4s.scala")}:${Yellow("1")}:${Yellow("2")}
                           |${Red("-\too\n\tbar\n\tbaz")}
                           |${Green("+\tux\n\tqux\n\tfoo")}
                           |""".stripMargin)
  }

  test("reportFinishedRun should round decimal mutation scores") {
    implicit val config: Config =
      Config.default.copy(thresholds = stryker4s.config.Thresholds(break = 48, low = 49, high = 50))
    val sut = new ConsoleReporter()
    val threeReport = MutationTestResult(
      thresholds = Thresholds(80, 60), // These thresholds are not used
      files = Map(
        "stryker4s.scala" -> FileResult(
          source = "foo\nbar\nbaz",
          mutants = Seq(
            MutantResult("0", "", "bar\nbaz\nqu", Location(Position(1, 1), Position(2, 2)), MutantStatus.Survived),
            MutantResult("1", "", "==", Location(Position(1, 1), Position(1, 3)), MutantStatus.Killed),
            MutantResult("2", "", ">=", Location(Position(1, 1), Position(1, 3)), MutantStatus.Killed)
          )
        )
      )
    )

    sut
      .onRunFinished(
        FinishedRunEvent(threeReport, Metrics.calculateMetrics(threeReport), 15.seconds, config.baseDir)
      )
      .assertLoggedInfo(s"Mutation score: ${Green("66.67")}%")

  }

  test("reportFinishedRun should log NaN correctly as n/a") {
    implicit val config: Config = Config.default
    val sut = new ConsoleReporter()

    val report = MutationTestResult(
      thresholds = Thresholds(80, 60),
      files = Map(
        "stryker4s.scala" -> FileResult(
          source = "foo\nbar\nbaz",
          mutants = Seq(
            MutantResult("0", "", "bar\nbaz\nqu", Location(Position(1, 1), Position(2, 2)), MutantStatus.CompileError)
          )
        )
      )
    )

    sut
      .onRunFinished(FinishedRunEvent(report, Metrics.calculateMetrics(report), 15.seconds, config.baseDir))
      .assertLoggedInfo(s"Mutation score: ${LightGray("n/a")}")
  }

  // 1 killed, 1 survived, mutation score 50
  val report = MutationTestResult(
    thresholds = Thresholds(80, 60), // These thresholds are not used
    files = Map(
      "stryker4s.scala" -> FileResult(
        source = "foo\nbar\nbaz",
        mutants = Seq(
          MutantResult("0", "", "bar\nbaz\nqu", Location(Position(1, 1), Position(2, 2)), MutantStatus.Survived),
          MutantResult("1", "", "==", Location(Position(1, 1), Position(1, 3)), MutantStatus.Killed)
        )
      )
    )
  )
  val metrics = Metrics.calculateMetrics(report)

  val reportWithNoCoverage = MutationTestResult(
    thresholds = Thresholds(80, 60), // These thresholds are not used
    files = Map(
      "stryker4s.scala" -> FileResult(
        source = "foo\nbar\nbaz",
        mutants = Seq(
          MutantResult("0", "", "bar\nbaz\nqu", Location(Position(1, 1), Position(2, 2)), MutantStatus.Survived),
          MutantResult("1", "", "==", Location(Position(1, 1), Position(1, 3)), MutantStatus.Killed),
          MutantResult("2", "", ">=", Location(Position(1, 1), Position(1, 3)), MutantStatus.NoCoverage)
        )
      )
    )
  )
  val metricsWithNoCoverage = Metrics.calculateMetrics(reportWithNoCoverage)

  test("reportFinishedRun should report the mutation score when it is info") {
    implicit val config: Config =
      Config.default.copy(thresholds = stryker4s.config.Thresholds(break = 48, low = 49, high = 50))
    val sut = new ConsoleReporter()

    sut
      .onRunFinished(FinishedRunEvent(report, metrics, 15.seconds, config.baseDir))
      .assertLoggedInfo(s"Mutation score: ${Green("50.0")}%")
  }

  test("reportFinishedRun should report the mutation score when it is warning") {
    implicit val config: Config =
      Config.default.copy(thresholds = stryker4s.config.Thresholds(break = 49, low = 50, high = 51))
    val sut = new ConsoleReporter()

    sut
      .onRunFinished(FinishedRunEvent(report, metrics, 15.seconds, config.baseDir))
      .assertLoggedWarn(s"Mutation score: ${Yellow("50.0")}%")
  }

  test("reportFinishedRun should report the mutation score when it is dangerously low") {
    implicit val config: Config =
      Config.default.copy(thresholds = stryker4s.config.Thresholds(break = 50, low = 51, high = 52))
    val sut = new ConsoleReporter()

    sut
      .onRunFinished(FinishedRunEvent(report, metrics, 15.seconds, config.baseDir))
      .assertLoggedError("Mutation score dangerously low! Below the low threshold of 51%")
      .assertLoggedError(s"Mutation score: ${Red("50.0")}%")
  }

  test("reportFinishedRun should log when below threshold") {
    implicit val config: Config =
      Config.default.copy(thresholds = stryker4s.config.Thresholds(break = 51, low = 52, high = 53))
    val sut = new ConsoleReporter()

    sut
      .onRunFinished(FinishedRunEvent(report, metrics, 15.seconds, config.baseDir))
      .assertLoggedError(s"Mutation score below threshold! Mutation score: ${Red("50.0")}%. Threshold: 51%")
  }

  test("reportFinishedRun should log covered code if it is different to the total") {
    implicit val config: Config =
      Config.default.copy(thresholds = stryker4s.config.Thresholds(break = 31, low = 32, high = 34))
    val sut = new ConsoleReporter()

    sut
      .onRunFinished(FinishedRunEvent(reportWithNoCoverage, metricsWithNoCoverage, 15.seconds, config.baseDir))
      .assertLoggedWarn(s"Mutation score: ${Yellow("33.33")}% (of total), ${Green("50.0")}% (of covered code)")
  }

  test("reportFinishedRun should warn when mutation score is n/a") {
    implicit val config: Config = Config.default
    val sut = new ConsoleReporter()
    val allIgnoredReport = MutationTestResult(
      thresholds = Thresholds(80, 60),
      files = Map(
        "stryker4s.scala" -> FileResult(
          source = "foo",
          mutants = Seq(
            MutantResult("0", "", ">", Location(Position(1, 1), Position(1, 2)), MutantStatus.Ignored)
          )
        )
      )
    )

    sut
      .onRunFinished(
        FinishedRunEvent(allIgnoredReport, Metrics.calculateMetrics(allIgnoredReport), 15.seconds, config.baseDir)
      )
      .assertLoggedWarn(
        "It looks like no mutations were actually tested. This could indicate that the test runner is not set up correctly, or that all mutants are excluded from coverage."
      )
      .assertLoggedWarn(
        "You can enable 'log-test-runner-stdout' in your configuration to see test runner output. See https://stryker-mutator.io/docs/stryker4s/configuration/ for more information."
      )

  }

  test("reportFinishedRun should warn when all mutants survived") {
    implicit val config: Config = Config.default
    val sut = new ConsoleReporter()
    val allSurvivedReport = MutationTestResult(
      thresholds = Thresholds(80, 60),
      files = Map(
        "stryker4s.scala" -> FileResult(
          source = "foo",
          mutants = Seq(
            MutantResult("0", "", ">", Location(Position(1, 1), Position(1, 2)), MutantStatus.Survived),
            MutantResult("1", "", "==", Location(Position(1, 2), Position(1, 4)), MutantStatus.Survived)
          )
        )
      )
    )

    sut
      .onRunFinished(
        FinishedRunEvent(allSurvivedReport, Metrics.calculateMetrics(allSurvivedReport), 15.seconds, config.baseDir)
      )
      .assertLoggedWarn(
        "None of the mutations were detected. This may indicate that your tests are not running, or that the test assertions are too weak to catch mutations."
      )
      .assertLoggedWarn(
        "You can enable 'log-test-runner-stdout' in your configuration to see test runner output. See https://stryker-mutator.io/docs/stryker4s/configuration/ for more information."
      )
  }
}
