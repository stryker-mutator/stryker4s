package stryker4s.config.pure

import java.nio.file.Path

import scala.meta.Dialect
import scala.meta.dialects._

import better.files.File
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert
import pureconfig.generic.semiauto._
import stryker4s.config._
import stryker4s.extension.mutationtype.Mutation

/** Conversions of custom case classes or enums so PureConfig can read it.
  * @example `toFileReader` makes PureConfig able to read `better.files.File` from a `java.nio.file.Path`
  */
trait ConfigConfigReader {

  implicit def fileReader: ConfigReader[File] =
    ConfigReader[Path] map (p => File(p))

  implicit def reporterReader: ConfigReader[ReporterType] =
    deriveEnumerationReader[ReporterType]

  implicit def dashboardReportTypeReader: ConfigReader[DashboardReportType] =
    deriveEnumerationReader[DashboardReportType]

  implicit def exclusionsReader: ConfigReader[Config.ExcludedMutations] =
    ConfigReader[List[String]] emap { exclusions =>
      val (valid, invalid) = exclusions.partition(Mutation.mutations.contains)
      if (invalid.nonEmpty)
        Left(
          CannotConvert(
            exclusions.mkString(", "),
            s"excluded-mutations",
            s"invalid option(s) '${invalid.mkString(", ")}'. Valid exclusions are '${Mutation.mutations.mkString(", ")}'"
          )
        )
      else
        Right(valid.toSet)
    }

  implicit def uriReader = _root_.pureconfig.module.sttp.reader

  implicit def thresholdsReader: ConfigReader[Thresholds] = {
    def isNotPercentage(n: Int) = n < 0 || n > 100

    def notPercentageError(value: Int, name: String): Left[CannotConvert, Thresholds] =
      Left(CannotConvert(value.toString(), s"thresholds.$name", "must be a percentage 0-100"))

    deriveReader[Thresholds] emap {
      case Thresholds(high, _, _) if isNotPercentage(high)   => notPercentageError(high, "high")
      case Thresholds(_, low, _) if isNotPercentage(low)     => notPercentageError(low, "low")
      case Thresholds(_, _, break) if isNotPercentage(break) => notPercentageError(break, "break")
      case Thresholds(high, low, _) if high < low =>
        Left(
          CannotConvert(
            high.toString(),
            "thresholds.high",
            s"'high' ($high) must be greater than or equal to 'low' ($low)"
          )
        )
      case Thresholds(_, low, break) if low <= break =>
        Left(
          CannotConvert(
            low.toString(),
            "thresholds.low",
            s"'low' ($low) must be greater than 'break' ($break)"
          )
        )
      case valid => Right(valid)
    }
  }

  implicit def dialectReader: ConfigReader[Dialect] = {
    val scalaVersions = Map(
      List("scala211", "scala2.11", "2.11", "211") -> Scala211,
      List("scala212", "scala2.12", "2.12", "212") -> Scala212,
      List("scala213", "scala2.13", "2.13", "213", "2") -> Scala213,
      List("scala3", "scala3.0", "3.0", "3", "dotty") -> Scala3
    )
    ConfigReader[String].emap { input =>
      scalaVersions
        .collectFirst { case (strings, dialect) if strings.contains(input.toLowerCase()) => dialect }
        .toRight(
          CannotConvert(
            input,
            "scalaDialect",
            s"Unsupported scalaDialect. Leaving this configuration empty defaults to scala3 which might also work for you. Valid scalaDialects are: ${scalaVersions
              .flatMap(_._1)
              .map(d => s"'$d'")
              .mkString(", ")}."
          )
        )
    }
  }

}
