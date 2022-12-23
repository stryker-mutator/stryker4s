package stryker4jvm.mutator.scala

import stryker4jvm.core.config.LanguageMutatorConfig
import stryker4jvm.core.model.Instrumenter

import java.util as ju
import stryker4jvm.core.model.MutantWithId

class ScalaInstrumenter extends Instrumenter[ScalaAST] {
  override def instrument(ast: ScalaAST, mutations: ju.Map[ScalaAST, ju.List[MutantWithId[ScalaAST]]], config: LanguageMutatorConfig): ScalaAST = ???
}
