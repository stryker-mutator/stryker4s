package stryker4s.config

import java.nio.file.Path

import better.files._
import com.typesafe.config.ConfigRenderOptions
import pureconfig.ConfigWriter
import stryker4s.run.report.{ConsoleReporter, MutantRunReporter}

case class Config(files: Seq[String] = Seq("**/main/scala/**/*.scala"),
                  baseDir: File = File.currentWorkingDirectory,
                  testRunner: TestRunner = CommandRunner("sbt", "test"),
                  reporters: List[MutantRunReporter] = List(new ConsoleReporter)) {

  def toHoconString: String = {
    implicit val fileWriter: ConfigWriter[File] = ConfigWriter[Path].contramap[File](file => file.path)
    implicit val reportersWriter: ConfigWriter[List[MutantRunReporter]] = ConfigWriter[List[String]]
      .contramap(mutantRunReporters => mutantRunReporters.map {
        case _: ConsoleReporter => MutantRunReporter.consoleReporter
      })

    val options = ConfigRenderOptions
      .defaults()
      .setOriginComments(false)
      .setJson(false)

    ConfigWriter[Config].to(this).render(options)
  }
}
