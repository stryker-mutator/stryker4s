package stryker4jvm.mutator.kotlin

import stryker4jvm.core.config.LanguageMutatorConfig
import stryker4jvm.core.model.InstrumenterOptions
import stryker4jvm.core.model.LanguageMutator
import stryker4jvm.core.model.languagemutator.LanguageMutatorProvider

class KotlinMutatorProvider : LanguageMutatorProvider {

    override fun provideMutator(config: LanguageMutatorConfig, options: InstrumenterOptions) : LanguageMutator<KotlinAST> {

        return KotlinMutator(
                KotlinParser(),
                KotlinCollector.apply(config),
                KotlinInstrumenter(options)
        );
    }
}