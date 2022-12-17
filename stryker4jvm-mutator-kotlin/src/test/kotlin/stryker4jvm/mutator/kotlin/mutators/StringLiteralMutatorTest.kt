package stryker4jvm.mutator.kotlin.mutators

import io.mockk.clearAllMocks
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.*
import stryker4jvm.mutator.kotlin.mutators.StringLiteralMutator
import stryker4jvm.mutator.kotlin.utility.PsiUtility
import kotlin.reflect.typeOf
import org.junit.jupiter.api.Test

class StringLiteralMutatorTest {

    @Test
    fun testStringMutatorMutate() {
        // Arrange
        clearAllMocks()
        val target = StringLiteralMutator
        val testFile = PsiUtility.createPsiFile("fun dummy() { print(\"test\" + \"\") }")

        // Act
        val result = target.mutateFile(testFile)

        // Assert
//        assert(result[0].mutations[0].mutatorName == "StringLiteral")
//        assert(result[0].mutations[0].element.text == "\"\"")
//        assert(result[1].mutations[0].element.text == "\"Stryker was here!\"")
    }
}
