package stryker4s.mutants.tree

import cats.data.Ior.Both
import cats.data.{Ior, NonEmptyList, NonEmptyVector}
import cats.syntax.all.*
import stryker4s.exception.{Stryker4sException, UnableToBuildPatternMatchException}
import stryker4s.extension.TreeExtensions.{treeEq, AncestorsExtension, TransformOnceExtension}
import stryker4s.log.Logger
import stryker4s.model.*
import stryker4s.model.SourceReplacement.*

import scala.collection.immutable.SortedSet
import scala.meta.*

/** Instrument (place) mutants in a tree
  *
  * @param options
  *   Options for instrumenting a mutation switch, such as on what the mutation should be activated (like
  *   `sys.env.get("ACTIVE_MUTATION")`).
  */
class MutantInstrumenter(options: InstrumenterOptions)(implicit log: Logger) {

  def instrumentFile(context: SourceContext, mutantMap: Map[PlaceableTree, MutantsWithId]): MutatedFile = {

    // Rendered mutation switches for each outermost mutated statement, used to splice the file together
    val spliceReplacements = SortedSet.newBuilder[SourceReplacement]

    // Statements that are not inside another mutated statement. Nested mutated statements are rendered as part of their
    // outermost statement's mutation switch, so splicing them in separately would duplicate (overlapping) source ranges
    val outermostTrees = mutantMap.keySet.filterNot(
      _.tree.existsAncestorUpTo(context.source)(ancestor => mutantMap.contains(PlaceableTree(ancestor)))
    )

    def instrumentWithMutants(
        mutantMap: Map[PlaceableTree, MutantsWithId]
    ): PartialFunction[Tree, Tree] = {

      Function.unlift { originalTree =>
        val p = PlaceableTree(originalTree)
        mutantMap.get(p).map { case mutations =>
          val mutableCases = mutations.map(mutantToCase)

          // Continue deeper into the tree (without the currently placed mutants)
          val withDefaultsTransformed =
            PlaceableTree(p.tree.transformOnce(instrumentWithMutants(mutantMap - p)))
          val default = defaultCase(withDefaultsTransformed, mutations.map(_.id).toNonEmptyList)

          val cases = mutableCases :+ default

          val mutationSwitch = Either
            .catchNonFatal(buildMatch(cases))
            // Also done on the final tree, but has to happen here too: this is the tree that is recorded for splicing,
            // so a marker left inside it is reprinted into the file verbatim
            .map(removeDanglingEndMarkers)
            .valueOr { e =>
              log.error(
                s"Failed to instrument mutants in `${context.path}`. Original statement: [${originalTree.text}]"
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

          if (outermostTrees.contains(p))
            // A displaced `end` marker sits outside the replaced range, so widen over it
            spliceReplacements += SourceReplacement(
              p.tree.begOffset,
              displacedEndMarker(p.tree).fold(p.tree.endOffset)(_.endOffset),
              mutationSwitch
            )

          mutationSwitch
        }
      }
    }

    val newTree = Either
      .catchNonFatal(context.source.transformOnce(instrumentWithMutants(mutantMap)))
      .map(removeDanglingEndMarkers)
      .valueOr {
        case e: Stryker4sException => throw e
        case e                     =>
          log.error(s"Failed to instrument mutants in `${context.path}`.", e)
          throw new UnableToBuildPatternMatchException(context.path)
      }

    val mutations: MutantsWithId = mutantMap.map(_._2).toVector.toNev.get.flatten
    val splice = spliceReplacements.result().toNes.map(SourceSplice(context.source.pos.input.text, _))

    MutatedFile(context.path, newTree, mutations, splice)
  }

  /** Removes `end` markers that no longer directly follow the construct they close
    */
  private def removeDanglingEndMarkers[T <: Tree](tree: T): T = tree
    .transform {
      case b: Term.Block if hasEndMarker(b.stats)    => b.copyWithComments(stats = retainBoundEndMarkers(b.stats))
      case b: Template.Body if hasEndMarker(b.stats) => b.copyWithComments(stats = retainBoundEndMarkers(b.stats))
      case b: Ctor.Block if hasEndMarker(b.stats)    => b.copyWithComments(stats = retainBoundEndMarkers(b.stats))
      case b: Pkg.Body if hasEndMarker(b.stats)      => b.copyWithComments(stats = retainBoundEndMarkers(b.stats))
      case s: Source if hasEndMarker(s.stats)        => s.copyWithComments(stats = retainBoundEndMarkers(s.stats))
    }
    .asInstanceOf[T]

  private def hasEndMarker(stats: List[Stat]): Boolean = stats.exists(_.is[Term.EndMarker])

  /** The `end` marker directly following `tree`, displaced by the switch that replaces `tree`
    */
  private def displacedEndMarker(tree: Tree): Option[Term.EndMarker] = tree.parent
    .flatMap(statsOf)
    .flatMap(_.dropWhile(_ ne tree) match {
      case _ :: (marker: Term.EndMarker) :: _ if !stillBinds(marker) => marker.some
      case _                                                         => none
    })

  /** The direct statements of every tree that can hold an `end` marker as a sibling of a placeable statement
    */
  private def statsOf(tree: Tree): Option[List[Stat]] = tree match {
    case b: Term.Block    => b.stats.some
    case b: Template.Body => b.stats.some
    case b: Ctor.Block    => b.stats.some
    case b: Pkg.Body      => b.stats.some
    case s: Source        => s.stats.some
    case _                => none
  }

  /** Drops every `end` marker directly preceded by a mutation switch, keeping all others
    */
  private def retainBoundEndMarkers(stats: List[Stat]): List[Stat] = stats
    .foldLeft(List.empty[Stat]) {
      case (acc, marker: Term.EndMarker) if !stillBinds(marker) && acc.headOption.exists(isMutationSwitch) => acc
      case (acc, stat) => stat :: acc
    }
    .reverse

  /** `end match` still binds after its construct is replaced, as the mutation switch is itself a `Term.Match`
    */
  private def stillBinds(marker: Term.EndMarker): Boolean = marker.name.value == "match"

  private def isMutationSwitch(tree: Tree): Boolean = tree match {
    case t: Term.Match => t.expr === options.mutationContext
    case _             => false
  }

  def mutantToCase(mutant: MutantWithId): Case = {
    val newTree = mutant.mutatedCode.mutatedStatement

    Case(options.pattern(mutant.id.value), none, newTree)
  }

  def defaultCase(placeableTree: PlaceableTree, mutantIds: NonEmptyList[MutantId]): Case = {
    val term = placeableTree.tree.asInstanceOf[Term]
    Case(
      Pat.Wildcard(),
      None,
      options.coverageStatement.fold(term) { coverageFn =>
        val coverageTerm = coverageFn(mutantIds.map(_.value))
        // Create a block, or place it in the original
        term match {
          case t @ Term.Block(stats) => t.copyWithComments(coverageTerm :: stats)
          case term                  => Term.Block(List(coverageTerm, term))
        }
      }
    )
  }

  def buildMatch(cases: NonEmptyVector[Case]): Term.Match =
    Term.Match.After_4_4_5(options.mutationContext, cases.toList)

  /** Removes any mutants that are in the same range as a compile error
    */
  def attemptRemoveMutant(errors: NonEmptyList[CompilerErrMsg]): PartialFunction[Tree, Tree] = {
    case tree: Term.Match if isMutationSwitch(tree) =>
      // Filter out any cases that are in the same range as a compile error
      val newCases = tree.casesBlock.cases.filterNot(caze =>
        (caze.pat =!= Pat.Wildcard()) && errors.exists(compileErrorIsInCaseStatement(caze, _))
      )

      tree.copy(cases = newCases)
  }

  def mutantIdsForCompileErrors(tree: Tree, errors: NonEmptyList[CompilerErrMsg]) = {
    val mutationSwitchingCases: List[Case] = tree.collect {
      case tree: Term.Match if isMutationSwitch(tree) =>
        // Filter out default case as it's not mutated
        tree.casesBlock.cases.filterNot(_.pat === Pat.Wildcard())
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
    case Lit.Int(value) =>
      MutantId(value)
    case Member.Apply(Name("Some"), Pat.ArgClause(List(Lit.String(value)))) =>
      MutantId(value.toInt)
    case _ => throw new IllegalArgumentException(s"Could not extract mutant id from '${pat.text}'")
  }

  /** Checks if the compile error is inside the mutant case statement
    */
  private def compileErrorIsInCaseStatement(caze: Case, error: CompilerErrMsg): Boolean = {
    error.offset match {
      case Some(offset) => caze.begOffset <= offset && caze.endOffset >= offset
      case None         => (caze.pos.startLine + 1) <= error.line && (caze.pos.lastLine + 1) >= error.line
    }
  }
}
