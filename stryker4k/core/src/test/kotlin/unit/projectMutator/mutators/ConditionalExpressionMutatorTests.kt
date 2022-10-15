package unit.projectMutator.mutators

import process.projectMutator.mutators.ConditionalExpressionMutator
import io.mockk.clearAllMocks
import models.SourceFile
import utility.PsiUtility
import kotlin.test.Test

class ConditionalExpressionMutatorTests {

    @Test
    fun conditionalExpressionMutatorMutateTest() {
        // Arrange
        clearAllMocks()
        val target = ConditionalExpressionMutator
        val testFile = PsiUtility.createPsiFile("""
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
        val result = target.mutateFile(SourceFile("", testFile))

        // Assert
        assert(result[0].mutations[0].mutatorName == "ConditionalExpression")
        assert(result[0].mutations[0].element.text == "true")
        assert(result[0].mutations[1].element.text == "false")
        assert(result[1].mutations[0].element.text == "true")
        assert(result[1].mutations[1].element.text == "false")
        assert(result[2].mutations[0].element.text == "true")
        assert(result[2].mutations[1].element.text == "false")
        assert(result[3].mutations[0].element.text == "true")
        assert(result[3].mutations[1].element.text == "false")
        assert(result[4].mutations[0].element.text == "true")
        assert(result[4].mutations[1].element.text == "false")
        assert(result[5].mutations[0].element.text == "true")
        assert(result[5].mutations[1].element.text == "false")
        assert(result[6].mutations[0].element.text == "true")
        assert(result[6].mutations[1].element.text == "false")
        assert(result[7].mutations[0].element.text == "true")
        assert(result[7].mutations[1].element.text == "false")
        assert(result[8].mutations[0].element.text == "true")
        assert(result[8].mutations[1].element.text == "false")
        assert(result[9].mutations[0].element.text == "true")
        assert(result[9].mutations[1].element.text == "false")
    }
}
