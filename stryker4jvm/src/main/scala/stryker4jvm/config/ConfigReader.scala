package stryker4jvm.config

import cats.syntax.either.*
import fansi.{Color, Underlined}
import pureconfig.error.*
import pureconfig.generic.ProductHint
import pureconfig.generic.auto.*
import pureconfig.{ConfigReader as PureConfigReader, ConfigSource}
import stryker4jvm.config.Config.*
import stryker4jvm.logging.FansiLogger

import java.io.FileNotFoundException

object ConfigReader {
  private val configDocUrl: String =
    "https://stryker-mutator.io/docs/stryker4s/configuration"

  implicit val hint: ProductHint[Config] = ProductHint[Config](allowUnknownKeys = false)

  def readConfig(confSource: ConfigSource = ConfigSource.file("stryker4s.conf"))(implicit log: FansiLogger): Config = {
    Reader
      .withoutRecovery[Config](confSource)
      .recoverWithReader(Failure.onUnknownKey)
      .recoverWith(Failure.onFileNotFound.andThen(_.asRight[ConfigReaderFailures]))
      .config
  }

  def readConfigOfType[T](
      confSource: ConfigSource = ConfigSource.file("stryker4s.conf")
  )(implicit log: FansiLogger, pureconfig: PureConfigReader[T]): Either[ConfigReaderFailures, T] =
    Reader.withoutRecovery[T](confSource).tryRead

  /** A configuration on how to attempt to read a config. The reason for its existence is to provide a convenient way to
    * attempt to read a config with various `PureConfigReader`s, depending on the [[ConfigReaderFailures]] that was
    * returned from the last attempt. In addition to that, some logging is also enforced during the reading process. If
    * not for those points, simply using the returned [[Either]] of the [[PureConfigReader]] would be sufficient as
    * well, since the exposed API here is basically a subset of [[Either]] s.
    *
    * @param file
    *   the [[File]] from which the config is to be read.
    * @param pureconfig
    *   [[PureConfigReader]] to read [[T]]
    * @tparam T
    *   the type of the config that is to be read.
    */
  private class Reader[T] private (
      configSource: ConfigSource,
      onFailure: PartialFunction[ConfigReaderFailures, Reader.Result[T]]
  )(implicit
      log: FansiLogger,
      pureconfig: PureConfigReader[T]
  ) {

    /** Handle certain [[ConfigReaderFailures]] by providing a way to return a [[Reader.Result]] if they occur
      */
    def recoverWith(pf: PartialFunction[ConfigReaderFailures, Reader.Result[T]]): Reader[T] =
      new Reader[T](configSource, this.onFailure orElse pf)

    /** Handle certain [[ConfigReaderFailures]] by providing a different [[PureConfigReader]]
      */
    def recoverWithReader(pf: PartialFunction[ConfigReaderFailures, PureConfigReader[T]]): Reader[T] = {
      def setReader(d: PureConfigReader[T]): Reader.Result[T] = {
        val api = new Reader[T](configSource, this.onFailure)(log, d)
        api.tryRead
      }

      recoverWith(pf.andThen(setReader(_)))
    }

    /** Force the reading of the config.
      * @note
      *   this will throw exceptions when a [[ConfigReaderFailures]] occurs for which no recover-strategy was defined,
      */
    def config: T = tryRead.valueOr(Failure.throwException)

    /** Attempt to read a config
      */
    def tryRead: Reader.Result[T] = {
      log.info(s"Attempting to read config from ${Underlined.On("stryker4s.conf")}")
      configSource
        .at("stryker4s")
        .load[T]
        .recoverWith(onFailure)
    }
  }

  private object Reader {
    type Result[T] = Either[ConfigReaderFailures, T]

    def withoutRecovery[T](
        configSource: ConfigSource
    )(implicit log: FansiLogger, d: PureConfigReader[T]): Reader[T] =
      new Reader[T](configSource, PartialFunction.empty)
  }

  private object Failure {

    implicit val hint: ProductHint[Config] = ProductHint[Config](allowUnknownKeys = true)

    /** When the config-parsing fails because of an unknown key in the configuration, a [[PureConfigReader]] is provided
      * that does not fail when unknown keys are present. The names of the unknown keys are logged.
      */
    def onUnknownKey(implicit
        log: FansiLogger
    ): PartialFunction[ConfigReaderFailures, PureConfigReader[Config]] = {
      case ConfigReaderFailures(ConvertFailure(UnknownKey(key), _, _), failures*) =>
        val unknownKeys = key +: failures.collect { case ConvertFailure(UnknownKey(k), _, _) => k }

        log.warn(
          s"""The following configuration key(s) are not used, they could stem from an older stryker4s version: '${fansi.Str
              .join(unknownKeys.map(Color.Yellow(_)), ", ")}'.
             |Please check the documentation at $configDocUrl for available options.""".stripMargin
        )
        implicitly[PureConfigReader[Config]]
    }

    /** When the config-parsing fails because no file is found at the specified location, a default config is provided.
      */
    def onFileNotFound(implicit log: FansiLogger): PartialFunction[ConfigReaderFailures, Config] = {
      case ConfigReaderFailures(CannotReadFile(fileName, Some(_: FileNotFoundException)), _*) =>
        log.warn(s"Could not find config file $fileName")
        log.warn("Using default config instead...")
        log.debug("Config used: " + Config.default)

        Config.default
    }

    /** Throw a [[ConfigReaderException]] and log the encountered failures.
      */
    def throwException[T](failures: ConfigReaderFailures)(implicit log: FansiLogger): Nothing = {
      log.error("Failures in reading config:")
      log.error(failures.toList.map(_.description).mkString(System.lineSeparator))

      throw ConfigReaderException[Config](failures)
    }
  }
}
