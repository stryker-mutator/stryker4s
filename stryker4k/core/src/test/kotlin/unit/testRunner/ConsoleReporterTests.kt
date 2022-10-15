package unit.testRunner

import io.mockk.every
import io.mockk.mockk
import models.*
import process.testRunner.ConsoleReporter
import kotlin.test.Test

class ConsoleReporterTests {

    @Test
    fun shouldReportProgress() {
        // Arrange
        val sourceFile = SourceFile("\\src\\main\\kotlin\\test.kt", mockk())
        val mutable = mockk<Mutable>()
        every { mutable.sourceFile } returns sourceFile
        sourceFile.mutables.add(mutable)
        val mutation = Mutation(mutable, mockk(), "", 1)
        val testResult = TestResult(mutation, Result.Killed)
        val target = ConsoleReporter()

        // Act
        target.reportProgress(testResult, 1)

        // Assert
        assert(target.logger.logged[0] == "Running mutants: 1/1 done")
    }

    @Test
    fun shouldLogReport() {
        // Arrange
        val sourceFile = SourceFile("\\src\\main\\kotlin\\test.kt", mockk())
        val sourceFile2 = SourceFile("\\src\\main\\kotlin\\test2.kt", mockk())
        val mutable = mockk<Mutable>()
        val mutable2 = mockk<Mutable>()
        every { mutable.sourceFile } returns sourceFile
        every { mutable2.sourceFile } returns sourceFile2
        sourceFile.mutables.add(mutable)
        val mutation = Mutation(mutable, mockk(), "")
        val mutation2 = Mutation(mutable2, mockk(), "")
        val results = listOf(
            TestResult(mutation, Result.Survived),
            TestResult(mutation2, Result.Killed),
        )
        val target = ConsoleReporter()
        target.testResults.addAll(results)

        // Act
        target.reportFinished()

        // Assert
        assert(target.logger.logged[0] == "Mutation testing finished. The results are:")
        assert(target.logger.logged[1] == "-----------------------------------------------")
        assert(target.logger.logged[2] == "|           | % score | # killed | # survived |")
        assert(target.logger.logged[3] == "|-----------|---------|----------|------------|")
        assert(target.logger.logged[4] == "| All files | 50.00%  | 1        | 1          |")
        assert(target.logger.logged[5] == "| test.kt   | 0.00%   | 0        | 1          |")
        assert(target.logger.logged[6] == "| test2.kt  | 100.00% | 1        | 0          |")
        assert(target.logger.logged[7] == "-----------------------------------------------")
    }

    @Test
    fun shouldLogEmptyReport() {
        // Arrange
        val target = ConsoleReporter()

        // Act
        target.reportFinished()

        // Assert
        assert(target.logger.logged[0] == "Mutation testing finished. The results are:")
        assert(target.logger.logged[1] == "-----------------------------------------------")
        assert(target.logger.logged[2] == "|           | % score | # killed | # survived |")
        assert(target.logger.logged[3] == "|-----------|---------|----------|------------|")
        assert(target.logger.logged[4] == "| All files | 0.00%   | 0        | 0          |")
        assert(target.logger.logged[5] == "-----------------------------------------------")
    }

    @Test
    fun shouldCalcCorrectPathLength() {
        // Arrange
        val target = ConsoleReporter()
        val filesShort = listOf("testy.kt")
        val filesmiddle = listOf("testty.kt")
        val fileslong = listOf("testtie.kt")

        // Act
        val result1 = target.calcMaxPathLength(filesShort)
        val result2 = target.calcMaxPathLength(filesmiddle)
        val result3 = target.calcMaxPathLength(fileslong)

        // Assert
        assert(result1 == 9)
        assert(result2 == 9)
        assert(result3 == 10)
    }
}
