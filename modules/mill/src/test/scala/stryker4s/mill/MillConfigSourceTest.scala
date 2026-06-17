package stryker4s.mill

import cats.effect.IO
import fs2.io.file.Path
import stryker4s.testkit.Stryker4sIOSuite

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
      filesValue = None,
      testFilterValue = Some(Seq("*MyTest")),
      reportersValue = Some(Seq("html", "console")),
      excludedMutationsValue = Some(Seq("BooleanLiteral")),
      thresholdsHighValue = Some(85),
      thresholdsLowValue = Some(60),
      thresholdsBreakValue = Some(0),
      dashboardBaseUrlValue = None,
      dashboardReportTypeValue = None,
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
      _ <- config.testFilter.load.assertEquals(Seq("*MyTest"))
      _ <- config.thresholdsHigh.load.assertEquals(85)
      _ <- config.dashboardProject.load.assertEquals(Some("github.com/foo/bar"))
      _ <- config.dashboardModule.load.assertEquals(Some("my-module"))
      _ <- config.timeout.load.assertEquals(5.seconds)
      _ <- config.concurrency.load.assertEquals(4)
      _ <- config.cleanTmpDir.load.assertEquals(true)
    } yield ()
  }

  test("should make the baseDir absolute") {
    emptyConfigSource.baseDir.load.assertEquals(Path("/tmp/project").absolute)
  }

  test("missing values are reported as missing") {
    emptyConfigSource.thresholdsHigh.attempt
      .map(_.leftValue.messages.loneElement)
      .assertEquals("Missing mill config strykerThresholdsHigh")
  }

  test("fails on values not supported by the mill plugin") {
    emptyConfigSource.legacyTestRunner.attempt
      .map(_.leftValue.messages.loneElement)
      .assertEquals("Missing key legacyTestRunner is not supported by mill build")
  }
}
