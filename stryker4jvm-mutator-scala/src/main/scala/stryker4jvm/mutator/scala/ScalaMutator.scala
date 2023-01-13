package stryker4jvm.mutator.scala

import stryker4jvm.core.model.LanguageMutator

class ScalaMutator(var parser: ScalaParser, var collector: ScalaCollector, var instrumenter: ScalaInstrumenter)
    extends LanguageMutator[ScalaAST](parser, collector, instrumenter)
