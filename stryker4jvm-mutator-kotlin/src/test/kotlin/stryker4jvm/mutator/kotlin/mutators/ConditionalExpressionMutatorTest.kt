package stryker4jvm.mutator.kotlin.mutators

import io.mockk.clearAllMocks
import stryker4jvm.mutator.kotlin.utility.PsiUtility
import org.junit.jupiter.api.Test
import stryker4jvm.mutator.kotlin.KotlinAST
import stryker4jvm.mutator.kotlin.KotlinCollector
import org.junit.jupiter.api.Assertions.*

class ConditionalExpressionMutatorTest {

    @Test
    fun testConditionalExpressionMutatorMutate() {
        // Arrange
        clearAllMocks()
        val target = KotlinCollector(arrayOf(ConditionalExpressionMutator))
        val testFile = KotlinAST(PsiUtility.createPsiFile("""
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
        """.trimIndent()))

        // Act
        val result = target.collect(testFile)
        val ignored = result.ignoredMutations
        val mutations = result.mutations

        // Assert
        val trueElement = KotlinAST(PsiUtility.createPsiElement("true"))
        val falseElement = KotlinAST(PsiUtility.createPsiElement("false"))

        assertTrue(ignored.isEmpty())
        assertEquals(10, mutations.size)

        var statement = KotlinAST(PsiUtility.createPsiElement("0 < 1"))
        assertEquals("ConditionalExpression", mutations[statement]!![0].metaData.mutatorName)
        assertEquals("true", mutations[statement]!![0].metaData.replacement)
        assertEquals(trueElement, mutations[statement]!![0].mutatedStatement)
        assertEquals("false", mutations[statement]!![1].metaData.replacement)
        assertEquals(falseElement, mutations[statement]!![1].mutatedStatement)
//        assert(result[0].mutations[0].mutatorName == "ConditionalExpression")
//        assert(result[0].mutations[0].element.text == "true")
//        assert(result[0].mutations[1].element.text == "false")
//        assert(result[1].mutations[0].element.text == "true")
//        assert(result[1].mutations[1].element.text == "false")
//        assert(result[2].mutations[0].element.text == "true")
//        assert(result[2].mutations[1].element.text == "false")
//        assert(result[3].mutations[0].element.text == "true")
//        assert(result[3].mutations[1].element.text == "false")
//        assert(result[4].mutations[0].element.text == "true")
//        assert(result[4].mutations[1].element.text == "false")
//        assert(result[5].mutations[0].element.text == "true")
//        assert(result[5].mutations[1].element.text == "false")
//        assert(result[6].mutations[0].element.text == "true")
//        assert(result[6].mutations[1].element.text == "false")
//        assert(result[7].mutations[0].element.text == "true")
//        assert(result[7].mutations[1].element.text == "false")
//        assert(result[8].mutations[0].element.text == "true")
//        assert(result[8].mutations[1].element.text == "false")
//        assert(result[9].mutations[0].element.text == "true")
//        assert(result[9].mutations[1].element.text == "false")
    }
}
