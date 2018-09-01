package stryker4s.config

import java.nio.file.Path

import better.files._
import ch.qos.logback.classic.Level
import com.typesafe.config.ConfigRenderOptions
import pureconfig.ConfigWriter

case class Config(files: Seq[String] = Seq("**/main/scala/**/*.scala"),
                  baseDir: File = File.currentWorkingDirectory,
                  testRunner: TestRunner = CommandRunner("sbt", "test"),
                  logLevel: Level = Level.INFO) {

  // Support parsing logback.classic.Level logging levels by using their string representation
  // Note: Defaults to DEBUG if the value cannot be parsed
  implicit def logLevelWriter: ConfigWriter[Level] =
    ConfigWriter[String].contramap[Level](b => b.toString)
  implicit val writer: ConfigWriter[File] = ConfigWriter[Path].contramap[File](c => c.path)

  def toHoconString: String = {
    val options = ConfigRenderOptions
      .defaults()
      .setOriginComments(false)
      .setJson(false)

    ConfigWriter[Config].to(this).render(options)
  }
}
