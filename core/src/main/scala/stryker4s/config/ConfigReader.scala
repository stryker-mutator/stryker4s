package stryker4s.config

import java.io.FileNotFoundException

import better.files.File
import grizzled.slf4j.Logging
import pureconfig.error.{CannotReadFile, ConfigReaderException, ConfigReaderFailures, ConvertFailure, UnknownKey}
import pureconfig.generic.ProductHint
import pureconfig.{ConfigSource, Derivation, ConfigReader => PureConfigReader}
import stryker4s.config.implicits.ConfigReaderImplicits
import pureconfig.generic.auto._

object ConfigReader extends ConfigReaderImplicits with Logging {

  val defaultConfigFileLocation: File = File.currentWorkingDirectory / "stryker4s.conf"
  private val configDocUrl: String =
    "https://github.com/stryker-mutator/stryker4s/blob/master/docs/CONFIGURATION.md"

  /** Read config from stryker4s.conf. Or use the default Config if no config file is found.
    */
  def readConfig(confFile: File = defaultConfigFileLocation): Config = {
    implicit val hint: ProductHint[Config] = ProductHint[Config](allowUnknownKeys = false)
    def recoverUnknownKey: PartialFunction[ConfigReaderFailures, Config] = {
      case ConfigReaderFailures(ConvertFailure(UnknownKey(key), _, _), failures) =>
        val unknownKeys = key :: failures.collect {
          case ConvertFailure(UnknownKey(k), _, _) => k
        }
        warn(s"The following configuration keys are not used: ${unknownKeys.mkString(", ")}.\n" +
           s"Please check the documentation at $configDocUrl for available options.")
        nonStrictlyReadConfig(confFile)
    }

    recoverAndLog(
      recoverWith(readConfigOfType[Config](confFile))(recoverUnknownKey)
    )
  }

  def readConfigOfType[T](
      confFile: File = defaultConfigFileLocation
  )(implicit derivation: Derivation[PureConfigReader[T]]): Either[ConfigReaderFailures, T] =
    ConfigSource.file(confFile.path).at("stryker4s").load[T]

  private def recoverAndLog(either: Either[ConfigReaderFailures, Config]): Config =
    either.fold(
      tryRecoverFromFailures,
      logAndReturn(info("Using stryker4s.conf in the current working directory"))
    )

  private def nonStrictlyReadConfig(confFile: File): Config =
    recoverAndLog(
      readConfigOfType[Config](confFile)
    )

  private def tryRecoverFromFailures(failures: ConfigReaderFailures): Config = failures match {
    case ConfigReaderFailures(CannotReadFile(fileName, Some(_: FileNotFoundException)), _) =>
      warn(s"Could not find config file $fileName")
      warn("Using default config instead...")
      // FIXME: sbt has its own (older) dependency on Typesafe config, which causes an error with Pureconfig when running the sbt plugin
      //  If that's fixed we can add this again
      //  https://github.com/stryker-mutator/stryker4s/issues/116
      // info("Config used: " + defaultConf.toHoconString)

      Config()
    case _ =>
      error("Failures in reading config: ")
      error(failures.toList.map(_.description).mkString(System.lineSeparator))
      throw ConfigReaderException(failures)
  }

  private def recoverWith[A, B, BB >: B](eab: Either[A, B])(pf: PartialFunction[A, BB]): Either[A, BB] = eab match {
    case Left(a) if pf.isDefinedAt(a) => Right(pf(a))
    case _                            => eab
  }

  def logAndReturn[T](log: => Unit)(obj: T): T = {
    log
    obj
  }
}
