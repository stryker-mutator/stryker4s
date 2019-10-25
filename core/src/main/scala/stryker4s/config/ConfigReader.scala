package stryker4s.config

import java.io.FileNotFoundException

import better.files.File
import grizzled.slf4j.Logging
import pureconfig.error.{CannotReadFile, ConfigReaderException, ConfigReaderFailures, ConvertFailure, UnknownKey}
import pureconfig.generic.ProductHint
import pureconfig.{ConfigSource, Derivation, ConfigReader => PureConfigReader}
import stryker4s.config.implicits.ConfigReaderImplicits
import cats.syntax.either._
import pureconfig.generic.auto._

object ConfigReader extends ConfigReaderImplicits with Logging {


  val defaultConfigFileLocation: File = File.currentWorkingDirectory / "stryker4s.conf"

  private val configDocUrl: String =
    "https://github.com/stryker-mutator/stryker4s/blob/master/docs/CONFIGURATION.md"

  /** Read config from stryker4s.conf. Or use the default Config if no config file is found.
   */
  def readConfig(confFile: File = defaultConfigFileLocation): Config = {
    implicit val hint: ProductHint[Config] = ProductHint[Config](allowUnknownKeys = false)

    Reader[Config](confFile)
       .recoverWithDerivation(Failure.onUnknownKey)
       .recoverWith(Failure.onFileNotFound.andThen(_.asRight[ConfigReaderFailures]))
       .config
  }

  def readConfigOfType[T](
                            confFile: File = defaultConfigFileLocation
                         )(implicit derivation: Derivation[PureConfigReader[T]]): Either[ConfigReaderFailures, T] =
    Reader[T](confFile).tryRead

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
  private class Reader[T] private (file: File)(
     implicit derivation: Derivation[PureConfigReader[T]]) {

    private var onFailure: PartialFunction[ConfigReaderFailures, Reader.Result[T]] = PartialFunction.empty

    /**
    * Handle certain [[ConfigReaderFailures]] by providing a way to return a [[Reader.Result]]
     * if they occur
     */
    def recoverWith(pf: PartialFunction[ConfigReaderFailures, Reader.Result[T]]): Reader[T] = {
      this.onFailure = onFailure orElse pf
      this
    }

    /**
    * Handle certain [[ConfigReaderFailures]] by providing a different [[Derivation]] with
     * which the [[PureConfigReader]] should be configured.
     */
    def recoverWithDerivation(pf: PartialFunction[ConfigReaderFailures, Derivation[PureConfigReader[T]]]): Reader[T] = {

      def bar(d: Derivation[PureConfigReader[T]]): Reader.Result[T] = {
        val api = new Reader[T](file)(d)
        api.recoverWith(onFailure)
        api.tryRead
      }

      val foo: PartialFunction[ConfigReaderFailures, Reader.Result[T]] = {
        pf.andThen(bar)
      }
      recoverWith(foo)
    }

    /**
    * Force the reading of the config.
     * @note this will throw exceptions when a [[ConfigReaderFailures]] occurs for
     *       which no recover-strategy was defined,
     *
     */
    def config: T = tryRead.valueOr(Failure.throwException)

    /**
    * Attempt to read a config
     */
    def tryRead: Reader.Result[T] = {
      ConfigSource.file(file.path).at("stryker4s")
         .load[T]
         .recoverWith(onFailure)
         .map { c => info("Using stryker4s.conf in the current working directory"); c }
    }
  }

  private object Reader {

    type Result[T] = Either[ConfigReaderFailures, T]

    def apply[T](file: File)(implicit d: Derivation[PureConfigReader[T]]): Reader[T] =
      new Reader[T](file)
  }

  private object Failure {

    def onUnknownKey: PartialFunction[ConfigReaderFailures, Derivation[PureConfigReader[Config]]] = {
      case ConfigReaderFailures(ConvertFailure(UnknownKey(key), _, _), failures) =>
        val unknownKeys = key :: failures.collect {
          case ConvertFailure(UnknownKey(k), _, _) => k
        }

        warn(
          s"The following configuration keys are not used: ${unknownKeys.mkString(", ")}.\n" +
             s"Please check the documentation at $configDocUrl for available options."
        )
        implicit val hint: ProductHint[Config] = ProductHint[Config](allowUnknownKeys = true)
        implicitly[Derivation[PureConfigReader[Config]]]
    }

    def onFileNotFound: PartialFunction[ConfigReaderFailures, Config] = {
      case ConfigReaderFailures(CannotReadFile(fileName, Some(_: FileNotFoundException)), _) =>
        warn(s"Could not find config file $fileName")
        warn("Using default config instead...")
        // FIXME: sbt has its own (older) dependency on Typesafe config, which causes an error with Pureconfig when running the sbt plugin
        //  If that's fixed we can add this again
        //  https://github.com/stryker-mutator/stryker4s/issues/116
        // info("Config used: " + defaultConf.toHoconString)

        Config.default
    }

    def throwException[T](failures: ConfigReaderFailures) = {
        error("Failures in reading config: ")
        error(failures.toList.map(_.description).mkString(System.lineSeparator))

        throw ConfigReaderException(failures)
    }
  }
}