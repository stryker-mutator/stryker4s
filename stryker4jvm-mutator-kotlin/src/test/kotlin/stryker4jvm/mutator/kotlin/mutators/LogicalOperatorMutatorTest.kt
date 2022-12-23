package stryker4jvm.mutator.kotlin.mutators

import io.mockk.clearAllMocks
import kotlin.test.Test
import kotlin.test.*

class LogicalOperatorMutatorTest {

    @Test
    fun testLogicalOperatorMutatorMutate() {
        // Arrange
        clearAllMocks()
        val target = MutatorTestUtil.newCollector(LogicalOperatorMutator)
        val testFile = MutatorTestUtil.parse("""
            fun dummy() { 
                if(0 || 1) print("a")
                if(0 && 1) print("a")
            }
        """.trimIndent())

        // Act
        val result = target.collect(testFile)
        val ignored = result.ignoredMutations
        val mutations = result.mutations

        // Assert
        assertTrue(ignored.isEmpty())
        assertEquals(2, mutations.size)

        MutatorTestUtil.testName("LogicalOperator", result)

        MutatorTestUtil.testMutations(
                mapOf(
                        Pair("0 || 1", mutableListOf("0 && 1")),
                        Pair("0 && 1", mutableListOf("0 || 1"))
                ),
                result
        )
    }
}
