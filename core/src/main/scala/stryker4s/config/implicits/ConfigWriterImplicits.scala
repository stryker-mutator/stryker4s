package stryker4s.config.implicits

import java.nio.file.Path

import better.files.File
import com.typesafe.config.ConfigRenderOptions
import pureconfig.ConfigWriter
import stryker4s.config.{ExcludedMutations, ReporterType}

object ConfigWriterImplicits {

  private[config] implicit val fileWriter: ConfigWriter[File] =
    ConfigWriter[Path] contramap (_.path)

  private[config] implicit val exclusionsWriter: ConfigWriter[ExcludedMutations] =
    ConfigWriter[List[String]] contramap (_.exclusions.toList)

  private[config] implicit val reportersWriter: ConfigWriter[ReporterType] =
    ConfigWriter[String] contramap (_.name)

  private[config] val options: ConfigRenderOptions = ConfigRenderOptions
    .defaults()
    .setOriginComments(false)
    .setJson(false)
}
