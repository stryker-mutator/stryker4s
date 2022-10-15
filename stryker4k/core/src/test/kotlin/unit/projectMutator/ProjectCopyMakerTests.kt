package unit.projectMutator

import process.projectMutator.ProjectCopyMaker
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.verify
import utility.FileUtility
import kotlin.test.Test

class ProjectCopyMakerTests {

    @Test
    fun shouldCopyProject() {
        // Arrange
        val tempDirPath = "tempDir"

        mockkObject(FileUtility)
        every { FileUtility.createTempDir() } returns tempDirPath
        every { FileUtility.readDir(any()) } returns listOf("test.kt", "test.lock")
        every { FileUtility.copyFileTo(any(), any()) } returns ""

        val target = ProjectCopyMaker

        // Act
        val result = target.copySourceProject("")

        // Assert
        assert(result == tempDirPath)
        verify { FileUtility.copyFileTo("test.kt", any()) }
        verify(exactly = 0) { FileUtility.copyFileTo("test.lock", any()) }
    }
}
