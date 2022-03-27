package stryker4s.mutants.tree

import cats.data.{NonEmptyList, NonEmptyMap, NonEmptyVector}
import cats.syntax.all.*
import stryker4s.extension.TreeExtensions.TransformOnceExtension
import stryker4s.model.{MutantWithId, MutatedFile, PlaceableTree}
import stryker4s.mutants.SourceContext

import scala.meta.*
import scala.util.{Failure, Success}

/** Instrument (place) mutants in a tree
  *
  * @param mutationContext
  *   on what the mutation should be activated. E.g. on `sys.env.get("ACTIVE_MUTATION")`.
  * @param condition
  *   Optional condition to add to the default case. Useful to add a side-effectful coverage measurement.
  */
final class MutantInstrumenter(options: InstrumenterOptions) {

  def apply(context: SourceContext, mutantMap: NonEmptyMap[PlaceableTree, MutationsWithId]): MutatedFile = {

    val newTree = context.source
      .transformOnce {
        Function.unlift { t =>
          val p = PlaceableTree(t)
          mutantMap(p).map { case (mutations) =>
            val mutableCases = mutations.map(mutantToCase)
            val default = defaultCase(p, mutations.map(_.id.globalId).toNonEmptyList)

            val cases = mutableCases :+ default

            buildMatch(cases)
          }
        }
      } match {
      case Success(tree) => tree.syntax
      case Failure(e) =>
        throw new RuntimeException(
          s"Failed to instrument mutants in `${context.path}`. Please create a new issue including the stacktrace on GitHub https://github.com/stryker-mutator/stryker4s/issues/new",
          e
        )
    }

    val mutations: MutationsWithId = mutantMap.toSortedMap.toVector.toNev.get.flatMap(_._2)

    MutatedFile(context.path, newTree, mutations)
  }

  def mutantToCase(mutant: MutantWithId): Case = {
    val newTree = mutant.mutatedCode.mutatedStatement.asInstanceOf[Term]

    buildCase(newTree, options.pattern(mutant.id.globalId))
  }

  def defaultCase(placeableTree: PlaceableTree, mutantIds: NonEmptyList[Int]): Case =
    p"case _ if ${options.condition.mapApply(mutantIds)} => ${placeableTree.tree.asInstanceOf[Term]}"

  def buildCase(expression: Term, pattern: Pat): Case = p"case $pattern => $expression"

  def buildMatch(cases: NonEmptyVector[Case]) = q"(${options.mutationContext} match { ..case ${cases.toList} })"
}
