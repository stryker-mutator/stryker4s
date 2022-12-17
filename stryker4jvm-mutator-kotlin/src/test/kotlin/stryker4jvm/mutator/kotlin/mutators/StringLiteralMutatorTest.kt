package stryker4jvm.mutator.kotlin.mutators

import io.mockk.clearAllMocks
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.*
import org.junit.jupiter.api.Assertions
import stryker4jvm.mutator.kotlin.mutators.StringLiteralMutator
import stryker4jvm.mutator.kotlin.utility.PsiUtility
import kotlin.reflect.typeOf
import org.junit.jupiter.api.Test

class StringLiteralMutatorTest {

    @Test
    fun testStringMutatorMutate() {
        // Arrange
        clearAllMocks()
        val target = MutatorTest.newCollector(StringLiteralMutator)
        val testFile = MutatorTest.parse("fun dummy() { print(\"test\" + \"\") }")

        // Act
        val result = target.collect(testFile)
        val ignored = result.ignoredMutations
        val mutations = result.mutations

        // Assert
        Assertions.assertTrue(ignored.isEmpty())
        Assertions.assertEquals(2, mutations.size)

        MutatorTest.testName("StringLiteral", result)

        MutatorTest.testMutations(
                mapOf(
                        Pair("\"test\"", mutableListOf("\"\"")),
                        Pair("\"\"", mutableListOf("\"Stryker was here!\""))
                ),
                result
        )
    }
}
