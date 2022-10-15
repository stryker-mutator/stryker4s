package unit.projectMutator.mutators

import process.projectMutator.mutators.EqualityOperatorMutator
import io.mockk.clearAllMocks
import models.SourceFile
import utility.PsiUtility
import kotlin.test.Test

class EqualityOperatorMutatorTests {

    @Test
    fun equalityOperatorMutatorMutateTest() {
        // Arrange
        clearAllMocks()
        val target = EqualityOperatorMutator
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
            }
        """.trimIndent())

        // Act
        val result = target.mutateFile(SourceFile("", testFile))

        // Assert
        assert(result[0].mutations[0].mutatorName == "EqualityOperator")
        assert(result[0].mutations[0].element.text == "0 <= 1")
        assert(result[0].mutations[1].element.text == "0 >= 1")
        assert(result[1].mutations[0].element.text == "0 < 1")
        assert(result[1].mutations[1].element.text == "0 > 1")
        assert(result[2].mutations[0].element.text == "0 >= 1")
        assert(result[2].mutations[1].element.text == "0 <= 1")
        assert(result[3].mutations[0].element.text == "0 > 1")
        assert(result[3].mutations[1].element.text == "0 < 1")
        assert(result[4].mutations[0].element.text == "0 != 1")
        assert(result[5].mutations[0].element.text == "0 == 1")
        assert(result[6].mutations[0].element.text == "0 !== 1")
        assert(result[7].mutations[0].element.text == "0 === 1")
    }
}
