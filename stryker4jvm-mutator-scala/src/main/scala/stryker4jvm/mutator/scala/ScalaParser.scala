package stryker4jvm.mutator.scala

import scala.meta.{dialects, Dialect, Parsed, Source}
import scala.meta.parsers.XtensionParseInputLike
import stryker4jvm.core.model.Parser

import java.nio

/** Class used to parse (scala) files into a ScalaAST
  */
class ScalaParser(scalaDialect: Dialect) extends Parser[ScalaAST] {

  /** Method used to parse a file (via a path) to a ScalaAST
    *
    * @param file
    *   The path to a scala file, which we want to parse
    * @return
    *   A ScalaAST if the file was successfully parsed, otherwise we throw an Exception
    */
  override def parse(file: nio.file.Path): ScalaAST = {
    implicit val dialect: Dialect = scalaDialect

    val parsed = file.parse[Source]

    parsed match {
      case _: Parsed.Error => throw new Exception("Cannot parse")
      case s =>
        val ret = new ScalaAST(value = s.get)
        return ret
    }
  }
}
