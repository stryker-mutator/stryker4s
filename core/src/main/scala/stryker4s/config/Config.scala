package stryker4s.config

import java.nio.file.Path

import better.files._
import com.typesafe.config.ConfigRenderOptions
import pureconfig.ConfigWriter

case class Config(files: Seq[String] = Seq("**/main/scala/**/*.scala"),
                  baseDir: File = File.currentWorkingDirectory,
                  testRunner: TestRunner = CommandRunner("sbt", "test")) {

  def toHoconString: String = {
    implicit val writer: ConfigWriter[File] = ConfigWriter[Path].contramap[File](c => c.path)
    val options = ConfigRenderOptions
      .defaults()
      .setOriginComments(false)
      .setJson(false)

    ConfigWriter[Config].to(this).render(options)
  }
}
