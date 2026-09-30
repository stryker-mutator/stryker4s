package stryker4s.mill

import cats.effect.IO
import cats.syntax.all.*
import ciris.ConfigValue
import fs2.io.file.Path
import stryker4s.config.*
import stryker4s.testkit.Stryker4sIOSuite
import sttp.client4.UriContext

import scala.concurrent.duration.*
import scala.meta.dialects

class MillConfigSourceTest extends Stryker4sIOSuite {

  def emptyConfigSource: MillConfigSource[IO] = new MillConfigSource[IO](
    baseDirValue = Path("/tmp/project"),
    mutateValue = None,
    filesValue = None,
    testFilterValue = None,
    reportersValue = None,
    excludedMutationsValue = None,
    thresholdsHighValue = None,
    thresholdsLowValue = None,
    thresholdsBreakValue = None,
    dashboardBaseUrlValue = None,
    dashboardReportTypeValue = None,
    dashboardProjectValue = None,
    dashboardVersionValue = None,
    dashboardModuleValue = None,
    timeoutValue = None,
    timeoutFactorValue = None,
    maxTestRunnerReuseValue = None,
    scalaDialectValue = None,
    concurrencyValue = None,
    debugLogTestRunnerStdoutValue = None,
    debugDebugTestRunnerValue = None,
    staticTmpDirValue = None,
    cleanTmpDirValue = None,
    openReportValue = None
  )

  test("should load filled-in values") {
    val config = new MillConfigSource[IO](
      baseDirValue = Path("/tmp/project"),
      mutateValue = Some(Seq("src/main/scala/**.scala")),
      filesValue = Some(Seq("src/main/scala/**")),
      testFilterValue = Some(Seq("*MyTest")),
      reportersValue = Some(Seq("html", "console")),
      excludedMutationsValue = Some(Seq("BooleanLiteral")),
      thresholdsHighValue = Some(85),
      thresholdsLowValue = Some(60),
      thresholdsBreakValue = Some(0),
      dashboardBaseUrlValue = Some("https://dashboard.stryker-mutator.io"),
      dashboardReportTypeValue = Some("full"),
      dashboardProjectValue = Some("github.com/foo/bar"),
      dashboardVersionValue = Some("main"),
      dashboardModuleValue = Some("my-module"),
      timeoutValue = Some(5.seconds),
      timeoutFactorValue = Some(1.5),
      maxTestRunnerReuseValue = Some(10),
      scalaDialectValue = Some(dialects.Scala3),
      concurrencyValue = Some(4),
      debugLogTestRunnerStdoutValue = Some(true),
      debugDebugTestRunnerValue = Some(false),
      staticTmpDirValue = Some(false),
      cleanTmpDirValue = Some(true),
      openReportValue = Some(false)
    )

    for {
      _ <- config.mutate.load.assertEquals(Seq("src/main/scala/**.scala"))
      _ <- config.files.load.assertEquals(Seq("src/main/scala/**"))
      _ <- config.testFilter.load.assertEquals(Seq("*MyTest"))
      _ <- config.reporters.load.assertEquals(Seq[ReporterType](Html, Console))
      _ <- config.excludedMutations.load.assertEquals(Seq(ExcludedMutation("BooleanLiteral")))
      _ <- config.thresholdsHigh.load.assertEquals(85)
      _ <- config.thresholdsLow.load.assertEquals(60)
      _ <- config.thresholdsBreak.load.assertEquals(0)
      _ <- config.dashboardBaseUrl.load.assertEquals(uri"https://dashboard.stryker-mutator.io")
      _ <- config.dashboardReportType.load.assertEquals(Full: DashboardReportType)
      _ <- config.dashboardProject.load.assertEquals(Some("github.com/foo/bar"))
      _ <- config.dashboardVersion.load.assertEquals(Some("main"))
      _ <- config.dashboardModule.load.assertEquals(Some("my-module"))
      _ <- config.timeout.load.assertEquals(5.seconds)
      _ <- config.timeoutFactor.load.assertEquals(1.5)
      _ <- config.maxTestRunnerReuse.load.assertEquals(Some(10))
      _ <- config.scalaDialect.load.assertEquals(dialects.Scala3)
      _ <- config.concurrency.load.assertEquals(4)
      _ <- config.debugLogTestRunnerStdout.load.assertEquals(true)
      _ <- config.debugDebugTestRunner.load.assertEquals(false)
      _ <- config.staticTmpDir.load.assertEquals(false)
      _ <- config.cleanTmpDir.load.assertEquals(true)
      _ <- config.openReport.load.assertEquals(false)
    } yield ()
  }

  test("should make the baseDir absolute") {
    emptyConfigSource.baseDir.load.assertEquals(Path("/tmp/project").absolute)
  }

  test("missing values are reported as missing with their mill config key") {
    val source = emptyConfigSource
    // Every value-backed config def should report the exact mill build key it reads from when absent
    val cases: List[(ConfigValue[IO, ?], String)] = List(
      source.mutate -> "strykerMutate",
      source.files -> "strykerFiles",
      source.testFilter -> "strykerTestFilter",
      source.reporters -> "strykerReporters",
      source.excludedMutations -> "strykerExcludedMutations",
      source.thresholdsHigh -> "strykerThresholdsHigh",
      source.thresholdsLow -> "strykerThresholdsLow",
      source.thresholdsBreak -> "strykerThresholdsBreak",
      source.dashboardBaseUrl -> "strykerDashboardBaseUrl",
      source.dashboardReportType -> "strykerDashboardReportType",
      source.dashboardProject -> "strykerDashboardProject",
      source.dashboardVersion -> "strykerDashboardVersion",
      source.dashboardModule -> "strykerDashboardModule",
      source.timeout -> "strykerTimeout",
      source.timeoutFactor -> "strykerTimeoutFactor",
      source.maxTestRunnerReuse -> "strykerMaxTestRunnerReuse",
      source.scalaDialect -> "strykerScalaDialect",
      source.concurrency -> "strykerConcurrency",
      source.debugLogTestRunnerStdout -> "strykerDebugLogTestRunnerStdout",
      source.debugDebugTestRunner -> "strykerDebugDebugTestRunner",
      source.staticTmpDir -> "strykerStaticTmpDir",
      source.cleanTmpDir -> "strykerCleanTmpDir",
      source.openReport -> "strykerOpenReport"
    )

    cases.traverse_ { case (configValue, key) =>
      configValue.attempt
        .map(_.leftValue.messages.loneElement)
        .assertEquals(s"Missing mill config $key")
    }
  }

  test("fails on values not supported by the mill plugin") {
    emptyConfigSource.legacyTestRunner.attempt
      .map(_.leftValue.messages.loneElement)
      .assertEquals("Missing key legacyTestRunner is not supported by mill build")
  }
}
