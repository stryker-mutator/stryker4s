package stryker4s.run

import cats.effect.{Deferred, IO, Ref, Resource}
import cats.syntax.all._
import stryker4s.config.Config
import stryker4s.model._
import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.stubs.TestRunnerStub
import stryker4s.testutil.{Stryker4sIOSuite, TestData}

import scala.concurrent.duration._

class TestRunnerTest extends Stryker4sIOSuite with LogMatchers with TestData {

  describe("timeoutRunner") {
    implicit val config = Config.default

    describe("initialTestRun") {
      it("should complete the timeout after the initial testrun") {
        val op = for {
          timeout <- Deferred[IO, FiniteDuration]
          sut = TestRunner.timeoutRunner(timeout, initialTestRunner())
          _ <- sut.use(_.initialTestRun())
        } yield timeout

        op.flatMap(_.tryGet).asserting(_ shouldBe defined)
      }

      it("should use the reported timeout if available") {
        val reportedTimeout = 2.seconds
        val op = for {
          timeout <- Deferred[IO, FiniteDuration]
          innerTR = initialTestRunner(InitialTestRunCoverageReport(true, Map.empty, Map.empty, reportedTimeout))
          sut = TestRunner.timeoutRunner(timeout, innerTR)
          _ <- sut.use(_.initialTestRun())
        } yield timeout

        op.flatMap(_.tryGet).asserting { setTimeout =>
          setTimeout.value shouldBe ((reportedTimeout * 1.5) + config.timeout)
          "Timeout set to 8 seconds (net 2 seconds)" shouldBe loggedAsInfo
        }
      }

      it("should not log the timeout setting if timeout has already been completed") {
        val op = for {
          timeout <- Deferred[IO, FiniteDuration]
          _ <- timeout.complete(10.seconds)
          sut = TestRunner.timeoutRunner(timeout, initialTestRunner())
          _ <- sut.use(_.initialTestRun())
        } yield timeout

        op.asserting { _ =>
          "Timeout set to " should not be loggedAsInfo
        }
      }
    }

    describe("runMutant") {
      it("should timeout slow mutant runs") {
        val mutant = createMutant
        val op = for {
          timeout <- Deferred[IO, FiniteDuration]
          // Set timeout to something short
          _ <- timeout.complete(1.millisecond)
          // TestRunner is slower than timeout
          sut = TestRunner.timeoutRunner(timeout, timeoutRunner(5.milliseconds, mutant))
          result <- sut.use(_.runMutant(mutant))
        } yield result

        op.asserting { result =>
          result shouldBe TimedOut(mutant)
          s"Mutant ${mutant.id} timed out over 1 millisecond" shouldBe loggedAsDebug
        }
      }

      it("should recreate the inner resource after a timeout") {
        val op = for {
          timeout <- Deferred[IO, FiniteDuration]
          // Set timeout to something short
          _ <- timeout.complete(1.millisecond)
          log <- Ref[IO].of(List.empty[String])
          innerTR = recreateLoggingTestRunner(log, timeoutRunner(5.milliseconds, createMutant))
          sut = TestRunner.timeoutRunner(timeout, innerTR)
          _ <- sut.use(_.runMutant(createMutant))
        } yield log

        op.flatMap(_.get).asserting { log =>
          log shouldBe List("open", "close", "open", "close")
        }
      }
    }
  }

