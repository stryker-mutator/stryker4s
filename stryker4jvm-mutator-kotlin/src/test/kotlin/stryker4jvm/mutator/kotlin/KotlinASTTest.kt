package stryker4jvm.mutator.kotlin

import kotlin.test.Test
import kotlin.test.*
import stryker4jvm.mutator.kotlin.mutators.MutatorTest

class KotlinASTTest {
    @Test
    fun testKotlinAST(): Unit {
        val ast = MutatorTest.parse("false")
        val another = MutatorTest.parse("false")
        assertFalse(ast == another) // should not equal each other

        val map = mutableMapOf(Pair(ast, ast), Pair(another, another))
        assertTrue(map[ast] == ast)
        assertTrue(map[another] == another)
        assertFalse(map[ast] == map[another])

        // replace false with true
        //PsiUtility.replacePsiElement(ast.tree, MutatorTest.parse("true").tree)
//        assertEquals("true", ast.tree.text) // tree is modified but AST is immutable
//        assertEquals("false", ast.syntax()) // syntax never changes...
//        assertTrue(map[ast] == ast) // does not modify map consistency (i.e. hashcode is fixed)
    }
}