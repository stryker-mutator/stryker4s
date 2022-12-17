package stryker4jvm.mutator.kotlin.mutators

import io.mockk.clearAllMocks
import stryker4jvm.mutator.kotlin.utility.PsiUtility
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import stryker4jvm.mutator.kotlin.KotlinAST
import stryker4jvm.mutator.kotlin.KotlinCollector

class BooleanLiteralMutatorTest {
    @Test
    fun testBooleanMutator() {
        // Arrange
        clearAllMocks()
        val target = KotlinCollector(arrayOf(BooleanLiteralMutator))
        val testFile = KotlinAST(PsiUtility.createPsiFile("fun dummy() { print(true && false) }"))

        // Act
        val result = target.collect(testFile)
        val ignored = result.ignoredMutations
        val mutations = result.mutations

        // Assert
        assertTrue(ignored.isEmpty())
        assertEquals(2, mutations.size) // we have two mutations

        val trueElement = KotlinAST(PsiUtility.createPsiElement("true"))
        val falseElement = KotlinAST(PsiUtility.createPsiElement("false"))

        assertEquals(falseElement, mutations[trueElement]!![0].mutatedStatement)
        assertEquals("false", mutations[trueElement]!![0].metaData.replacement)
        assertEquals("BooleanLiteral", mutations[trueElement]!![0].metaData.mutatorName)

        assertEquals(trueElement, mutations[falseElement]!![0].mutatedStatement)
        assertEquals("true", mutations[falseElement]!![0].metaData.replacement)
        assertEquals("BooleanLiteral", mutations[falseElement]!![0].metaData.mutatorName)
    }
}
