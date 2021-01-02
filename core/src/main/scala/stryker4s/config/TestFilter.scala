package stryker4s.config

import java.util.regex.Pattern

import scala.util.Try

import stryker4s.config.TestFilter.wildcardToRegex

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

case class Partition(negative: Seq[Regex], positive: Seq[Regex])

case class Regex(regex: String) {
  def matches(testName: String): Boolean = Try(Pattern.matches(regex, testName)).fold(_ => false, b => b)
}

object TestFilter {

  def wildcardToRegex(wildcard: String): String = s"^${wildcard.toList.map(convertChar).mkString}$$"

  def convertChar(c: Char): String =
    c match {
      case '*'                 => ".*"
      case '?'                 => "."
      case _ if isRegexChar(c) => s"\\${c.toString}"
      case c                   => c.toString
    }

  def isRegexChar(c: Char): Boolean =
    Seq('(', ')', '[', ']', '$', '^', '.', '{', '}', '|', '\\').foldLeft(false)((acc, elt) => acc || c == elt)
}
