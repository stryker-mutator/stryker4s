package unit

import Stryker4k
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.verify
import process.projectLoader.ProjectLoader
import process.projectMutator.MutationGenerator
import process.projectMutator.MutationPlacer
import process.projectMutator.ProjectCopyMaker
import process.testRunner.CommandTestRunner
import utility.FileUtility
import java.io.IOException
import kotlin.test.Test

class Stryker4kTests {

    @Test
    fun shouldCallAllStrykerComponents() {
        // Arrange
        clearAllMocks()
        val sut = Stryker4k
        mockkObject(ProjectLoader)
        every { ProjectLoader.loadProject(any()) } returns mutableListOf()
        mockkObject(MutationGenerator)
        every { MutationGenerator.generateMutations(any()) }  returns mutableListOf()
        mockkObject(ProjectCopyMaker)
        every { ProjectCopyMaker.copySourceProject(any()) } returns "tempPath"
        mockkObject(MutationPlacer)
        every { MutationPlacer.placeMutations(any(), any()) } returns Unit
        mockkObject(CommandTestRunner)
        every { CommandTestRunner.run(any(), any()) } returns Unit
        mockkObject(FileUtility)
        every { FileUtility.deleteTempDir(any()) } returns Unit
        every { FileUtility.deleteTempDir(any()) } answers { throw IOException() }

        // Act
        sut.run(arrayOf("testProject"))

        // Assert
        verify { ProjectLoader.loadProject("testProject") }
        verify { MutationGenerator.generateMutations(any()) }
        verify { ProjectCopyMaker.copySourceProject("testProject") }
        verify { MutationPlacer.placeMutations(any(), "tempPath") }
        verify { CommandTestRunner.run(any(), "tempPath") }

        assert(sut.logger.logged[0] == "Loading project")
        assert(sut.logger.logged[1] == "Generating mutations" )
        assert(sut.logger.logged[2] == "Making project copy")
        assert(sut.logger.logged[3] == "Placing mutations in copy")
        assert(sut.logger.logged[4] == "Preparing to run mutants")
        assert(sut.logger.logged[5] == "Failed to delete project copy.")
        clearAllMocks()
    }
}
