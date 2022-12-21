package stryker4jvm.mutator.kotlin.mutators

import io.mockk.clearAllMocks
import kotlin.test.Test
import kotlin.test.*

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
        assertTrue(ignored.isEmpty())
        assertEquals(2, mutations.size)

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
