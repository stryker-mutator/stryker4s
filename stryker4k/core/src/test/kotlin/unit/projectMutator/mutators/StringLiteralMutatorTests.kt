package unit.projectMutator.mutators

import process.projectMutator.mutators.StringLiteralMutator
import io.mockk.clearAllMocks
import models.SourceFile
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.*
import utility.PsiUtility
import kotlin.reflect.typeOf
import kotlin.test.Test

class StringLiteralMutatorTests {

    @Test
    fun stringMutatorMutateTest() {
        // Arrange
        clearAllMocks()
        val target = StringLiteralMutator
        val testFile = PsiUtility.createPsiFile("fun dummy() { print(\"test\" + \"\") }")

        // Act
        val result = target.mutateFile(SourceFile("", testFile))

        // Assert
        assert(result[0].mutations[0].mutatorName == "StringLiteral")
        assert(result[0].mutations[0].element.text == "\"\"")
        assert(result[1].mutations[0].element.text == "\"Stryker was here!\"")
    }
}
