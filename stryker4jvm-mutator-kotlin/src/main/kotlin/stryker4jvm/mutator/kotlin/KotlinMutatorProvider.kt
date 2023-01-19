package stryker4jvm.mutator.kotlin

import stryker4jvm.core.config.LanguageMutatorConfig
import stryker4jvm.core.exception.LanguageMutatorProviderException
import stryker4jvm.core.logging.Logger
import stryker4jvm.core.model.InstrumenterOptions
import stryker4jvm.core.model.LanguageMutator
import stryker4jvm.core.model.languagemutator.LanguageMutatorProvider

class KotlinMutatorProvider : LanguageMutatorProvider {
    @Throws(LanguageMutatorProviderException::class)
    override fun provideMutator(config: LanguageMutatorConfig, logger: Logger, options: InstrumenterOptions): LanguageMutator<*> {

        return KotlinMutator(
                KotlinParser(),
                KotlinCollector.apply(config),
                KotlinInstrumenter(options)
        )
    }
}