  describe("retryRunner") {
    it("should retry after a first attempt fails") {
      val mutant = createMutant
      val secondResult = Killed(mutant)
      val op = for {
        log <- Ref[IO].of(List.empty[String])
        innerTR = recreateLoggingTestRunner(
          log,
          Resource.pure(new TestRunnerStub(Seq(() => fail("first attempt should be retried"), () => secondResult)))
        )
        sut = TestRunner.retryRunner(innerTR)
        result <- sut.use(_.runMutant(mutant))
        logResults <- log.get
      } yield (result, logResults)

      op.asserting { case (result, log) =>
        log shouldBe List("open", "close", "open", "close")
        result shouldBe secondResult
        s"Testrunner crashed for mutant ${mutant.id}. Starting a new one and retrying this mutant 2 more time(s)" shouldBe loggedAsInfo
      }
    }

    it("should report a mutant as Error after 3 failures") {
      val mutant = createMutant
      val op = for {
        log <- Ref[IO].of(List.empty[String])
        stubResults = (1 to 3).toList.map(_ => () => fail("attempt should be retried"))
        sut = TestRunner.retryRunner(
          recreateLoggingTestRunner(log, Resource.pure(new TestRunnerStub(stubResults)))
        )
        result <- sut.use(_.runMutant(mutant))
        logResults <- log.get
      } yield (result, logResults)

      op.asserting { case (result, log) =>
        log shouldBe List("open", "close", "open", "close", "open", "close", "open", "close")
        result shouldBe Error(mutant)
        s"Testrunner crashed for mutant ${mutant.id}. Starting a new one and retrying this mutant 2 more time(s)" shouldBe loggedAsInfo
        s"Testrunner crashed for mutant ${mutant.id}. Starting a new one and retrying this mutant 1 more time(s)" shouldBe loggedAsInfo
        s"Testrunner crashed for mutant ${mutant.id}. Starting a new one and retrying this mutant 0 more time(s)" shouldBe loggedAsInfo
      }
    }

    it("should only report as Error after 3 failed attempts") {
      val mutant = createMutant
      val thirdResult = Killed(mutant)
      val op = for {
        log <- Ref[IO].of(List.empty[String])
        stubResults = Seq(
          () => fail("attempt should be retried"),
          () => fail("attempt should be retried"),
          () => thirdResult
        )
        sut = TestRunner.retryRunner(
          recreateLoggingTestRunner(log, Resource.pure(new TestRunnerStub(stubResults)))
        )
        result <- sut.use(_.runMutant(mutant))
        logResults <- log.get
      } yield (result, logResults)

      op.asserting { case (result, log) =>
        log shouldBe List("open", "close", "open", "close", "open", "close")
        result shouldBe thirdResult
        s"Testrunner crashed for mutant ${mutant.id}. Starting a new one and retrying this mutant 2 more time(s)" shouldBe loggedAsInfo
        s"Testrunner crashed for mutant ${mutant.id}. Starting a new one and retrying this mutant 1 more time(s)" shouldBe loggedAsInfo
        s"Testrunner crashed for mutant ${mutant.id}. Starting a new one and retrying this mutant 0 more time(s)" should not be loggedAsInfo
      }
    }
  }

  describe("maxReuseTestRunner") {
    it("should not recreate if reuse is not reached yet") {
      val mutants = List(createMutant, createMutant)
      val expectedResults = mutants.map(Killed(_))
      val reuse = 3
      val op = for {
        log <- Ref[IO].of(List.empty[String])
        innerTR = recreateLoggingTestRunner(log, Resource.pure(new TestRunnerStub(expectedResults.map(() => _))))
        sut = TestRunner.maxReuseTestRunner(reuse, innerTR)
        result <- sut.use { tr =>
          mutants.traverse(tr.runMutant(_))
        }
        logResults <- log.get
      } yield (result, logResults)

      op.asserting { case (result, log) =>
        log shouldBe List("open", "close")
        result shouldBe expectedResults
        s"Testrunner has run for $reuse times. Restarting it..." should not be loggedAsInfo
      }
    }

    it("should recreate when reuse is reached") {
      val mutants = List(createMutant, createMutant, createMutant)
      val expectedResults = mutants.map(Killed(_))
      val reuse = 2
      val op = for {
        log <- Ref[IO].of(List.empty[String])
        innerTR = recreateLoggingTestRunner(log, Resource.pure(new TestRunnerStub(expectedResults.map(() => _))))
        sut = TestRunner.maxReuseTestRunner(reuse, innerTR)
        result <- sut.use { tr =>
          mutants.traverse(tr.runMutant(_))
        }
        logResults <- log.get
      } yield (result, logResults)

      op.asserting { case (result, log) =>
        log shouldBe List("open", "close", "open", "close")
        result shouldBe expectedResults
        s"Testrunner has run for $reuse times. Restarting it..." shouldBe loggedAsInfo
      }
    }
  }

  def initialTestRunner(result: InitialTestRunResult = NoCoverageInitialTestRun(true)): Resource[IO, TestRunner] =
    Resource.pure(new TestRunner {
      def initialTestRun(): IO[InitialTestRunResult] = IO.pure(result)
      def runMutant(mutant: Mutant): IO[MutantRunResult] = ???
    })

  def timeoutRunner(sleep: FiniteDuration, result: Mutant): Resource[IO, TestRunner] =
    Resource.pure(new TestRunner {
      def initialTestRun(): IO[InitialTestRunResult] = ???
      def runMutant(mutant: Mutant): IO[MutantRunResult] = IO.sleep(sleep).as(Killed(result))
    })

  def recreateLoggingTestRunner(
      log: Ref[IO, List[String]],
      testRunner: Resource[IO, TestRunner]
  ): Resource[IO, TestRunner] =
    Resource
      .make(log.update(_ :+ "open"))(_ => log.update(_ :+ "close"))
      .flatMap(_ => testRunner)

}
