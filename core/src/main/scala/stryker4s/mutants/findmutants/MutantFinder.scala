package stryker4s.mutants.findmutants

import scala.meta.parsers.XtensionParseInputLike
import scala.meta.{Dialect, Source}

import better.files.File
import cats.implicits._
import stryker4s.config.Config
import stryker4s.extension.FileExtensions._
import stryker4s.log.Logger
import stryker4s.model.{Mutant, MutationExcluded, MutationsInSource, RegexParseError}

class MutantFinder(matcher: MutantMatcher)(implicit config: Config, log: Logger) {
  def mutantsInFile(filePath: File): MutationsInSource = {
    val parsedSource = parseFile(filePath)
    val (included, excluded) = findMutants(parsedSource)
    MutationsInSource(parsedSource, included, excluded)
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

  def parseFile(file: File): Source = {
    implicit val dialect: Dialect = config.scalaDialect

    file.toJava
      .parse[Source]
      .fold(
        e => {
          log.error(s"Error while parsing file '${file.relativePath}', ${e.message}")
          throw e.details
        },
        identity
      )

  }
}
