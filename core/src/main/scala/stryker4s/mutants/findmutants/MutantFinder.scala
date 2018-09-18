package stryker4s.mutants.findmutants

import better.files.File
import grizzled.slf4j.Logging
import stryker4s.model.{Mutant, MutationsInSource}

import scala.meta.Source
import scala.meta.parsers.{Parsed, XtensionParseInputLike}

class MutantFinder(matcher: MutantMatcher) extends Logging {

  def mutantsInFile(filePath: File): MutationsInSource = {
    val parsedSource = parseFile(filePath)
    MutationsInSource(parsedSource, findMutants(parsedSource))
  }

  def findMutants(source: Source): Seq[Mutant] = {
    source.collect(matcher.allMatchers()).flatten
  }

  def parseFile(file: File): Source =
    file.contentAsString
      .replace("\r\n", "\n")
      .parse[Source] match {
      case Parsed.Success(source) =>
        source
      case Parsed.Error(_, msg, ex) =>
        error(s"Error while parsing file $file, $msg")
        throw ex
    }
}
