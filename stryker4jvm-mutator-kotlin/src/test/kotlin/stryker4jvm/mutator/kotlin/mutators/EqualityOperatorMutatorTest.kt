package stryker4jvm.mutator.kotlin.mutators

import io.mockk.clearAllMocks
import kotlin.test.Test
import kotlin.test.*

class EqualityOperatorMutatorTest {

    @Test
    fun testEqualityOperatorMutatorMutate() {
        // Arrange
        clearAllMocks()
        val target = MutatorTest.newCollector(EqualityOperatorMutator)
        val testFile = MutatorTest.parse("""
            fun dummy() { 
                if(0 < 1) print("a")
                if(0 <= 1) print("a")
                if(0 > 1) print("a")
                if(0 >= 1) print("a")
                if(0 == 1) print("a")
                if(0 != 1) print("a")
                if(0 === 1) print("a")
                if(0 !== 1) print("a")
            }
        """.trimIndent())

        // Act
        val result = target.collect(testFile)
        val ignored = result.ignoredMutations
        val mutations = result.mutations

        // Assert
        assertTrue(ignored.isEmpty())
        assertEquals(8, mutations.size)

        MutatorTest.testName("EqualityOperator", result)
        MutatorTest.testMutations(
                mapOf(
                        Pair("0 < 1", mutableListOf("0 <= 1", "0 >= 1")),
                        Pair("0 <= 1", mutableListOf("0 < 1", "0 > 1")),
                        Pair("0 > 1", mutableListOf("0 >= 1", "0 <= 1")),
                        Pair("0 >= 1", mutableListOf("0 > 1", "0 < 1")),
                        Pair("0 == 1", mutableListOf("0 != 1")),
                        Pair("0 != 1", mutableListOf("0 == 1")),
                        Pair("0 === 1", mutableListOf("0 !== 1")),
                        Pair("0 !== 1", mutableListOf("0 === 1"))
                ),
                result
        )
    }
}
