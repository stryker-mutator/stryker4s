package stryker4jvm.mutator.scala

import scala.meta.{Parsed, Source}

import scala.meta.parsers.XtensionParseInputLike
import stryker4jvm.core.model.Parser
import java.nio

class ScalaParser extends Parser[ScalaAST] {

  override def parse(file: nio.file.Path): ScalaAST = {
    val parsed = file.parse[Source];

    parsed match {
      case _: Parsed.Error => throw new Exception("Cannot parse")
      case s =>
        val ret = new ScalaAST(source = s.get)
        return ret
    }
  }
}
