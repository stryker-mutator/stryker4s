package stryker4jvm.mutator.kotlin

import stryker4jvm.core.model.Instrumenter
import stryker4jvm.model.MutantWithId

class KotlinInstrumenter : Instrumenter<KotlinAST> {
    override fun instrument(source: KotlinAST?, mutations: MutableList<MutantWithId<KotlinAST>>?): KotlinAST {
        TODO("Not yet implemented")
    }

}