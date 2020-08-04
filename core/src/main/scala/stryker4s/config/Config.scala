package stryker4s.config

import better.files._

final case class Config(
    mutate: Seq[String] = Seq("**/main/scala/**.scala"),
    testFilter: Seq[String] = Seq.empty,
    baseDir: File = File.currentWorkingDirectory,
    reporters: Set[ReporterType] = Set(Console, Html),
    files: Option[Seq[String]] = None,
    excludedMutations: Config.ExcludedMutations = Set.empty,
    thresholds: Thresholds = Thresholds(),
    dashboard: DashboardOptions = DashboardOptions()
)

object Config {

  /** Type alias for `Set[String]` so extra validation can be done
    */
  type ExcludedMutations = Set[String]

  lazy val default: Config = Config()
}
