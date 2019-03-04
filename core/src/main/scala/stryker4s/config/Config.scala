package stryker4s.config

import better.files._
import pureconfig.ConfigWriter
import stryker4s.run.report.{ConsoleReporter, MutantRunReporter}

case class Config(mutate: Seq[String] = Seq("**/main/scala/**/*.scala"),
                  baseDir: File = File.currentWorkingDirectory,
                  reporters: List[MutantRunReporter] = List(new ConsoleReporter),
                  files: Option[Seq[String]] = None,
                  excludedMutations: ExcludedMutations = ExcludedMutations(),
                  thresholds: Thresholds = Thresholds()) {

  def toHoconString: String = {
    import stryker4s.config.implicits.ConfigWriterImplicits._

    ConfigWriter[Config].to(this).render(options)
  }
}
