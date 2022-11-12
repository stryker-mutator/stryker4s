package stryker4jvm.mutator.kotlin

import org.jetbrains.kotlin.psi.KtElement
import scala.collection.Seq
import stryker4jvm.model.MutantWithId
import stryker4jvm.mutants.language.Instrumenter

class KotlinInstrumenter: Instrumenter<KtElement> {
    override fun apply(source: KtElement?, mutants: Seq<MutantWithId<KtElement>>?): KtElement {
        TODO("Not yet implemented")
    }

}