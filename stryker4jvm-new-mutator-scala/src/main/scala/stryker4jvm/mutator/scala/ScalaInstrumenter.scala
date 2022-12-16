package stryker4jvm.mutator.scala

import stryker4jvm.core.model.Instrumenter
import java.{util => ju}
import stryker4jvm.core.model.MutantWithId

class ScalaInstrumenter extends Instrumenter[ScalaAST] {
  override def instrument(x$1: ScalaAST, x$2: ju.Map[ScalaAST,ju.List[MutantWithId[ScalaAST]]]): ScalaAST = ???
}
