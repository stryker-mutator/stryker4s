package stryker4s.mutants.findmutants

import scala.meta.Parsed.{Error, Success}
import scala.meta.Source
import scala.meta.parsers.XtensionParseInputLike

import better.files.File
import stryker4s.config.Config
import stryker4s.extension.FileExtensions._
import stryker4s.log.Logger
import stryker4s.model.{Mutant, MutationsInSource}

class MutantFinder(matcher: MutantMatcher)(implicit config: Config, log: Logger) {
  def mutantsInFile(filePath: File): MutationsInSource = {
    val parsedSource = parseFile(filePath)
    val (included, excluded) = findMutants(parsedSource)
    MutationsInSource(parsedSource, included, excluded)
  }

  def findMutants(source: Source): (Seq[Mutant], Int) = {
    val (included, excluded) = source.collect(matcher.allMatchers).flatten.partition(_.isDefined)
    (included.flatten, excluded.size)
  }

  def parseFile(file: File): Source =
    // Match is reported as non-exhaustive for some reason
    (file.toJava.parse[Source]: @unchecked) match {
      case Success(source) =>
        source
      case Error(_, msg, ex) =>
        log.error(s"Error while parsing file '${file.relativePath}', $msg")
        throw ex
    }
}
