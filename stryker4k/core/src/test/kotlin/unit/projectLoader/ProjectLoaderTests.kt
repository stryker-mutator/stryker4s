package unit.projectLoader

import com.google.gson.JsonSyntaxException
import io.mockk.*
import models.Configuration
import process.projectLoader.ProjectLoader
import process.testRunner.ConsoleReporter
import process.testRunner.HTMLReporter
import utility.FileUtility
import utility.PsiUtility
import java.io.File
import java.io.IOException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNull

class ProjectLoaderTests {

    @BeforeTest
    fun setup() {
        unmockkAll()
        clearAllMocks()
        Configuration.sourcePath = ""
        Configuration.command = "gradlew test --rerun-tasks"
        Configuration.reporters = listOf(ConsoleReporter())
    }

    @AfterTest
    fun cleanup() {
        ProjectLoader.logger.logged.removeAll { true }
        unmockkAll()
        clearAllMocks()
    }

    @Test
    fun emptyProjectShouldLoadNoFilesAndUseDefaultConfig() {
        // Arrange
        val testPath = "test\\path"
        mockkObject(FileUtility)
        every { FileUtility.readFile(any<String>()) } answers { throw IOException() }
        every { FileUtility.readDir(any()) } returns listOf()
        mockkObject(PsiUtility)
        every { PsiUtility.createPsiFile(any()) } returns mockk()
        val target = ProjectLoader

        // Act
        val result = target.loadProject(testPath)

        // Assert
        assert(result.isEmpty())
        assert(Configuration.sourcePath == "test/path")
        assert(Configuration.command == "gradlew test --rerun-tasks")
        assert(Configuration.reporters.size == 1)
        assert(Configuration.reporters[0] is ConsoleReporter)
        assert(target.logger.logged[0] == "Using default configuration")

    }

    @Test
    fun shouldFailToParseConfigAndUseDefaults() {
        // Arrange
        val testPath = "test\\path"
        mockkObject(FileUtility)
        every { FileUtility.readFile(any<String>()) } answers { throw JsonSyntaxException("") }
        every { FileUtility.readDir(any()) } returns listOf()
        mockkObject(PsiUtility)
        every { PsiUtility.createPsiFile(any()) } returns mockk()
        val target = ProjectLoader

        // Act
        val result = target.loadProject(testPath)

        // Assert
        assert(result.isEmpty())
        assert(Configuration.sourcePath == "test/path")
        assert(Configuration.command == "gradlew test --rerun-tasks")
        assert(Configuration.reporters.size == 1)
        assert(Configuration.reporters[0] is ConsoleReporter)
        assert(target.logger.logged[0] == "Failed to load configuration file. Using default configuration")

    }

    @Test
    fun shouldLoadProjectFilesAndUseCustomConfig() {
        // Arrange
        val testPath = "testPath"
        val fileName = "test.kt"
        val fileName2 = "test.html"
        val testCommand = "custom test"
        mockkObject(FileUtility)
        every { FileUtility.readFile(any<String>()) } returns
            """{ "command": "$testCommand", "reporters": ["console", "html"] }"""
        every { FileUtility.readFile(any<File>()) } returns ""
        every { FileUtility.readDir(any()) } returns listOf("$testPath/$fileName", "$testPath/$fileName2")

        mockkObject(PsiUtility)
        every { PsiUtility.createPsiFile(any()) } returns mockk()

        // Act
        val result = ProjectLoader.loadProject(testPath)

        // Assert
        verify { FileUtility.readFile("testPath/stryker-conf.json") }
        verify { FileUtility.readDir("testPath") }
        assert(result.isNotEmpty())
        assert(result[0].path == "/$fileName")
        assertNull(result.find { it.path == "/$fileName2"})
        assert(Configuration.command == testCommand)
        assert(Configuration.reporters.size == 2)
        assert(Configuration.reporters[0] is ConsoleReporter)
        assert(Configuration.reporters[1] is HTMLReporter)
    }

    @Test
    fun shouldLoadProjectFilesAndUseCustomConfigReportersOnly() {
        // Arrange
        val testPath = "testPath"
        val fileName = "test.kt"
        mockkObject(FileUtility)
        every { FileUtility.readFile(any<String>()) } returns """{ "reporters": ["console", "html"] }"""
        every { FileUtility.readFile(any<File>()) } returns ""
        every { FileUtility.readDir(any()) } returns listOf("$testPath/$fileName")

        mockkObject(PsiUtility)
        every { PsiUtility.createPsiFile(any()) } returns mockk()

        // Act
        ProjectLoader.loadProject(testPath)

        val con = Configuration;
        con.command
        // Assert
        assert(Configuration.command == "gradlew test --rerun-tasks")
        assert(Configuration.reporters.size == 2)
        assert(Configuration.reporters[0] is ConsoleReporter)
        assert(Configuration.reporters[1] is HTMLReporter)

    }

    @Test
    fun shouldLoadProjectFilesAndUseCustomConfigCommandOnly() {
        // Arrange
        val testPath = "testPath"
        val fileName = "test.kt"
        val testCommand = "custom test"
        mockkObject(FileUtility)
        every { FileUtility.readFile(any<String>()) } returns """{ "command": "$testCommand" }"""
        every { FileUtility.readFile(any<File>()) } returns ""
        every { FileUtility.readDir(any()) } returns listOf("$testPath/$fileName")

        mockkObject(PsiUtility)
        every { PsiUtility.createPsiFile(any()) } returns mockk()

        // Act
        ProjectLoader.loadProject(testPath)

        // Assert
        assert(Configuration.command == testCommand)
        assert(Configuration.reporters.size == 1)
        assert(Configuration.reporters[0] is ConsoleReporter)
    }
}
