package unit.projectMutator

import process.projectMutator.MutationPlacer
import io.mockk.*
import models.Mutable
import models.Mutation
import models.SourceFile
import utility.FileUtility
import utility.PsiUtility
import java.io.IOException
import kotlin.test.Test

class MutationPlacerTests {

    @Test
    fun shouldPlaceMutations() {
        // Arrange
        val tempPath = "temp/path"
        val filePath = "temp/path/test.kt"

        mockkObject(FileUtility)
        every { FileUtility.writeFile(any(), any()) } returns Unit

        mockkObject(PsiUtility)
        every { PsiUtility.replacePsiElement(any(), any()) } returns mockk()

        val mockkSourceFile = spyk(SourceFile("/test.kt", mockk()))
        every { mockkSourceFile.getText() } returns ""
        val mockMutable = mockk<Mutable>()
        every { mockMutable.sourceFile } returns mockkSourceFile
        every { mockMutable.getText() } returns "true"
        every { mockMutable.originalElement } returns mockk()
        val mockMutation = spyk(Mutation(mockMutable, mockk(), ""))
        every { mockMutation.getText() } returns "false"
        every { mockMutable.mutations } returns mutableListOf(mockMutation)
        mockkSourceFile.mutables.add(mockMutable)

        // Act
        MutationPlacer.placeMutations(listOf(mockkSourceFile), tempPath)

        // Assert
        verify { FileUtility.writeFile(filePath, any()) }
        verify { PsiUtility.createPsiElement(
            "when(System.getenv(\"ACTIVE_MUTATION\") ?: null) {\n\"1\" -> false\nelse -> true\n}"
        ) }
    }


    @Test
    fun shouldFailToWriteMutatedFile() {
        // Arrange
        val tempPath = "temp/path"

        mockkObject(FileUtility)
        every { FileUtility.writeFile(any(), any()) } answers { throw IOException() }

        mockkObject(PsiUtility)
        every { PsiUtility.replacePsiElement(any(), any()) } returns mockk()
        every { PsiUtility.createPsiElement(any()) } returns mockk()

        val mockkSourceFile = spyk(SourceFile("/test.kt", mockk()))
        every { mockkSourceFile.getText() } returns ""
        val mockMutable = mockk<Mutable>()
        every { mockMutable.sourceFile } returns mockkSourceFile
        every { mockMutable.getText() } returns "true"
        every { mockMutable.originalElement } returns mockk()
        val mockMutation = spyk(Mutation(mockMutable, mockk(), ""))
        every { mockMutation.getText() } returns "false"
        every { mockMutable.mutations } returns mutableListOf(mockMutation)
        mockkSourceFile.mutables.add(mockMutable)
        val target = MutationPlacer

        // Act
        target.placeMutations(listOf(mockkSourceFile), tempPath)

        // Assert
        assert(target.logger.logged[0] == "Failed to mutate /test.kt")
    }
}
