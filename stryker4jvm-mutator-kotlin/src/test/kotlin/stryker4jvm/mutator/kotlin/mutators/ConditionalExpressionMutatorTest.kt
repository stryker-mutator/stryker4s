package stryker4jvm.mutator.kotlin.mutators

import io.mockk.clearAllMocks
import kotlin.test.Test
import kotlin.test.*

class ConditionalExpressionMutatorTest {

    @Test
    fun testConditionalExpressionMutatorMutate() {
        // Arrange
        clearAllMocks()
        val target = MutatorTestUtil.newCollector(ConditionalExpressionMutator)
        val testFile = MutatorTestUtil.parse("""
            fun dummy() { 
                if(0 < 1) print("a")
                if(0 <= 1) print("a")
                if(0 > 1) print("a")
                if(0 >= 1) print("a")
                if(0 == 1) print("a")
                if(0 != 1) print("a")
                if(0 === 1) print("a")
                if(0 !== 1) print("a")
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
        assertEquals(10, mutations.size)

        MutatorTestUtil.testName("ConditionalExpression", result)

        val expect = mutableListOf("true", "false") // they all mutate to true and false
        MutatorTestUtil.testMutations(
                mapOf(
                        Pair("0 < 1", expect),
                        Pair("0 <= 1", expect),
                        Pair("0 > 1", expect),
                        Pair("0 >= 1", expect),
                        Pair("0 == 1", expect),
                        Pair("0 != 1", expect),
                        Pair("0 === 1", expect),
                        Pair("0 !== 1", expect),
                        Pair("0 || 1", expect),
                        Pair("0 && 1", expect)
                ),
                result
        )
    }
}
