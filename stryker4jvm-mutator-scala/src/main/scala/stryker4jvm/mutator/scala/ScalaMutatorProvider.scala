package stryker4jvm.mutator.scala

import stryker4jvm.core.config.LanguageMutatorConfig
import stryker4jvm.core.logging.Logger
import stryker4jvm.core.model.{InstrumenterOptions, LanguageMutator}
import stryker4jvm.core.model.languagemutator.LanguageMutatorProvider
import stryker4jvm.core.logging.Logger

class ScalaMutatorProvider extends LanguageMutatorProvider {
  override def provideMutator(
      languageMutatorConfig: LanguageMutatorConfig,
      log: Logger,
      instrumenterOptions: InstrumenterOptions
  ): LanguageMutator[ScalaAST] = {

    implicit val log = new ScalaLogger();

    new ScalaMutator(
      new ScalaParser(),
      new ScalaCollector(
        traverser = new TraverserImpl(),
        matcher = new MutantMatcherImpl(config = languageMutatorConfig)
      ),
      new ScalaInstrumenter(options = ScalaInstrumenterOptions.sysContext(ActiveMutationContext.envVar))
    )
  }
}
