package stryker4s.config.ciris

import ciris.*
import com.typesafe.config.{Config, ConfigException, ConfigFactory, ConfigValue as HoconConfigValue}

import scala.jdk.CollectionConverters.*
import scala.util.Try
import cats.syntax.all.*

// From https://github.com/2m/ciris-hocon (not published for 2.12)
object Hocon extends HoconConfigDecoders {

  final class HoconAt(config: Config, path: String) {
    def apply(name: String): ConfigValue[Effect, HoconConfigValue] =
      Try(config.getValue(fullPath(name))).fold(
        {
          case _: ConfigException.Missing => ConfigValue.missing(key(name))
          case ex                         => ConfigValue.failed(ConfigError(ex.getMessage))
        },
        ConfigValue.loaded(key(name), _)
      )

    private def key(name: String) = ConfigKey(fullPath(name))
    private def fullPath(name: String) = s"$path.$name"
  }

  def hoconAt(path: String): HoconAt =
    hoconAt(ConfigFactory.load())(path)

  def hoconAt(config: Config)(path: String): HoconAt =
    new HoconAt(config.resolve(), path)
}

trait HoconConfigDecoders {
  implicit val stringHoconDecoder: ConfigDecoder[HoconConfigValue, String] =
    ConfigDecoder[HoconConfigValue].map(_.atKey("t").getString("t"))

  implicit def listHoconDecoder[T](implicit
      decoder: ConfigDecoder[HoconConfigValue, T]
  ): ConfigDecoder[HoconConfigValue, List[T]] =
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
