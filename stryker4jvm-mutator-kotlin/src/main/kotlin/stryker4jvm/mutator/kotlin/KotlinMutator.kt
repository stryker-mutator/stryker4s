package stryker4jvm.mutator.kotlin

import stryker4jvm.core.model.LanguageMutator

class KotlinMutator : LanguageMutator<KotlinAST>(KotlinParser(), KotlinCollector(), KotlinInstrumenter()) {

}