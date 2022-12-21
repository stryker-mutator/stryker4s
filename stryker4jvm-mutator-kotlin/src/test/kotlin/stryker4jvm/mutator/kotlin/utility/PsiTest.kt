package stryker4jvm.mutator.kotlin.utility

import kotlin.test.Test
import kotlin.test.*
import stryker4jvm.mutator.kotlin.KotlinAST
import stryker4jvm.mutator.kotlin.mutators.BooleanLiteralMutator
import stryker4jvm.mutator.kotlin.mutators.MutatorTest

/*
https://github.com/JetBrains/intellij-community/blob/master/platform/core-api/src/com/intellij/psi/PsiElement.java
https://github.com/JetBrains/kotlin/blob/master/compiler/psi/src/org/jetbrains/kotlin/psi/psiUtil/ktPsiUtil.kt
https://github.com/JetBrains/kotlin/blob/master/compiler/psi/src/org/jetbrains/kotlin/psi/KtPsiFactory.kt
https://github.com/JetBrains/intellij-structural-search-for-kotlin/blob/master/src/main/kotlin/com/jetbrains/kotlin/structuralsearch/KotlinReplaceHandler.kt
 */
class PsiTest {
    @Test
    fun testPsiEquality() {
        /*
        Apparently, PsiElement.equals does not return true when you compare
        elements with the same structure.

        Even PsiManager#areElementsEquivalent returns false in this case

        If you compare with .equals (or ==) you compare (as it appears to be) by reference

        Only if you compare with textMatches are two separate objects considered equal,
        but only if they contain the exact same text, i.e. ' true ' != 'true'
         */
        val tree = PsiUtility.createPsiElement("true")
        val tree2 = PsiUtility.createPsiElement("true")
        assertTrue(tree.textMatches(tree2))
        assertTrue(tree == tree)
        //val manager = PsiManager.getInstance(tree.project)
        //Assertions.assertTrue(manager.areElementsEquivalent(tree, tree2)) // yields false...
        //Assertions.assertTrue(tree2.isEquivalentTo(tree)) // yields false...
        //Assertions.assertTrue(tree.isEquivalentTo(tree2)) // yields false...

        // tree3 produces runtime exception because true with spaces is not accepted
        //val tree3 = PsiUtility.createPsiElement(" true ")
        //Assertions.assertFalse(tree.textMatches(tree3))
    }

    @Test
    fun testPsiLocation() {
        val input = """
            object test {
                fun testFunction() {
                    return true || false
                }
            }""".trimMargin()
        val ast = MutatorTest.parse(input)
        // boolean literal will only find the true and false literals
        val collector = MutatorTest.newCollector(BooleanLiteralMutator)
        val result = collector.collect(ast)
        assertFalse(result.mutations.isEmpty())

        var k: KotlinAST? = null
        result.mutations.forEach { (key, _) ->
            k = key
        }

        // true and false are at line 2 (0 indexed)
        val location = PsiUtility.getLocation(k!!.tree)
        assertEquals(2, location.start.line)
        assertEquals(2, location.end.line)

        // columns cannot be determined so they should always be zero
        assertEquals(0, location.end.column)
        assertEquals(0, location.start.column)
    }
}