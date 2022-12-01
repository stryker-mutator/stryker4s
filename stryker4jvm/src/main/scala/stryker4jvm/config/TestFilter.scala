package stryker4jvm.config

import stryker4jvm.mutator.scala.config.TestFilter.wildcardToRegex

import java.util.regex.Pattern
import scala.util.Try

class TestFilter()(implicit config: Config) {

  val exclamationMark = "!"

  lazy val partition: Partition = config.testFilter.partition(_.startsWith(exclamationMark)) match {
    case (negative, positive) =>
      Partition(
        negative.map(p => Regex(wildcardToRegex(p.substring(1)))),
        positive.map(p => Regex(wildcardToRegex(p)))
      )
  }

  def filter(testName: String): Boolean = {
    def matches(regexSeq: Seq[Regex]): Boolean =
      regexSeq.foldLeft(false)((acc, regex) => acc || regex.matches(testName))

    if (matches(partition.negative))
      false
    else
      partition.positive.isEmpty || matches(partition.positive)
  }
}

final case class Partition(negative: Seq[Regex], positive: Seq[Regex])

final case class Regex(regex: String) extends AnyVal {
  def matches(testName: String): Boolean = Try(Pattern.matches(regex, testName)).getOrElse(false)
}

object TestFilter {

  def wildcardToRegex(wildcard: String): String = s"^${wildcard.toList.map(convertChar).mkString}$$"

  def convertChar(c: Char): String =
    c match {
      case '*'                 => ".*"
      case '?'                 => "."
      case _ if isRegexChar(c) => "\\" + c.toString
      case c                   => c.toString
    }

  def isRegexChar(c: Char): Boolean =
    Seq('(', ')', '[', ']', '$', '^', '.', '{', '}', '|', '\\').foldLeft(false)((acc, elt) => acc || c == elt)
}
