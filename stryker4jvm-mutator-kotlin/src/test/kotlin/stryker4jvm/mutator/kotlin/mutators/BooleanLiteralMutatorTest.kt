package stryker4jvm.mutator.kotlin.mutators

import io.mockk.clearAllMocks
import stryker4jvm.mutator.kotlin.utility.PsiUtility
import kotlin.test.Test
import kotlin.test.*
import stryker4jvm.mutator.kotlin.KotlinAST
import stryker4jvm.mutator.kotlin.mutators.MutatorTestUtil.newCollector

class BooleanLiteralMutatorTest {
    @Test
    fun testBooleanMutator() {
        // Arrange
        clearAllMocks()
        val target = newCollector(BooleanLiteralMutator)
        val testFile = KotlinAST(PsiUtility.createPsiFile("fun dummy() { print(true && false) }"))

        // Act
        val result = target.collect(testFile)
        val ignored = result.ignoredMutations
        val mutations = result.mutations

        // Assert
        assertTrue(ignored.isEmpty())
        assertEquals(2, mutations.size) // we have two mutations

        MutatorTestUtil.testName("BooleanLiteral", result)
        MutatorTestUtil.testMutations(
                mapOf(
                        Pair("true", mutableListOf("false")),  // all trues map to false
                        Pair("false", mutableListOf("true"))), // all false map to trues
                result
        )
    }
}
