package stryker4jvm.mutator.kotlin.mutators

import io.mockk.clearAllMocks
import stryker4jvm.mutator.kotlin.utility.PsiUtility
import org.junit.jupiter.api.Test

class EqualityOperatorMutatorTest {

    @Test
    fun testEqualityOperatorMutatorMutate() {
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
        val result = target.mutateFile(testFile)

        // Assert
//        assert(result[0].mutations[0].mutatorName == "EqualityOperator")
//        assert(result[0].mutations[0].element.text == "0 <= 1")
//        assert(result[0].mutations[1].element.text == "0 >= 1")
//        assert(result[1].mutations[0].element.text == "0 < 1")
//        assert(result[1].mutations[1].element.text == "0 > 1")
//        assert(result[2].mutations[0].element.text == "0 >= 1")
//        assert(result[2].mutations[1].element.text == "0 <= 1")
//        assert(result[3].mutations[0].element.text == "0 > 1")
//        assert(result[3].mutations[1].element.text == "0 < 1")
//        assert(result[4].mutations[0].element.text == "0 != 1")
//        assert(result[5].mutations[0].element.text == "0 == 1")
//        assert(result[6].mutations[0].element.text == "0 !== 1")
//        assert(result[7].mutations[0].element.text == "0 === 1")
    }
}
