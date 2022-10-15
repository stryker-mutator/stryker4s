package unit.utility

import io.mockk.*
import utility.ProcessUtility
import kotlin.test.Test

class ProcessUtilityTests {

    @Test
    fun should_run_command() {
        // Arrange
        val sut = ProcessUtility
        mockkObject(sut)
        val processMock = spyk<Process>()
        every { processMock.exitValue() } returns 1
        every { ProcessUtility.startProcess(any()) } returns processMock

        // Act
        val result = sut.runCommand("gradlew test", "test", "ACTIVE_MUTATION", "2")

        // Assert
        verify { sut.startProcess(any()) }
        assert(result == 1)
    }
}
