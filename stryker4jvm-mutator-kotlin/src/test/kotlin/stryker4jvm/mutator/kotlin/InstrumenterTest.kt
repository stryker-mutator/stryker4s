package stryker4jvm.mutator.kotlin

import org.jetbrains.kotlin.com.intellij.psi.JavaPsiFacade
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.astReplace
import stryker4jvm.mutator.kotlin.utility.PsiUtility
import kotlin.test.Test

class InstrumenterTest {

    @Test
    fun testF() {
        val factory = JavaPsiFacade.getElementFactory(PsiUtility.project)
        val anotherFactory = KtPsiFactory(PsiUtility.project)
        val original = anotherFactory.createExpression("true")
        // both replace methods crash...
        //original.replace(anotherFactory.createExpression("false"))
        original.astReplace(anotherFactory.createExpression("false"))
    }

    @Test
    fun testInstrumenter() {
//        val instrumenter = KotlinInstrumenter()
//        val collector = MutatorTest.newCollector(BooleanLiteralMutator)
//        val ast = MutatorTest.parse("fun dummy() { print(true && false) }")
//
//        val collected = collector.collect(ast)
//        var id = 0
//        val mutantsWithId = collected.mutations.mapValues { (_, v) ->
//            v.map {
//                MutantWithId(id++, it)
//            }
//        }
//        val mutatedAst = instrumenter.instrument(ast, mutantsWithId.toMutableMap())
//        println(mutatedAst)
    }
}