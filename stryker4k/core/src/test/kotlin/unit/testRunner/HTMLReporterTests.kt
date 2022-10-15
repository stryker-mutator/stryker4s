package unit.testRunner

import io.mockk.*
import models.*
import mutationtesting.Location
import mutationtesting.Position
import process.testRunner.HTMLReporter
import utility.FileUtility
import java.io.File
import java.io.IOException
import kotlin.test.Test

class HTMLReporterTests {

    @Test
    fun shouldCreateHTMLReport() {
        // Arrange
        val sourceFile = spyk(SourceFile("\\src\\main\\kotlin\\test.kt", mockk()))
        every { sourceFile.getText() } returns ""
        sourceFile.originalText = ""
        val mutable = mockk<Mutable>()
        every { mutable.sourceFile } returns sourceFile
        every { mutable.location } returns Location(Position(0, 0), Position(0, 0))
        sourceFile.mutables.add(mutable)
        val mutation = spyk(Mutation(mutable, mockk(), ""))
        every { mutation.getText() } returns ""
        val results = listOf(TestResult(mutation, Result.Survived))
        val target = HTMLReporter()
        target.testResults.addAll(results)

        mockkObject(Configuration)
        every { Configuration.sourcePath } returns "test"

        val testJsonReport = File("src/test/kotlin/unit/testRunner/testJsonReport.json").readText(Charsets.UTF_8)

        mockkObject(FileUtility)
        every { FileUtility.writeFile(any(), any()) } returns Unit
        every { FileUtility.createDir(any()) } returns ""

        // Act
        target.reportFinished()

        // Assert
        verify { FileUtility.writeFile("test/build/reports/stryker4k-reports/index.html","""<!DOCTYPE html>
       <html lang="en">
       <head>
         <meta charset="UTF-8">
         <meta name="viewport" content="width=device-width, initial-scale=1.0">
         <script defer src="https://www.unpkg.com/mutation-testing-elements"></script>
       </head>
       <body>
         <mutation-test-report-app title-postfix="Stryker4k report">
           Your browser doesn't support <a href="https://caniuse.com/#search=custom%20elements">custom elements</a>.
           Please use a latest version of an evergreen browser (Firefox, Chrome, Safari, Opera, etc).
         </mutation-test-report-app>
         <script>
           const app = document.getElementsByTagName('mutation-test-report-app').item(0);
           function updateTheme() {
             document.body.style.backgroundColor = app.themeBackgroundColor;
           }
           app.addEventListener('theme-changed', updateTheme);
           updateTheme();
         </script>
         <script src="report.js"></script>
       </body>
       </html>""") }
        verify { FileUtility.writeFile("test/build/reports/stryker4k-reports/report.js", testJsonReport) }
        assert(target.logger.logged[0] == "Wrote HTML report to: test/build/reports/stryker4k-reports/index.html")
    }

    @Test
    fun shouldFailToWriteHTMLReport() {
        // Arrange
        val sourceFile = spyk(SourceFile("\\src\\main\\kotlin\\test.kt", mockk()))
        every { sourceFile.getText() } returns ""
        sourceFile.originalText = ""
        val mutable = mockk<Mutable>()
        every { mutable.sourceFile } returns sourceFile
        every { mutable.location } returns Location(Position(0, 0), Position(0, 0))
        sourceFile.mutables.add(mutable)
        val mutation = spyk(Mutation(mutable, mockk(), ""))
        every { mutation.getText() } returns ""
        val results = listOf(TestResult(mutation, Result.Survived))
        val target = HTMLReporter()
        target.testResults.addAll(results)

        mockkObject(Configuration)
        every { Configuration.sourcePath } returns "test"

        mockkObject(FileUtility)
        every { FileUtility.createDir(any()) } returns ""
        every { FileUtility.writeFile(any(), any()) } answers { throw IOException() }

        // Act
        target.reportFinished()

        // Assert
        assert(target.logger.logged[0] == "Failed to write HTML report.")
    }
}
