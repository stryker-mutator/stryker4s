package stryker4jvm.mutator.scala

import stryker4jvm.core.model.Parser

import java.nio.file.Path

class ScalaParser extends Parser[ScalaAST] {

  override def parse(file: Path): ScalaAST = ???
}
