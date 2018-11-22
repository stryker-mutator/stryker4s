package stryker4s.config.implicits
import java.nio.file.Path

import better.files.File
import com.typesafe.config.ConfigRenderOptions
import org.apache.logging.log4j.Level
import pureconfig.ConfigWriter
import stryker4s.mutants.Exclusions
import stryker4s.run.report.{ConsoleReporter, MutantRunReporter}

object ConfigWriterImplicits {

  private[config] implicit val fileWriter: ConfigWriter[File] = ConfigWriter[Path].contramap[File](_.path)
  private[config] implicit val logLevelWriter: ConfigWriter[Level] =
    ConfigWriter[String].contramap[Level](_.toString)
  private[config] implicit val exclusionsWriter: ConfigWriter[Exclusions] =
    ConfigWriter[List[String]].contramap(_.exclusions.toList)

  private[config] implicit val reportersWriter: ConfigWriter[List[MutantRunReporter]] = ConfigWriter[List[String]]
    .contramap(mutantRunReporters =>
      mutantRunReporters.map {
        case _: ConsoleReporter => MutantRunReporter.consoleReporter
    })

  private[config] implicit val options: ConfigRenderOptions = ConfigRenderOptions
    .defaults()
    .setOriginComments(false)
    .setJson(false)
}
