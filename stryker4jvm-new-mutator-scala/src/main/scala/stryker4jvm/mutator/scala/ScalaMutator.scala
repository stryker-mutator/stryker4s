package stryker4jvm.mutator.scala

import stryker4jvm.core.model.LanguageMutator

class ScalaMutator
    extends LanguageMutator[ScalaAST](new ScalaParser(), new ScalaCollector(), new ScalaInstrumenter()) {}
