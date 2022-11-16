package stryker4jvm.mutator.kotlin

import scala.collection.Seq
import stryker4jvm.core.model.MutantWithId
import stryker4jvm.mutants.language.Instrumenter

class KotlinInstrumenter : Instrumenter<KotlinAST> {
    override fun apply(source: KotlinAST?, mutants: Seq<MutantWithId<KotlinAST>>?): KotlinAST {
        TODO("Not yet implemented")
    }
}