package stryker4s.mutants.tree

import cats.data.{NonEmptyList, NonEmptyVector}
import cats.syntax.all.*
import stryker4s.extensions.TreeExtensions.TransformOnceExtension
import stryker4s.exception.{Stryker4sException, UnableToBuildPatternMatchException}
import stryker4jvm.logging.Logger
import stryker4s.model.{MutantId, MutantWithId, MutatedFile, PlaceableTree, SourceContext}

import scala.meta.*
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

/** Instrument (place) mutants in a tree
  *
  * @param options
  *   Options for instrumenting a mutation switch, such as on what the mutation should be activated (like
  *   `sys.env.get("ACTIVE_MUTATION")`).
  */
class MutantInstrumenter(options: InstrumenterOptions)(implicit log: Logger) {

  def instrumentFile(context: SourceContext, mutantMap: Map[PlaceableTree, MutantsWithId]): MutatedFile = {

    val newTree = context.source
      .transformOnce {
        Function.unlift { originalTree =>
          val p = PlaceableTree(originalTree)
          mutantMap.get(p).map { case (mutations) =>
            val mutableCases = mutations.map(mutantToCase)
            val default = defaultCase(p, mutations.map(_.id).toNonEmptyList)

            val cases = mutableCases :+ default

            try
              buildMatch(cases)
            catch {
              case NonFatal(e) =>
                log.error(
                  s"Failed to instrument mutants in `${context.path}`. Original statement: [${originalTree.syntax}]"
                )
                log.error(
                  s"Failed mutation(s) '${mutations.map(_.id.value).mkString_(", ")}' at ${originalTree.pos.input}:${originalTree.pos.startLine + 1}:${originalTree.pos.startColumn + 1}."
                )
                log.error(
                  "This is likely an issue on Stryker4s's end, please enable debug logging and restart Stryker4s."
                )
                throw UnableToBuildPatternMatchException(context.path, e)
            }
          }
        }
      } match {
      case Success(tree)                  => tree
      case Failure(e: Stryker4sException) => throw e
      case Failure(e) =>
        throw new UnableToBuildPatternMatchException(context.path, e)
    }

    val mutations: MutantsWithId = mutantMap.map(_._2).toVector.toNev.get.flatten

    MutatedFile(context.path, newTree, mutations)
  }

  def mutantToCase(mutant: MutantWithId): Case = {
    val newTree = mutant.mutatedCode.mutatedStatement.asInstanceOf[Term]

    buildCase(newTree, options.pattern(mutant.id.value))
  }

  def defaultCase(placeableTree: PlaceableTree, mutantIds: NonEmptyList[MutantId]): Case =
    p"case _ if ${options.condition.mapApply(mutantIds.map(_.value))} => ${placeableTree.tree.asInstanceOf[Term]}"

  def buildCase(expression: Term, pattern: Pat): Case = p"case $pattern => $expression"

  def buildMatch(cases: NonEmptyVector[Case]): Term.Match =
    q"(${options.mutationContext} match { ..case ${cases.toList} })"
}
