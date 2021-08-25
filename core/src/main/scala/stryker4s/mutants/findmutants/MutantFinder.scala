package stryker4s.mutants.findmutants

import cats.effect.IO
import cats.implicits._
import fs2.io.file.Path
import stryker4s.config.Config
import stryker4s.extension.FileExtensions._
import stryker4s.log.Logger
import stryker4s.model.{Mutant, MutantId, MutationExcluded, MutationsInSource, RegexParseError}

import scala.meta.parsers.XtensionParseInputLike
import scala.meta.{Dialect, Parsed, Source}

class MutantFinder(matcher: MutantMatcher)(implicit config: Config, log: Logger) {
  def mutantsInFile(filePath: Path): IO[MutationsInSource] = for {
    parsedSource <- parseFile(filePath)
    (included, excluded) <- IO(findMutants(parsedSource))
  } yield MutationsInSource(parsedSource, addFileInfo(filePath.toString, included), excluded)

  def addFileInfo(filePath: String, mutants: Seq[Mutant]): Seq[Mutant] = {
    mutants.zipWithIndex.map { case (mut, numInFile) =>
      //Assign the file and the index of the mutation in the file to the MutantId
      //This is used later to trace potential compiler errors back to the mutation
      mut.copy(id = MutantId(mut.id.globalId, filePath, numInFile))
    }
  }

  def findMutants(source: Source): (Seq[Mutant], Int) = {
    val (ignored, included) = source.collect(matcher.allMatchers).flatten.partitionEither(identity)
    val parseErrors = ignored.collect { case p: RegexParseError => p }
    parseErrors.foreach(p =>
      log.error(
        s"[RegexMutator]: The Regex parser of weapon-regex couldn't parse this regex pattern: '${p.original}'. Please report this issue at https://github.com/stryker-mutator/weapon-regex/issues. Inner error:",
        p.exception
      )
    )
    val excluded = ignored.collect { case m: MutationExcluded => m }
    (included, excluded.size)
  }

  def parseFile(file: Path): IO[Source] = {
    implicit val dialect: Dialect = config.scalaDialect

    IO(file.toNioPath.parse[Source]).flatMap {
      case e: Parsed.Error =>
        log.error(s"Error while parsing file '${file.relativePath}', ${e.message}")
        IO.raiseError(e.details)
      case s => IO.pure(s.get)
    }

  }
}
