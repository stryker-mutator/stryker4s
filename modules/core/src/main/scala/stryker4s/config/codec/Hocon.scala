package stryker4s.config.codec

import cats.syntax.all.*
import ciris.*
import com.typesafe.config.{Config, ConfigException, ConfigFactory, ConfigValue as HoconConfigValue}
import fs2.io.file.Path

import scala.jdk.CollectionConverters.*
import scala.util.Try

// From https://github.com/2m/ciris-hocon (not published for 2.12)
object Hocon extends HoconConfigDecoders {

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

  def hoconAt(path: String, filePath: Path): HoconAt =
    hoconAt(ConfigFactory.load())(path, filePath)

  def hoconAt(config: Config)(path: String, filePath: Path): HoconAt =
    new HoconAt(config.resolve(), path, filePath)
}

trait HoconConfigDecoders {
  implicit val stringHoconDecoder: ConfigDecoder[HoconConfigValue, String] =
    ConfigDecoder[HoconConfigValue].map(_.atKey("t").getString("t"))

  implicit def listHoconDecoder[T](implicit
      decoder: ConfigDecoder[HoconConfigValue, T]
  ): ConfigDecoder[HoconConfigValue, Seq[T]] =
    ConfigDecoder[HoconConfigValue]
      .map(_.atKey("t").getList("t").asScala.toList)
      .mapEither { (key, list) =>
        list.partitionEither(decoder.decode(key, _)) match {
          case (Nil, rights)       => Right(rights)
          case (firstLeft :: _, _) => Left(firstLeft)
        }
      }

  implicit val javaTimeDurationHoconDecoder: ConfigDecoder[HoconConfigValue, java.time.Duration] =
    ConfigDecoder[HoconConfigValue].map(_.atKey("t").getDuration("t"))

  implicit val javaPeriodHoconDecoder: ConfigDecoder[HoconConfigValue, java.time.Period] =
    ConfigDecoder[HoconConfigValue].map(_.atKey("t").getPeriod("t"))

  implicit def throughStringHoconDecoder[T](implicit d: ConfigDecoder[String, T]): ConfigDecoder[HoconConfigValue, T] =
    stringHoconDecoder.as[T]
}
