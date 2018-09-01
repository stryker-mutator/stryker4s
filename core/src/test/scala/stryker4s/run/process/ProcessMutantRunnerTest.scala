package stryker4s.run.process

import java.nio.file.Paths

import stryker4s.Stryker4sSuite
import stryker4s.config.Config
import stryker4s.model._
import stryker4s.run.ProcessMutantRunner
import stryker4s.scalatest.FileUtil
import stryker4s.stubs.TestProcessRunner

import scala.concurrent.TimeoutException
import scala.meta._
import scala.util.{Failure, Success}

class ProcessMutantRunnerTest extends Stryker4sSuite {
  implicit val config: Config = Config(
    baseDir = FileUtil.getResource("scalaFiles"))

  describe("apply") {
    it("should return a Survived mutant on an exitcode 0 process") {
      val testProcessRunner = new TestProcessRunner(Success(0))
      val sut =
        new ProcessMutantRunner(Command("foo", "test"), testProcessRunner)
      val mutant = Mutant(0, q"4", q"5")
      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
      val mutatedFile =
        MutatedFile(file,
                    q"def foo = 4",
                    Seq(RegisteredMutant(q"4", Seq(mutant))))

      val result = sut.apply(Seq(mutatedFile))

      testProcessRunner.timesCalled.next() should equal(1)
      result.mutationScore shouldBe 00.00
      val loneResult = result.results.loneElement
      loneResult should equal(Survived(mutant, Paths.get("simpleFile.scala")))
    }

    it("should return a Killed mutant on an exitcode 1 process") {
      val testProcessRunner = new TestProcessRunner(Success(1))
      val sut =
        new ProcessMutantRunner(Command("foo", "test"), testProcessRunner)
      val mutant = Mutant(0, q"4", q"5")
      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
      val mutatedFile =
        MutatedFile(file,
                    q"def foo = 4",
                    Seq(RegisteredMutant(q"4", Seq(mutant))))

      val result = sut.apply(Seq(mutatedFile))

      testProcessRunner.timesCalled.next() should equal(1)
      result.mutationScore shouldBe 100.00
      val loneResult = result.results.loneElement
      loneResult should equal(Killed(1, mutant, Paths.get("simpleFile.scala")))
    }

    it("should return a TimedOut mutant on a TimedOut process") {
      val exception = new TimeoutException("Test")
      val testProcessRunner = new TestProcessRunner(Failure(exception))
      val sut =
        new ProcessMutantRunner(Command("foo", "test"), testProcessRunner)
      val mutant = Mutant(0, q"4", q"5")
      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
      val mutatedFile =
        MutatedFile(file,
                    q"def foo = 4",
                    Seq(RegisteredMutant(q"4", Seq(mutant))))

      val result = sut.apply(Seq(mutatedFile))

      testProcessRunner.timesCalled.next() should equal(1)
      result.mutationScore shouldBe 100.00
      val loneResult = result.results.loneElement
      loneResult should equal(
        TimedOut(exception, mutant, Paths.get("simpleFile.scala")))
    }

    it("should return a combination of results on multiple runs") {
      val testProcessRunner = new TestProcessRunner(Success(1), Success(1))
      val sut =
        new ProcessMutantRunner(Command("foo", "test"), testProcessRunner)
      val mutant = Mutant(0, q"0", q"zero")
      val secondMutant = Mutant(1, q"1", q"one")
      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
      val mutants = Seq(mutant, secondMutant)
      val mutatedFile =
        MutatedFile(file, q"def foo = 4", Seq(RegisteredMutant(q"4", mutants)))

      val result = sut.apply(Seq(mutatedFile))

      testProcessRunner.timesCalled.next() should equal(2)

      result.mutationScore shouldBe 100.00
      result.results should contain only (
        Killed(1, mutant, Paths.get("simpleFile.scala")),
        Killed(1, secondMutant, Paths.get("simpleFile.scala"))
      )
    }

    it("should return a mutationScore of 66.67 when 2 of 3 mutants are killed") {
      val testProcessRunner =
        new TestProcessRunner(Success(1), Success(1), Success(0))
      val sut =
        new ProcessMutantRunner(Command("foo", "test"), testProcessRunner)
      val mutant = Mutant(0, q"0", q"zero")
      val secondMutant = Mutant(1, q"1", q"one")
      val thirdMutant = Mutant(2, q"5", q"5")
      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
      val mutants = Seq(mutant, secondMutant, thirdMutant)
      val mutatedFile =
        MutatedFile(file, q"def foo = 4", Seq(RegisteredMutant(q"4", mutants)))

      val result = sut.apply(Seq(mutatedFile))

      testProcessRunner.timesCalled.next() should equal(3)

      result.mutationScore shouldBe 66.67
      result.results should contain only (
        Killed(1, mutant, Paths.get("simpleFile.scala")),
        Killed(1, secondMutant, Paths.get("simpleFile.scala")),
        Survived(thirdMutant, Paths.get("simpleFile.scala"))
      )
    }
  }
}
