package stryker4jvm.mutator.kotlin

import org.jetbrains.kotlin.psi.KtElement
import scala.Tuple2
import scala.collection.immutable.Map
import scala.collection.immutable.Vector
import stryker4jvm.model.IgnoredMutationReason
import stryker4jvm.model.MutatedCode
import stryker4jvm.mutants.language.Collector

/*
well, this method is horrifying to implement.
Forced to use Tuples in tuples here, very scala specific classes
What exactly is this method supposed to return?

The first part of the tuple is a vector of mutations that were ignored?
If a mutation is ignored, why does it still have 'MutatedCode'?

The second part of the tuple is a map of AST to a vector of possible mutations?
 */
class MutationCollector : Collector<KtElement> {
    override fun apply(tree: KtElement?): Tuple2<Vector<Tuple2<MutatedCode<KtElement>, IgnoredMutationReason>>, Map<KtElement, Vector<MutatedCode<KtElement>>>> {
        val t = Tuple2(1, 2)
        TODO("Not yet implemented")
    }
}