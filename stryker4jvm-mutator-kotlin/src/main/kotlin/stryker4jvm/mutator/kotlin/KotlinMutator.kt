package stryker4jvm.mutator.kotlin

import stryker4jvm.core.model.LanguageMutator

class KotlinMutator(parser : KotlinParser,
                    collector : KotlinCollector,
                    instrumenter : KotlinInstrumenter)
    : LanguageMutator<KotlinAST>(parser, collector, instrumenter) {

    constructor(collector: KotlinCollector) : this(KotlinParser(), collector, KotlinInstrumenter())

    constructor() : this(KotlinParser(), KotlinCollector(), KotlinInstrumenter())
}