package stryker4s.testutil

import cats.data.NonEmptyVector
import cats.syntax.option.*
import fs2.io.file.Path
import mutationtesting.*
import stryker4s.config.Config
import stryker4s.model.{MutantId, MutantMetadata, MutantWithId, MutatedCode}
import stryker4s.mutants.tree.MutantsWithId
import stryker4s.mutation.{GreaterThan, Mutation}
import stryker4s.report.FinishedRunEvent

import scala.concurrent.duration.*
import scala.meta.{Term, Tree}

trait TestData {
  def createMutant =
    MutantWithId(
      MutantId(0),
      MutatedCode(Term.Name("<"), MutantMetadata(">", "<", GreaterThan.mutationName, createLocation, none))
    )

  def toMutations[T <: Tree](
      original: Term,
      category: Mutation[T],
      firstReplacement: Term,
      replacements: Term*
  ): MutantsWithId =
    NonEmptyVector
      .of(firstReplacement, replacements*)
      .zipWithIndex
      .map { case (replacement, id) =>
        MutantWithId(
          MutantId(id),
          MutatedCode(
            replacement,
            MutantMetadata(original.toString(), replacement.toString, category.mutationName, original.pos, none)
          )
        )
      }

  def createLocation = Location(Position(0, 0), Position(0, 0))

  def createMutationTestResult = MutationTestResult(thresholds = Thresholds(100, 0), files = Map.empty)

  def createFinishedRunEvent(
      testResult: MutationTestResult[Config] = createMutationTestResult,
      metrics: Option[MetricsResult] = none
  ) =
    FinishedRunEvent(
      testResult,
      metrics.getOrElse(Metrics.calculateMetrics(testResult)),
      10.seconds,
      Path("target/stryker4s-report/")
    )
}
