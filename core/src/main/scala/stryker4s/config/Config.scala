package stryker4s.config

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.FiniteDuration
import scala.meta.{dialects, Dialect}

import better.files._

final case class Config(
    mutate: Seq[String] = Seq("**/main/scala/**.scala"),
    testFilter: Seq[String] = Seq.empty,
    baseDir: File = File.currentWorkingDirectory,
    reporters: Set[ReporterType] = Set(Console, Html),
    files: Option[Seq[String]] = None,
    excludedMutations: Config.ExcludedMutations = Set.empty,
    thresholds: Thresholds = Thresholds(),
    dashboard: DashboardOptions = DashboardOptions(),
    timeout: FiniteDuration = FiniteDuration(5000, TimeUnit.MILLISECONDS),
    timeoutFactor: Double = 1.5,
    maxTestRunnerReuse: Option[Int] = None,
    legacyTestRunner: Boolean = false,
    scalaDialect: Dialect = dialects.Scala3,
    concurrency: Int = Config.defaultConcurrency
)

object Config extends pure.ConfigConfigReader with circe.ConfigEncoder {

  private def defaultConcurrency: Int = {
    // Use n - 1 threads unless this machine has a low thread-count 🛏️
    val cpus = Runtime.getRuntime().availableProcessors()
    if (cpus > 4) cpus - 1
    else cpus
  }

  /** Type alias for `Set[String]` so extra validation can be done
    */
  type ExcludedMutations = Set[String]

  lazy val default: Config = Config()
}
