package stryker4s.mutants.findmutants

import scala.meta.Source
import scala.meta.parsers.{Parsed, XtensionParseInputLike}

import better.files.File
import grizzled.slf4j.Logging
import stryker4s.config.Config
import stryker4s.extension.FileExtensions._
import stryker4s.model.{Mutant, MutationsInSource}

class MutantFinder(matcher: MutantMatcher)(implicit config: Config) extends Logging {
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
    file.toJava.parse[Source] match {
      case Parsed.Success(source) =>
        source
      case Parsed.Error(_, msg, ex) =>
        error(s"Error while parsing file '${file.relativePath}', $msg")
        throw ex
    }
}
