package stryker4s.mutants.tree

import cats.data.Ior.Both
import cats.data.{Ior, NonEmptyList, NonEmptyVector}
import cats.syntax.all.*
import stryker4s.extension.TreeExtensions.{IsEqualExtension, TransformOnceExtension}
import stryker4s.extension.exception.{Stryker4sException, UnableToBuildPatternMatchException}
import stryker4s.log.Logger
import stryker4s.model.*

import scala.meta.*
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

/** Instrument (place) mutants in a tree
  *
  * @param options
  *   Options for instrumenting a mutation switch, such as on what the mutation should be activated (like
  *   `sys.env.get("ACTIVE_MUTATION")`).
  */
class MutantInstrumenter(options: InstrumenterOptions)(implicit log: Logger) {

  def instrumentFile(context: SourceContext, mutantMap: Map[PlaceableTree, MutantsWithId]): MutatedFile = {

    def instrumentWithMutants(mutantMap: Map[PlaceableTree, MutantsWithId]): PartialFunction[Tree, Tree] = {

      Function.unlift { originalTree =>
        val p = PlaceableTree(originalTree)
        mutantMap.get(p).map { case mutations =>
          val mutableCases = mutations.map(mutantToCase)

          // Continue deeper into the tree (without the currently placed mutants)
          val withDefaultsTransformed = PlaceableTree(p.tree.transformOnce(instrumentWithMutants(mutantMap - p)))
          val default = defaultCase(withDefaultsTransformed, mutations.map(_.id).toNonEmptyList)

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
                "This is likely an issue on Stryker4s's end, please take a look at the debug logs",
                e
              )
              throw UnableToBuildPatternMatchException(context.path)
          }
        }
      }
    }

    val newTree = Try(context.source.transformOnce(instrumentWithMutants(mutantMap))) match {
      case Success(tree)                  => tree
      case Failure(e: Stryker4sException) => throw e
      case Failure(e) =>
        log.error(s"Failed to instrument mutants in `${context.path}`.", e)
        throw new UnableToBuildPatternMatchException(context.path)
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

  /** Removes any mutants that are in the same range as a compile error
    */
  def attemptRemoveMutant(errors: NonEmptyList[CompilerErrMsg]): PartialFunction[Tree, Tree] = {
    // Match on mutation switching trees
    case tree: Term.Match if tree.expr.isEqual(options.mutationContext) =>
      // Filter out any cases that are in the same range as a compile error
      val newCases = tree.cases.filterNot(caze => errors.exists(compileErrorIsInCaseStatement(caze, _)))

      tree.copy(cases = newCases)
  }

  def mutantIdsForCompileErrors(tree: Tree, errors: NonEmptyList[CompilerErrMsg]) = {
    val mutationSwitchingCases = tree.collect {
      // Match on mutation switching trees
      case tree: Term.Match if tree.expr.isEqual(options.mutationContext) =>
        // Filter out default case as it's not mutated
        tree.cases.filterNot(c => c.pat.isEqual(p"_"))
    }.flatten

    errors
      .nonEmptyPartition(err =>
        mutationSwitchingCases
          .find(compileErrorIsInCaseStatement(_, err))
          .map(caze => extractMutantId(caze.pat) -> err)
          .toRight(err)
      ) match {
      case Both(a, b)   => (a.some, b.toList.toMap)
      case Ior.Left(a)  => (a.some, Map.empty[MutantId, CompilerErrMsg])
      case Ior.Right(b) => (none, b.toList.toMap)
    }
  }

  /** Extracts the mutant id from a case statement
    */
  private def extractMutantId(pat: Pat) = pat match {
    case Lit.Int(value) => MutantId(value)
    case Pat.Extract.After_4_6_0(Term.Name("Some"), Pat.ArgClause(List(Lit.String(value)))) =>
      MutantId(value.toInt)
    case _ => throw new IllegalArgumentException(s"Could not extract mutant id from '${pat.syntax}'")
  }

  /** Checks if the compile error is inside the mutant case statement
    */
  private def compileErrorIsInCaseStatement(caze: Case, error: CompilerErrMsg): Boolean = {
    (caze.pos.startLine + 1) <= error.line && (caze.pos.endLine + 1) >= error.line
  }
}
