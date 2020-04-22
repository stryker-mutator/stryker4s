package stryker4s.config

import better.files._
import pureconfig.ConfigWriter
import pureconfig.generic.auto._

final case class Config(
    mutate: Seq[String] = Seq("**/main/scala/**.scala"),
    baseDir: File = File.currentWorkingDirectory,
    reporters: Set[ReporterType] = Set(Console, Html),
    files: Option[Seq[String]] = None,
    excludedMutations: ExcludedMutations = ExcludedMutations(),
    thresholds: Thresholds = Thresholds(),
    dashboard: DashboardOptions = DashboardOptions()
)

object Config {
  lazy val default: Config = Config()

  def toHoconString(config: Config): String = {
    import stryker4s.config.implicits.ConfigWriterImplicits._

    ConfigWriter[Config].to(config).render(options)
  }
}
