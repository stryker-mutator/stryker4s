package stryker4s.mutants.tree

import cats.data.{NonEmptyList, NonEmptyMap}
import cats.syntax.all.*
import stryker4s.model.{MutantWithId, MutatedFile, PlaceableTree}
import stryker4s.mutants.SourceContext
import stryker4s.mutants.applymutants.ActiveMutationContext.ActiveMutationContext

import scala.meta.*

/** Instrument (place) mutants in a tree
  *
  * @param mutationContext
  *   on what the mutation should be activated. E.g. on `sys.env.get("ACTIVE_MUTATION")`.
  * @param condition
  *   Optional condition to add to the default case. Useful to add a side-effectful coverage measurement.
  */
final class MutantInstrumenter(mutationContext: ActiveMutationContext, condition: Option[DefaultMutationCondition]) {

  def apply(context: SourceContext, mutantMap: NonEmptyMap[PlaceableTree, MutationsWithId]): MutatedFile = {

    val newTree = context.source.transform {
      Function.unlift { t =>
        val p = PlaceableTree(t)
        mutantMap(p).map { case (mutations) =>
          val mutableCases = mutations.map(mutantToCase)
          val default = defaultCase(p, mutations.map(_.id.globalId).toNonEmptyList)

          val cases = mutableCases :+ default

          q"($mutationContext match { ..case ${cases.toList} })"
        }
      }
    }.syntax

    val mutations: MutationsWithId = mutantMap.toSortedMap.toVector.toNev.get.flatMap(_._2)

    MutatedFile(context.path, newTree, mutations)
  }

  private def mutantToCase(mutant: MutantWithId): Case = {
    val newTree = mutant.mutatedCode.mutatedStatement.asInstanceOf[Term]

    buildCase(newTree, p"Some(${mutant.id.globalId})")
  }

  private def defaultCase(placeableTree: PlaceableTree, mutantIds: NonEmptyList[Int]): Case =
    p"case _ if ${condition.mapApply(mutantIds)} => ${placeableTree.tree.asInstanceOf[Term]}"

  private def buildCase(expression: Term, pattern: Pat): Case = p"case $pattern => $expression"
}
