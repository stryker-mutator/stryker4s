package stryker4jvm.mutator.kotlin

import org.jetbrains.kotlin.psi.KtElement
import scala.collection.Seq
import stryker4jvm.model.MutantWithId
import stryker4jvm.mutants.language.Instrumenter

class KotlinInstrumenter : Instrumenter<KotlinAST> {
    override fun apply(source: KotlinAST?, mutants: Seq<MutantWithId<KotlinAST>>?): KotlinAST {
        TODO("Not yet implemented")
    }
}