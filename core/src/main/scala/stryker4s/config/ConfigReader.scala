package stryker4s.config

import java.io.FileNotFoundException
import cats.syntax.either._
import grizzled.slf4j.Logging
import pureconfig.error._
import pureconfig.generic.ProductHint
import pureconfig.{ConfigSource, Derivation, ConfigReader => PureConfigReader}
import stryker4s.config.implicits.ConfigReaderImplicits
import pureconfig.generic.auto._

object ConfigReader extends ConfigReaderImplicits with Logging {
  private val configDocUrl: String =
    "https://github.com/stryker-mutator/stryker4s/blob/master/docs/CONFIGURATION.md"

  implicit val hint: ProductHint[Config] = ProductHint[Config](allowUnknownKeys = false)

  def readConfig(confSource: ConfigSource): Config = {
    Reader
      .withoutRecovery[Config](confSource)
      .recoverWithDerivation(Failure.onUnknownKey)
      .recoverWith(Failure.onFileNotFound.andThen(_.asRight[ConfigReaderFailures]))
      .config
  }

  def readConfigOfType[T](
      confSource: ConfigSource
  )(implicit derivation: Derivation[PureConfigReader[T]]): Either[ConfigReaderFailures, T] =
    Reader.withoutRecovery[T](confSource).tryRead

  /**
    * A configuration on how to attempt to read a config. The reason for its existence is to
    * provide a convenient way to attempt to read a config with various [[Derivation]]s,
    * depending on the [[ConfigReaderFailures]] that was returned from the last attempt.
    * In addition to that, some logging is also enforced during the reading process.
    * If not for those points, simply using the returned [[Either]] of the [[PureConfigReader]]
    * would be sufficient as well, since the exposed API here is basically a subset of [[Either]]s.
    * @param file the [[File]] from which the config is to be read.
    * @param derivation the [[Derivation]] that is used to configure the [[PureConfigReader]].
    * @tparam T the type of the config that is to be read.
    */
  private class Reader[T] private (
      configSource: ConfigSource,
      onFailure: PartialFunction[ConfigReaderFailures, Reader.Result[T]]
  )(implicit
      derivation: Derivation[PureConfigReader[T]]
  ) {

    /**
      * Handle certain [[ConfigReaderFailures]] by providing a way to return a [[Reader.Result]]
      * if they occur
      */
    def recoverWith(pf: PartialFunction[ConfigReaderFailures, Reader.Result[T]]): Reader[T] =
      new Reader[T](configSource, this.onFailure orElse pf)

    /**
      * Handle certain [[ConfigReaderFailures]] by providing a different [[Derivation]] with
      * which the [[PureConfigReader]] should be configured.
      */
    def recoverWithDerivation(pf: PartialFunction[ConfigReaderFailures, Derivation[PureConfigReader[T]]]): Reader[T] = {
      def setDerivation(d: Derivation[PureConfigReader[T]]): Reader.Result[T] = {
        val api = new Reader[T](configSource, this.onFailure)(d)
        api.tryRead
      }

      recoverWith(pf.andThen(setDerivation(_)))
    }

    /**
      * Force the reading of the config.
      * @note this will throw exceptions when a [[ConfigReaderFailures]] occurs for
      *       which no recover-strategy was defined,
      */
    def config: T = tryRead.valueOr(Failure.throwException)

    /**
      * Attempt to read a config
      */
    def tryRead: Reader.Result[T] = {
      info(s"Attempting to read config from stryker4s.conf")
      configSource
        .at("stryker4s")
        .load[T]
        .recoverWith(onFailure)
    }
  }

  private object Reader {
    type Result[T] = Either[ConfigReaderFailures, T]

    def withoutRecovery[T](configSource: ConfigSource)(implicit d: Derivation[PureConfigReader[T]]): Reader[T] =
      new Reader[T](configSource, PartialFunction.empty)
  }

  private object Failure {

    implicit val hint: ProductHint[Config] = ProductHint[Config](allowUnknownKeys = true)

    /**
      * When the config-parsing fails because of an unknown key in the configuration, a
      * derivation for the [[PureConfigReader]] is provided that does not fail
      * when unknown keys are present.
      * The names of the unknown keys are logged.
      */
    def onUnknownKey: PartialFunction[ConfigReaderFailures, Derivation[PureConfigReader[Config]]] = {
      case ConfigReaderFailures(ConvertFailure(UnknownKey(key), _, _), failures @ _*) =>
        val unknownKeys = key +: failures.collect {
          case ConvertFailure(UnknownKey(k), _, _) => k
        }

        warn(
          s"The following configuration key(s) are not used, they could stem from an older " +
            s"stryker4s version: '${unknownKeys.mkString(", ")}'.\n" +
            s"Please check the documentation at $configDocUrl for available options."
        )
        implicitly[Derivation[PureConfigReader[Config]]]
    }

    /**
      * When the config-parsing fails because no file is found at the specified location,
      * a default config is provided.
      */
    def onFileNotFound: PartialFunction[ConfigReaderFailures, Config] = {
      case ConfigReaderFailures(CannotReadFile(fileName, Some(_: FileNotFoundException)), _*) =>
        warn(s"Could not find config file $fileName")
        warn("Using default config instead...")
        debug("Config used: " + Config.default)

        Config.default
    }

    /**
      * Throw a [[ConfigReaderException]] and log the encountered failures.
      */
    def throwException[T](failures: ConfigReaderFailures): Nothing = {
      error("Failures in reading config: ")
      error(failures.toList.map(_.description).mkString(System.lineSeparator))

      throw ConfigReaderException(failures)
    }
  }
}
