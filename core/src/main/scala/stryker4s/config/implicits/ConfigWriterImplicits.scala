package stryker4s.config.implicits

import java.nio.file.Path

import better.files.File
import com.typesafe.config.ConfigRenderOptions
import org.apache.logging.log4j.Level
import pureconfig.ConfigWriter
import stryker4s.config.ExcludedMutations
import stryker4s.run.report.{ConsoleReporter, MutantRunReporter}

object ConfigWriterImplicits {

  private[config] implicit val fileWriter: ConfigWriter[File] =
    ConfigWriter[Path] contramap (_.path)

  private[config] implicit val logLevelWriter: ConfigWriter[Level] =
    ConfigWriter[String] contramap (_.toString)

  private[config] implicit val exclusionsWriter: ConfigWriter[ExcludedMutations] =
    ConfigWriter[List[String]] contramap (_.exclusions.toList)

  private[config] implicit val reportersWriter: ConfigWriter[MutantRunReporter] =
    ConfigWriter[String] contramap {
      case _: ConsoleReporter => MutantRunReporter.consoleReporter
    }

  private[config] val options: ConfigRenderOptions = ConfigRenderOptions
    .defaults()
    .setOriginComments(false)
    .setJson(false)
}
