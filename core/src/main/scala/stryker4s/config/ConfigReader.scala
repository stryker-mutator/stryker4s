package stryker4s.config

import java.io.FileNotFoundException

import better.files.File
import grizzled.slf4j.Logging
import pureconfig.error.{CannotReadFile, ConfigReaderException, ConfigReaderFailures}
import pureconfig.generic.auto._
import stryker4s.config.implicits.ConfigReaderImplicits

object ConfigReader extends ConfigReaderImplicits with Logging {

  /** Read config from stryker4s.conf. Or use the default Config if no config file is found.
    */
  def readConfig(confFile: File = File.currentWorkingDirectory / "stryker4s.conf"): Config =
    pureconfig.loadConfig[Config](confFile.path, namespace = "stryker4s") match {
      case Left(failures) => tryRecoverFromFailures(failures)
      case Right(config) =>
        info("Using stryker4s.conf in the current working directory")

        config
    }

  private def tryRecoverFromFailures(failures: ConfigReaderFailures): Config = failures match {
    case ConfigReaderFailures(CannotReadFile(fileName, Some(_: FileNotFoundException)), _) =>
      val defaultConf = Config()

      warn(s"Could not find config file $fileName")
      warn("Using default config instead...")
      // FIXME: sbt has its own (older) dependency on Typesafe config, which causes an error with Pureconfig when running the sbt plugin
      //  If that's fixed we can add this again
      //  https://github.com/stryker-mutator/stryker4s/issues/116
      // info("Config used: " + defaultConf.toHoconString)

      defaultConf
    case _ =>
      error("Failures in reading config: ")
      error(failures.toList.map(_.description).mkString(System.lineSeparator))
      throw ConfigReaderException(failures)
  }
}
