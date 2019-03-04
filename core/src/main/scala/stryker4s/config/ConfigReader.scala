package stryker4s.config

import java.io.FileNotFoundException

import better.files.File
import grizzled.slf4j.Logging
import pureconfig.error.{CannotReadFile, ConfigReaderException, ConfigReaderFailures}
import stryker4s.config.implicits.ConfigReaderImplicits

object ConfigReader extends Logging with ConfigReaderImplicits {

  private[this] val defaultConfigFileLocation: File = File.currentWorkingDirectory / "stryker4s.conf")

  /** Read config from stryker4s.conf. Or use the default Config if no config file is found.
    */
  def readConfig(confFile: File = defaultConfigFileLocation): Config =
    readConfig[Config](confFile.path) match {
      case Left(failures) => tryRecoverFromFailures(failures)
      case Right(config) =>
        info("Using stryker4s.conf in the current working directory")

        config
    }

  def readConfig[T](confFile: File = defaultConfigFileLocation): Either[ConfigReaderFailures, T] = {
    pureconfig.loadConfig[T](confFile.path, namespace = "stryker4s")
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
