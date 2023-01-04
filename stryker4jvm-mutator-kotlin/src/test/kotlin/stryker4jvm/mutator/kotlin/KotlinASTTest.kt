package stryker4jvm.mutator.kotlin

import stryker4jvm.mutator.kotlin.mutators.MutatorTestUtil
import stryker4jvm.mutator.kotlin.utility.PsiUtility
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class KotlinASTTest {
    @Test
    fun testKotlinAST() {
        val ast = MutatorTestUtil.parse("false")
        val another = MutatorTestUtil.parse("false")
        assertNotEquals(ast, another) // should not equal each other

        val map = mutableMapOf(Pair(ast, ast), Pair(another, another))
        assertEquals(map[ast], ast)
        assertEquals(map[another], another)
        assertNotEquals(map[ast], map[another])
    }
}