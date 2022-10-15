package unit.projectMutator.mutators

import process.projectMutator.mutators.LogicalOperatorMutator
import io.mockk.clearAllMocks
import models.SourceFile
import utility.PsiUtility
import kotlin.test.Test

class LogicalOperatorMutatorTests {

    @Test
    fun logicalOperatorMutatorMutateTest() {
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
        val result = target.mutateFile(SourceFile("", testFile))

        // Assert
        assert(result[0].mutations[0].mutatorName == "LogicalOperator")
        assert(result[0].mutations[0].element.text == "0 && 1")
        assert(result[1].mutations[0].element.text == "0 || 1")
    }
}
