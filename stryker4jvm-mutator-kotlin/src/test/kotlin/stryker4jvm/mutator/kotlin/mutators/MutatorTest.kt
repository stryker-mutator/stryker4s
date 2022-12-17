package stryker4jvm.mutator.kotlin.mutators

import stryker4jvm.core.model.CollectedMutants
import stryker4jvm.mutator.kotlin.KotlinAST
import org.junit.jupiter.api.Assertions.*
import stryker4jvm.mutator.kotlin.KotlinCollector
import stryker4jvm.mutator.kotlin.utility.PsiUtility
import java.util.*

object MutatorTest {
    /**
     * Simplified way to create a new collector
     */
    fun newCollector(vararg mutators: Mutator<*>): KotlinCollector {
        return KotlinCollector(mutators)
    }

    fun parse(text: String): KotlinAST {
        return KotlinAST(PsiUtility.createPsiFile(text))
    }

    /**
     * Tests the mutator name of the result. Makes sure that all found mutations
     * have the provided name.
     */
    fun testName(name: String, result: CollectedMutants<KotlinAST>) {
        result.mutations.forEach { entry ->
            entry.value.forEach { mutatedCode ->
                assertEquals(name, mutatedCode.metaData.mutatorName)
            }
        }
    }

    /**
     * Tests whether the expected mutations to be found are actually found.
     * The provided map maps original code to a list of possible mutations.
     * This function passes without errors iff all actual mutations are equal to found mutations (order does not matter)
     */
    fun testMutations(map: Map<String, MutableList<String>>, result: CollectedMutants<KotlinAST>) {
        result.mutations.forEach { entry ->
            assertTrue(map.containsKey(entry.key.syntax()))

            val expectedMutations = map[entry.key.syntax()]!!
            expectedMutations.sort()
            val actualMutations = entry.value.map { mc -> mc.metaData.replacement }.sorted()
            assertEquals(expectedMutations.size, actualMutations.size)
            actualMutations.forEachIndexed { index, s ->
                assertEquals(expectedMutations[index], s)
            }
        }
    }
}