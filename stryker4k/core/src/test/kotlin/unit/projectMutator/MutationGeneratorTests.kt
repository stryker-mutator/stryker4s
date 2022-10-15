package unit.projectMutator

import process.projectMutator.MutationGenerator
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import models.Mutable
import models.Mutation
import models.SourceFile
import process.projectMutator.mutators.*
import kotlin.test.Test

class MutationGeneratorTests {

    @Test
    fun shouldGenerateMutations() {
        // Arrange
        val mockkMutable = mockk<Mutable>()
        every { mockkMutable.mutations } returns mutableListOf(Mutation(mockkMutable, mockk(), ""))

        mockkObject(BooleanLiteralMutator)
        mockkObject(StringLiteralMutator)
        mockkObject(EqualityOperatorMutator)
        mockkObject(ConditionalExpressionMutator)
        mockkObject(LogicalOperatorMutator)

        every { BooleanLiteralMutator.mutateFile(any()) } returns listOf(mockkMutable)
        every { StringLiteralMutator.mutateFile(any()) } returns listOf(mockkMutable)
        every { EqualityOperatorMutator.mutateFile(any()) } returns listOf(mockkMutable)
        every { ConditionalExpressionMutator.mutateFile(any()) } returns listOf(mockkMutable)
        every { LogicalOperatorMutator.mutateFile(any()) } returns listOf(mockkMutable)

        val target = MutationGenerator
        val sourceFiles = listOf(SourceFile("", mockk()))

        // Act
        val result = target.generateMutations(sourceFiles)

        // Assert
        assert(sourceFiles[0].mutables.isNotEmpty())
        assert(result.isNotEmpty())
    }
}
