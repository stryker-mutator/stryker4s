package stryker4s.config

import java.nio.file.Path

import better.files._
import ch.qos.logback.classic.Level
import com.typesafe.config.ConfigRenderOptions
import pureconfig.ConfigWriter
import stryker4s.run.report.{ConsoleReporter, MutantRunReporter}

case class Config(mutate: Seq[String] = Seq("**/main/scala/**/*.scala"),
                  baseDir: File = File.currentWorkingDirectory,
                  testRunner: TestRunner = CommandRunner("sbt", "test"),
                  reporters: List[MutantRunReporter] = List(new ConsoleReporter),
                  logLevel: Level = Level.INFO,
                  files: Option[Seq[String]] = None) {

  def toHoconString: String = {
    implicit val fileWriter: ConfigWriter[File] = ConfigWriter[Path].contramap[File](_.path)
    implicit val logLevelWriter: ConfigWriter[Level] = ConfigWriter[String].contramap[Level](_.toString)
    implicit val reportersWriter: ConfigWriter[List[MutantRunReporter]] = ConfigWriter[List[String]]
      .contramap(mutantRunReporters =>
        mutantRunReporters.map {
          case _: ConsoleReporter => MutantRunReporter.consoleReporter
      })

    val options = ConfigRenderOptions
      .defaults()
      .setOriginComments(false)
      .setJson(false)

    ConfigWriter[Config].to(this).render(options)
  }
}
