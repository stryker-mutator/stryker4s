package stryker4s.config

import cats.effect.IO
import fs2.io.file.Path
import stryker4s.config.source.DefaultsConfigSource
import stryker4s.run.process.Command

import scala.concurrent.duration.FiniteDuration
import scala.meta.Dialect

/** Configuration for Stryker4s.
  *
  * Defaults are in [[stryker4s.config.source.DefaultsConfigSource]]
  */
final case class Config(
    mutate: Seq[String],
    testFilter: Seq[String],
    baseDir: Path,
    reporters: Seq[ReporterType],
    files: Seq[String],
    excludedMutations: Seq[ExcludedMutation],
    thresholds: Thresholds,
    dashboard: DashboardOptions,
    timeout: FiniteDuration,
    timeoutFactor: Double,
    maxTestRunnerReuse: Option[Int],
    legacyTestRunner: Boolean,
    scalaDialect: Dialect,
    concurrency: Int,
    debug: DebugOptions,
    staticTmpDir: Boolean,
    cleanTmpDir: Boolean,
    testRunner: Command,
    openReport: Boolean
)

object Config {

  protected[config] def defaultConcurrency: Int = concurrencyFor(Runtime.getRuntime().availableProcessors())

  def concurrencyFor(cpuCoreCount: Int) = {
    // Use (n / 4 concurrency, rounded) + 1
    (cpuCoreCount.toDouble / 4).round.toInt + 1
  }

  // Only used in tests
  lazy val default: Config = {
    import cats.effect.unsafe.implicits.global
    // We need to run this as IO because .load needs Async
    new ConfigLoader(new DefaultsConfigSource[IO]()).config.load.unsafeRunSync()
  }
}
