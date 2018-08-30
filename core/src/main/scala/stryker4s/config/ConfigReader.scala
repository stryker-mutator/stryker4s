package stryker4s.config

import java.io.FileNotFoundException
import java.nio.file.Path

import better.files.File
import grizzled.slf4j.Logging
import pureconfig.error._
import pureconfig.{ConfigReader => PConfigReader}
object ConfigReader extends Logging {

  /** Converts a [[java.nio.file.Path]] to a [[better.files.File]] so PureConfig can read it
    *
    */
  private implicit val toFileReader: PConfigReader[File] = PConfigReader[Path].map(p => File(p))

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
      warn(s"Could not find config file $fileName")
      warn("Using default config instead...")
      val defaultConf = Config()
      debug("Config used: " + defaultConf.toHoconString)
      defaultConf
    case _ =>
      error("Failures in reading config: ")
      error(failures.toList.map(_.description).mkString(System.lineSeparator))
      throw ConfigReaderException(failures)
  }
}
