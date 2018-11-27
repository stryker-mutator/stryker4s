package stryker4s.config

import better.files._
import org.apache.logging.log4j.Level
import pureconfig.ConfigWriter
import stryker4s.mutants.Exclusions
import stryker4s.run.report.{ConsoleReporter, MutantRunReporter}

case class Config(mutate: Seq[String] = Seq("**/main/scala/**/*.scala"),
                  baseDir: File = File.currentWorkingDirectory,
                  testRunner: TestRunner = CommandRunner("sbt", "test"),
                  reporters: List[MutantRunReporter] = List(new ConsoleReporter),
                  logLevel: Level = Level.INFO,
                  files: Option[Seq[String]] = None,
                  excludedMutations: Exclusions = Exclusions(Set.empty),
                  thresholds: Option[Thresholds] = None) {

  def toHoconString: String = {
    import stryker4s.config.implicits.ConfigWriterImplicits._

    ConfigWriter[Config].to(this).render(options)
  }
}
