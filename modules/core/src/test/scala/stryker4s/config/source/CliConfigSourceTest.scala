package stryker4s.config.source

import cats.effect.IO
import cats.syntax.all.*
import fs2.io.file.Path
import stryker4s.config.*
import stryker4s.testkit.Stryker4sIOSuite
import sttp.client3.*

import scala.concurrent.duration.*
import scala.meta.dialects

class CliConfigSourceTest extends Stryker4sIOSuite {

  test("should load a filled config") {
    val args = Seq(
      "--base-dir=/tmp/project",
      "--test-filter=foo",
      "--mutate=bar/src/main/**/*.scala,foo/src/main/**/*.scala,!excluded/file.scala",
      "--reporters=Html",
      "--excluded-mutations=BooleanLiteral",
      "--thresholds.high=85",
      "--thresholds.low=65",
      "--thresholds.break=10",
      "--timeout=5s",
      "--timeout-factor=2.5",
      "--max-test-runner-reuse=15",
      "--legacy-test-runner=true",
      "--scala-dialect=Scala212",
      "--concurrency=3",
      "--debug-test-runner=true",
      "--log-test-runner-stdout=true",
      "--static-tmp-dir=true",
      "--clean-tmp-dir=false",
      "--dashboard.base-url=https://fakeurl.com",
      "--dashboard.report-type=MutationScoreOnly",
      "--dashboard.project=someProject",
      "--dashboard.version=someVersion",
      "--dashboard.module=someModule"
    )
    val conf = new CliConfigSource[IO](args)

    conf.baseDir.load.assertEquals(Path("/tmp/project")) *>
      conf.mutate.load.assertEquals(
        Seq("bar/src/main/**/*.scala", "foo/src/main/**/*.scala", "!excluded/file.scala")
      ) *>
      conf.reporters.load.map(_.loneElement).assertEquals(Html) *>
      conf.excludedMutations.load.map(_.loneElement).assertEquals(ExcludedMutation("BooleanLiteral")) *>
      conf.thresholdsHigh.load.assertEquals(85) *>
      conf.thresholdsLow.load.assertEquals(65) *>
      conf.thresholdsBreak.load.assertEquals(10) *>
      conf.timeoutFactor.load.assertEquals(2.5) *>
      conf.timeout.load.assertEquals(5.seconds) *>
      conf.maxTestRunnerReuse.load.assertEquals(15.some) *>
      conf.legacyTestRunner.load.assertEquals(true) *>
      conf.scalaDialect.load.assertEquals(dialects.Scala212) *>
      conf.concurrency.load.assertEquals(3) *>
      conf.debugDebugTestRunner.load.assertEquals(true) *>
      conf.debugLogTestRunnerStdout.load.assertEquals(true) *>
      conf.staticTmpDir.load.assertEquals(true) *>
      conf.cleanTmpDir.load.assertEquals(false) *>
      conf.dashboardBaseUrl.load.assertEquals(uri"https://fakeurl.com") *>
      conf.dashboardReportType.load.assertEquals(MutationScoreOnly) *>
      conf.dashboardProject.load.assertEquals("someProject".some) *>
      conf.dashboardVersion.load.assertEquals("someVersion".some) *>
      conf.dashboardModule.load.assertEquals("someModule".some)
  }

  test("fails on unknown arguments") {
    val args = Seq("--unknown-argument=foo")
    val conf = new CliConfigSource[IO](args)

    conf.baseDir.attempt
      .map(_.leftValue.messages.loneElement)
      .assertEquals("Missing cli arg --base-dir")
  }

}
