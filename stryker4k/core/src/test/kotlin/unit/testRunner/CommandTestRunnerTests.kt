package unit.testRunner

import io.mockk.*
import models.*
import process.testRunner.CommandTestRunner
import process.testRunner.ConsoleReporter
import utility.ProcessUtility
import java.io.IOException
import kotlin.test.Test

class CommandTestRunnerTests {

    @Test
    fun shouldRunConfiguredCommand() {
        // Arrange
        val target = CommandTestRunner

        mockkObject(ProcessUtility)
        every { ProcessUtility.runCommand(any(), any(), any(), "1") } answers { throw IOException() }
        every { ProcessUtility.runCommand(any(), any(), any(), "2") } returns 1

        val command = "custom test"
        mockkObject(Configuration)
        every { Configuration.command } returns command
        val mockkReporter = mockk<ConsoleReporter>(relaxed = true)
        every { Configuration.reporters } returns listOf(mockkReporter)

        val sourcePath = "test/path"

        val mutations = listOf(
            Mutation(mockk(), mockk(), "", 1),
            Mutation(mockk(), mockk(), "", 2)
        )
        // Act
        target.run(mutations, sourcePath)

        // Assert
        verify { ProcessUtility.runCommand(command, sourcePath, any(), any()) }
        verify { mockkReporter.reportProgress(match { it.result == Result.Killed }, 2) }
        verify { mockkReporter.reportFinished() }
        assert(target.logger.logged[0] == "Failed to run tests for mutation number: 1. Ignoring this mutation...")
    }
}
