package stryker4s.config

import cats.syntax.option.*
import fs2.io.file.Path

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.meta.{dialects, Dialect}

// TODO: remove default values
/** Configuration for Stryker4s.
  *
  * Defaults are in [[stryker4s.config.source.DefaultsConfigSource]]
  */
final case class Config(
    mutate: Seq[String] = Seq.empty,
    testFilter: Seq[String] = Seq.empty,
    baseDir: Path = Path("").absolute,
    reporters: Seq[ReporterType] = Seq(Console, Html),
    files: Seq[String] = Seq.empty,
    excludedMutations: Seq[ExcludedMutation] = Seq.empty,
    thresholds: Thresholds = Thresholds(),
    dashboard: DashboardOptions = DashboardOptions(),
    timeout: FiniteDuration = FiniteDuration(5000, TimeUnit.MILLISECONDS),
    timeoutFactor: Double = 1.5,
    maxTestRunnerReuse: Option[Int] = none,
    legacyTestRunner: Boolean = false,
    scalaDialect: Dialect = dialects.Scala213Source3,
    concurrency: Int = Config.defaultConcurrency,
    debug: DebugOptions = DebugOptions(),
    staticTmpDir: Boolean = false,
    cleanTmpDir: Boolean = true
)

object Config {

  protected[config] def defaultConcurrency: Int = concurrencyFor(Runtime.getRuntime().availableProcessors())

  def concurrencyFor(cpuCoreCount: Int) = {
    // Use (n / 4 concurrency, rounded) + 1
    (cpuCoreCount.toDouble / 4).round.toInt + 1
  }

  lazy val default: Config = Config()
}
