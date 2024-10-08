package stryker4s.config.codec

import cats.syntax.all.*
import ciris.*
import com.typesafe.config.{Config, ConfigException, ConfigValue as HoconConfigValue}
import fs2.io.file.Path

import scala.jdk.CollectionConverters.*
import scala.util.Try

// From https://github.com/2m/ciris-hocon (not published for 2.12)
object Hocon {

  final class HoconAt(config: Config, path: String, filePath: Path) {
    def apply(name: String): ConfigValue[Effect, HoconConfigValue] =
      Try(config.getValue(fullPath(name))).fold(
        {
          case _: ConfigException.Missing => ConfigValue.missing(s"${fullPath(name)} in $filePath")
          case ex                         => ConfigValue.failed(ConfigError(ex.getMessage))
        },
        ConfigValue.loaded(key(name), _)
      )

    private def key(name: String) = ConfigKey(fullPath(name))
    private def fullPath(name: String) = s"$path.$name"
  }

  def hoconAt(config: Config)(path: String, filePath: Path): HoconAt =
    new HoconAt(config.resolve(), path, filePath)
}

/** Lets Ciris decode HoconConfigValues into various types
  */
trait HoconConfigDecoders {
  implicit val stringHoconDecoder: ConfigDecoder[HoconConfigValue, String] =
    ConfigDecoder[HoconConfigValue].map(_.atKey("t").getString("t"))

  implicit def listHoconDecoder[T](implicit
      decoder: ConfigDecoder[HoconConfigValue, T]
  ): ConfigDecoder[HoconConfigValue, Seq[T]] =
    ConfigDecoder[HoconConfigValue]
      .map(_.atKey("t").getList("t").asScala.toList)
      .mapEither { (key, values) =>
        values.partitionEither(decoder.decode(key, _)) match {
          case (Nil, decodedValues) => decodedValues.distinct.asRight
          case (failures, _)        => failures.reduce(_ and _).asLeft
        }
      }

  implicit def throughStringHoconDecoder[T](implicit d: ConfigDecoder[String, T]): ConfigDecoder[HoconConfigValue, T] =
    stringHoconDecoder.as[T]
}
