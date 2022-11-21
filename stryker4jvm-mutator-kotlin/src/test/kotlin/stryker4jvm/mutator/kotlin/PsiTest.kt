package stryker4jvm.mutator.kotlin

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import stryker4jvm.mutator.kotlin.utility.PsiUtility

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
        Assertions.assertTrue(tree.textMatches(tree2))
        Assertions.assertTrue(tree == tree)

        // tree3 produces runtime exception because true with spaces is not accepted
        //val tree3 = PsiUtility.createPsiElement(" true ")
        //Assertions.assertFalse(tree.textMatches(tree3))
    }
}