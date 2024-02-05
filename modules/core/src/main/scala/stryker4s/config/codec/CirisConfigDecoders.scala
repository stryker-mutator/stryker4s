package stryker4s.config.codec

import cats.syntax.all.*
import ciris.{ConfigDecoder, ConfigError, ConfigKey}
import fs2.io.file.Path
import stryker4s.config.*
import stryker4s.mutation.Mutation
import sttp.model.Uri

import scala.meta.{dialects, Dialect}

trait CirisConfigDecoders {
  implicit def pathDecoder: ConfigDecoder[String, Path] =
    ConfigDecoder[String].map(Path(_))

  implicit def seqDecoder[F[_], A, B](implicit decoder: ConfigDecoder[A, B]): ConfigDecoder[Seq[A], Seq[B]] =
    ConfigDecoder[Seq[A]].mapEither { case (key, list) =>
      list.toList.partitionEither(decoder.decode(key, _)) match {
        case (Nil, rights)       => Right(rights)
        case (firstLeft :: _, _) => Left(firstLeft)
      }
    }

  implicit def reporterDecoder: ConfigDecoder[String, ReporterType] = ConfigDecoder[String].mapEither {
    case (key, str) =>
      str.toLowerCase match {
        case "console"   => Console.asRight
        case "html"      => Html.asRight
        case "json"      => Json.asRight
        case "dashboard" => Dashboard.asRight
        case _           => ConfigError.decode("reporter", key, str).asLeft
      }
  }

  implicit def dashboardReportTypeDecoder: ConfigDecoder[String, DashboardReportType] =
    ConfigDecoder[String].mapEither { case (key, str) =>
      str.toLowerCase match {
        case "full"              => Full.asRight
        case "mutationScoreOnly" => MutationScoreOnly.asRight
        case "scoreOnly"         => MutationScoreOnly.asRight
        case _                   => ConfigError.decode("dashboard.reportType", key, str).asLeft
      }
    }

  implicit def exclusionsDecoder: ConfigDecoder[String, ExcludedMutation] =
    ConfigDecoder[String].mapEither { case (key, exclusion) =>
      if (Mutation.mutations.contains(exclusion.toLowerCase)) {
        ExcludedMutation(exclusion).asRight
      } else {
        val validExclusions = Mutation.mutations.mkString(", ")
        key match {
          case Some(k) =>
            ConfigError(
              s"invalid ${k.description} with value '$exclusion'. Valid exclusions are '$validExclusions'"
            ).asLeft
          case None =>
            ConfigError(
              s"invalid option '$exclusion'. Valid exclusions are '$validExclusions'"
            ).asLeft
        }
      }
    }

  implicit def uriDecoder: ConfigDecoder[String, Uri] = ConfigDecoder[String].mapEither { case (key, str) =>
    Uri.parse(str) match {
      case Right(uri) => uri.asRight
      case Left(msg)  => ConfigError.decode("uri", key, str).and(ConfigError(msg)).asLeft
    }
  }

  implicit def validateThresholds: ConfigDecoder[Thresholds, Thresholds] = {
    def isNotPercentage(n: Int) = n < 0 || n > 100

    def notPercentageError(value: Int, key: Option[ConfigKey]): Either[ConfigError, Thresholds] =
      ConfigError.decode(s"percentage", key, value).asLeft

    ConfigDecoder.instance[Thresholds, Thresholds] {
      case (key, Thresholds(high, _, _)) if isNotPercentage(high)   => notPercentageError(high, key)
      case (key, Thresholds(_, low, _)) if isNotPercentage(low)     => notPercentageError(low, key)
      case (key, Thresholds(_, _, break)) if isNotPercentage(break) => notPercentageError(break, key)
      case (key, Thresholds(high, low, _)) if high < low =>
        ConfigError
          .decode("thresholds.high", key, high)
          .and(
            ConfigError(
              s"'high' ($high) must be greater than or equal to 'low' ($low)"
            )
          )
          .asLeft
      case (key, Thresholds(_, low, break)) if low <= break =>
        ConfigError
          .decode("thresholds.low", key, low)
          .and(
            ConfigError(
              s"'low' ($low) must be greater than 'break' ($break)"
            )
          )
          .asLeft
      case (_, valid) => valid.asRight
    }
  }

  implicit def dialectReader: ConfigDecoder[String, Dialect] = {
    val deprecatedVersions = List("scala211", "scala2.11", "2.11", "211")

    val scalaVersions = Map(
      List("scala212", "scala2.12", "2.12", "212") -> dialects.Scala212,
      List("scala212source3") -> dialects.Scala212Source3,
      List("scala213", "scala2.13", "2.13", "213", "2") -> dialects.Scala213,
      List("scala213source3", "source3") -> dialects.Scala213Source3,
      List("scala3future", "future") -> dialects.Scala3Future,
      List("scala30", "scala3.0", "3.0", "30", "dotty") -> dialects.Scala30,
      List("scala31", "scala3.1", "3.1", "31") -> dialects.Scala31,
      List("scala32", "scala3.2", "3.2", "32") -> dialects.Scala32,
      List("scala33", "scala3.3", "3.3", "33") -> dialects.Scala33,
      List("scala3", "3") -> dialects.Scala3
    )

    ConfigDecoder[String].mapEither { case (key, input) =>
      def toCannotConvert(msg: String) = {
        val invalidDialectString =
          s"Leaving this configuration empty defaults to scala213source3 which might also work for you. Valid scalaDialects are: ${scalaVersions.keys.flatten
              .map(d => s"'$d'")
              .mkString(", ")}"
        ConfigError.decode("scala-dialect", key, input).and(ConfigError(s"$msg. $invalidDialectString"))
      }

      if (deprecatedVersions.contains(input))
        toCannotConvert("Deprecated dialect").asLeft
      else
        scalaVersions
          .collectFirst { case (strings, dialect) if strings.contains(input.toLowerCase()) => dialect }
          .toRight(toCannotConvert("Unsupported dialect"))
    }
  }
}
