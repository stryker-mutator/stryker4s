package stryker4s.config

import better.files._
import pureconfig.ConfigWriter
import pureconfig.generic.auto._

case class Config(mutate: Seq[String] = Seq("**/main/scala/**/*.scala"),
                  baseDir: File = File.currentWorkingDirectory,
                  reporters: Seq[ReporterType] = Seq(ConsoleReporterType),
                  files: Option[Seq[String]] = None,
                  excludedMutations: ExcludedMutations = ExcludedMutations(),
                  thresholds: Thresholds = Thresholds()) {

  def toHoconString: String = {
    import stryker4s.config.implicits.ConfigWriterImplicits._

    ConfigWriter[Config].to(this).render(options)
  }
}
