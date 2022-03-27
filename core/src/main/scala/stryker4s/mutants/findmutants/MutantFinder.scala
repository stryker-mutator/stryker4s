package stryker4s.mutants.findmutants

import cats.effect.IO
import cats.syntax.either.*
import fs2.io.file.Path
import stryker4s.config.Config
import stryker4s.extension.FileExtensions.*
import stryker4s.log.Logger

import scala.meta.parsers.XtensionParseInputLike
import scala.meta.{Dialect, Parsed, Source}

class MutantFinder()(implicit config: Config, log: Logger) {

  // TODO: Regex mutations
  // def findMutants(source: Source): (Seq[Mutant], Int) = {
  //   val (ignored, included) = source.collect(matcher.allMatchers).flatten.partitionEither(identity)
  //   val parseErrors = ignored.collect { case p: RegexParseError => p }
  //   parseErrors.foreach(p =>
  //     log.error(
  //       s"[RegexMutator]: The Regex parser of weapon-regex couldn't parse this regex pattern: '${p.pattern}'. Please report this issue at https://github.com/stryker-mutator/weapon-regex/issues. Inner error:",
  //       p.exception
  //     )
  //   )
  //   val excluded = ignored.count(_ == MutationExcluded)
  //   (included, excluded)
  // }

  def parseFile(file: Path): IO[Source] = {
    implicit val dialect: Dialect = config.scalaDialect

    IO(file.toNioPath.parse[Source]).flatMap {
      case e: Parsed.Error =>
        log.error(s"Error while parsing file '${file.relativePath}', ${e.message}")
        IO.raiseError(e.details)
      case s => IO.fromEither(s.toEither.leftMap(_.details))
    }

  }
}
