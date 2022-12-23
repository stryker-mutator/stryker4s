package stryker4jvm.mutator.scala

import stryker4jvm.core.config.LanguageMutatorConfig
import stryker4jvm.core.model.Collector
import stryker4jvm.core.model.CollectedMutants

class ScalaCollector extends Collector[ScalaAST] {

  override def collect(ast: ScalaAST, config: LanguageMutatorConfig): CollectedMutants[ScalaAST] = ???

}
