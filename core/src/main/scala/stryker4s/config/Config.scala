package stryker4s.config

import better.files._

final case class Config(
    mutate: Seq[String] = Seq("**/main/scala/**.scala"),
    testFilter: Seq[String] = Seq(),
    baseDir: File = File.currentWorkingDirectory,
    reporters: Set[ReporterType] = Set(Console, Html),
    files: Option[Seq[String]] = None,
    excludedMutations: ExcludedMutations = ExcludedMutations(),
    thresholds: Thresholds = Thresholds(),
    dashboard: DashboardOptions = DashboardOptions()
)

object Config {
  lazy val default: Config = Config()
}
