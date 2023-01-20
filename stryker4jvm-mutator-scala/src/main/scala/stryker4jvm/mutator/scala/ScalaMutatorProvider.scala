package stryker4jvm.mutator.scala

import stryker4jvm.core.config.LanguageMutatorConfig
import stryker4jvm.core.model.{InstrumenterOptions, LanguageMutator}
import stryker4jvm.core.model.languagemutator.LanguageMutatorProvider
import stryker4jvm.core.logging.Logger

/** Class used to actually create and provide the scala mutator to stryker4jvm
  */
class ScalaMutatorProvider extends LanguageMutatorProvider {
  override def provideMutator(
      languageMutatorConfig: LanguageMutatorConfig,
      log: Logger,
      instrumenterOptions: InstrumenterOptions
  ): LanguageMutator[ScalaAST] = {
    new ScalaMutator(
      new ScalaParser(),
      new ScalaCollector(
        traverser = new TraverserImpl()(log),
        matcher = new MutantMatcherImpl(config = languageMutatorConfig)
      )(log),
      new ScalaInstrumenter(
        options = ScalaInstrumenterOptions.fromJavaOptions(instrumenterOptions = instrumenterOptions)
      )
    )
  }
}
