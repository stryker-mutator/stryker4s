package stryker4jvm.mutator.kotlin.mutators

import io.mockk.clearAllMocks
import kotlin.test.Test
import kotlin.test.*

class StringLiteralMutatorTest {

    @Test
    fun testStringMutatorMutate() {
        // Arrange
        clearAllMocks()
        val target = MutatorTestUtil.newCollector(StringLiteralMutator)
        val testFile = MutatorTestUtil.parse("fun dummy() { print(\"test\" + \"\") }")

        // Act
        val result = target.collect(testFile)
        val ignored = result.ignoredMutations
        val mutations = result.mutations

        // Assert
        assertTrue(ignored.isEmpty())
        assertEquals(2, mutations.size)

        MutatorTestUtil.testName("StringLiteral", result)

        MutatorTestUtil.testMutations(
                mapOf(
                        Pair("\"test\"", mutableListOf("\"\"")),
                        Pair("\"\"", mutableListOf("\"Stryker was here!\""))
                ),
                result
        )
    }
}
