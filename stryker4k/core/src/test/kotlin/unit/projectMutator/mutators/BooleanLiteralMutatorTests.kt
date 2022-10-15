package unit.projectMutator.mutators

import process.projectMutator.mutators.BooleanLiteralMutator
import io.mockk.clearAllMocks
import models.SourceFile
import utility.PsiUtility
import kotlin.test.Test

class BooleanLiteralMutatorTests {

    @Test
    fun booleanMutatorMutateTest() {
        // Arrange
        clearAllMocks()
        val target = BooleanLiteralMutator
        val testFile = PsiUtility.createPsiFile("fun dummy() { print(true && false) }")

        // Act
        val result = target.mutateFile(SourceFile("", testFile))

        // Assert
        assert(result[0].mutations[0].mutatorName == "BooleanLiteral")
        assert(result[0].mutations[0].element.text == "false")
        assert(result[1].mutations[0].element.text == "true")
    }
}
