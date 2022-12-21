package stryker4jvm.mutator.kotlin

import org.jetbrains.kotlin.psi.KtConstantExpression
import stryker4jvm.core.config.LanguageMutatorConfig
import stryker4jvm.core.model.MutantWithId
import stryker4jvm.mutator.kotlin.mutators.BooleanLiteralMutator
import stryker4jvm.mutator.kotlin.mutators.MutatorTestUtil
import stryker4jvm.mutator.kotlin.utility.PsiUtility
import kotlin.test.Test
import kotlin.test.*

class InstrumenterTest {

        @Test
    fun shouldReplace() {
        val file = PsiUtility.createPsiFile("val x = true")
        val `true` = PsiUtility.findElementsInFile(file, KtConstantExpression::class.java)
        val pre = file.text
        assertEquals("val x = true", pre)
        `true`.first().replace(PsiUtility.createPsiElement("false"))
        val post = file.text
        assertEquals("val x = false", post)
    }

    @Test
    fun testReplace() {
        // a replacement of the top-level element creates a new object...
        val original = PsiUtility.createPsiElement("true")
        assertEquals("true", original.text)
        val mutated = original.replace(PsiUtility.createPsiElement("false"))
        assertEquals("false", mutated.text)
    }

    @Test
    fun testInstrumenter() {
        val instrumenter = KotlinInstrumenter()
        val collector = MutatorTestUtil.newCollector(BooleanLiteralMutator)
        val ast = MutatorTestUtil.parse("fun dummy() { print(true && false) }")

        val collected = collector.collect(ast)
        var id = 0
        val mutantsWithId = collected.mutations.mapValues { (_, v) ->
            v.map {
                MutantWithId(id++, it)
            }
        }
        val mutatedAst = instrumenter.instrument(ast, mutantsWithId.toMutableMap())
        val mutatedText = mutatedAst!!.tree.text
        assertTrue(mutatedText.contains("\"0\" -> false"))
        assertTrue(mutatedText.contains("else -> true"))
        assertFalse(mutatedText.contains("\"0\" -> true"))

        assertTrue(mutatedText.contains("\"1\" -> true"))
        assertTrue(mutatedText.contains("else -> false"))
        assertFalse(mutatedText.contains("\"1\" -> false"))

    }
}