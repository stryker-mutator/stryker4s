package stryker4s.config.source

import cats.effect.IO
import cats.syntax.all.*
import fs2.io.file.Path
import stryker4s.config.*
import stryker4s.testkit.Stryker4sIOSuite
import sttp.client4.UriContext

import scala.concurrent.duration.*
import scala.meta.dialects

class DefaultsConfigSourceTest extends Stryker4sIOSuite {
  test("loads all default values") {
    val conf = new DefaultsConfigSource[IO]()

    conf.mutate.load.assertEquals(Seq("**/main/scala/**.scala")) *>
      conf.testFilter.load.assertEquals(Seq.empty) *>
      conf.baseDir.load.assertEquals(Path("").absolute) *>
      conf.reporters.load.assertSameElementsAs(Seq(Console, Html)) *>
      conf.files.load.assertEquals(Seq("**", "!target/**", "!project/**", "!.metals/**", "!.bloop/**", "!.idea/**")) *>
      conf.excludedMutations.load.assertEquals(Seq.empty) *>
      conf.thresholdsHigh.load.assertEquals(80) *>
      conf.thresholdsLow.load.assertEquals(60) *>
      conf.thresholdsBreak.load.assertEquals(0) *>
      conf.dashboardBaseUrl.load.assertEquals(uri"https://dashboard.stryker-mutator.io") *>
      conf.dashboardReportType.load.assertEquals(Full) *>
      conf.dashboardProject.load.assertEquals(none) *>
      conf.dashboardVersion.load.assertEquals(none) *>
      conf.dashboardModule.load.assertEquals(none) *>
      conf.timeout.load.assertEquals(5.seconds) *>
      conf.timeoutFactor.load.assertEquals(1.5) *>
      conf.maxTestRunnerReuse.load.assertEquals(none) *>
      conf.legacyTestRunner.load.assertEquals(false) *>
      conf.scalaDialect.load.assertEquals(dialects.Scala213Source3) *>
      conf.concurrency.load.assertEquals(Config.defaultConcurrency) *>
      conf.debugLogTestRunnerStdout.load.assertEquals(false) *>
      conf.debugDebugTestRunner.load.assertEquals(false) *>
      conf.staticTmpDir.load.assertEquals(false) *>
      conf.cleanTmpDir.load.assertEquals(true) *>
      conf.testRunnerCommand.load.assertEquals("sbt") *>
      conf.testRunnerArgs.load.assertEquals("test")
  }

}
