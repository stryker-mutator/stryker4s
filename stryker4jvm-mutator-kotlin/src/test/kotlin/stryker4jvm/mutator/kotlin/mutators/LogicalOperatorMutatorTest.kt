package stryker4jvm.mutator.kotlin.mutators

import io.mockk.clearAllMocks
import stryker4jvm.mutator.kotlin.utility.PsiUtility
import org.junit.jupiter.api.Test

class LogicalOperatorMutatorTest {

    @Test
    fun testLogicalOperatorMutatorMutate() {
        // Arrange
        clearAllMocks()
        val target = LogicalOperatorMutator
        val testFile = PsiUtility.createPsiFile("""
            fun dummy() { 
                if(0 || 1) print("a")
                if(0 && 1) print("a")
            }
        """.trimIndent())

        // Act
        val result = target.mutateFile(testFile)

        // Assert
//        assert(result[0].mutations[0].mutatorName == "LogicalOperator")
//        assert(result[0].mutations[0].element.text == "0 && 1")
//        assert(result[1].mutations[0].element.text == "0 || 1")
    }
}
