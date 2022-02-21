package stryker4s.mutants

import cats.data.NonEmptyVector
import cats.syntax.either.*
import cats.syntax.functor.*
import stryker4s.extension.TreeExtensions.{IsEqualExtension, TransformOnceExtension, *}
import stryker4s.extension.mutationtype.*
import stryker4s.model.{MutantMetadata, MutatedCode, MutationExcluded, PlaceableTree}
import stryker4s.mutants.tree.{IgnoredMutations, Mutations}

import scala.annotation.tailrec
import scala.meta.*

trait Traverser {

  /** If the currently visiting node is a node where mutations can be placed, that node is returned, otherwise None
    */
  def canPlace(currentTree: Term): Option[Term]

  def findMutations: PartialFunction[Tree, PlaceableTree => Either[IgnoredMutations, Mutations]]

}

class TraverserImpl extends Traverser {

  def canPlace(currentTree: Term): Option[Term] =
    if (currentTree.topStatement() == currentTree) Some(currentTree) else None
  // currentTree.parent.flatMap {
  //   case d: Defn.Def if d.body == currentTree => Some(d.body)
  //   case d: Defn.Val if d.rhs == currentTree  => Some(d.rhs)
  //   case t: Term.Assign                       => Some(t)
  //   case _: Term.Name                         => None
  //   case t: Term.Match                        => Some(t)
  //   case p
  //       if p.parent
  //         .collect { case c: Case => c }
  //         .exists(_.cond.contains(currentTree)) =>
  //     // .exists(caze => caze.parent.collect { case t: Term.Try => t }.exists(_.catchp.contains(caze))) =>
  //     None
  //   case t: Case if t.cond.flatMap(_.find(currentTree)).isDefined => None
  //   // case t: Case if t.parent.exists(_.is[Term]) => t.parent.asInstanceOf[Option[Term]]
  //   case p if p.parent.contains(lastTopStatement.tree) => None
  //   case t: Term.Apply                                 => Some(t)
  //   case t: Term.ApplyInfix                            => Some(t)
  //   case t: Term.Block                                 => Some(t)
  //   case t: Term.If                                    => Some(t)
  //   case t: Term.ForYield                              => Some(t)
  //   case t: Lit                                        => Some(t)
  //   case _                                             => None
  // }

  def findMutations: PartialFunction[Tree, PlaceableTree => Either[IgnoredMutations, Mutations]] = {
    case (EqualTo(orig))             => createMutations(orig)(NotEqualTo)
    case (GreaterThan(orig))         => createMutations(orig)(GreaterThanEqualTo, LesserThan, EqualTo)
    case (NonEmptyString(orig))      => createMutations(orig)(EmptyString)
    case (StringInterpolation(orig)) => createMutations(orig)(EmptyString)
  }

  private def createMutations[T <: Tree](
      original: Term
  )(
      firstReplacement: SubstitutionMutation[T],
      restReplacements: SubstitutionMutation[T]*
  ): PlaceableTree => Either[IgnoredMutations, Mutations] = { placeableTree =>
    val replacements: NonEmptyVector[SubstitutionMutation[T]] =
      NonEmptyVector(firstReplacement, restReplacements.toVector)

    val mutations = replacements.map { replacement =>
      val tree: Tree = replacement.tree
      val metadata = MutantMetadata(original.syntax, tree.syntax, replacement.mutationName, original.pos)
      val mutatedTopStatement = placeableTree.tree
        .transformExactlyOnce {
          case t if t.isEqual(original) =>
            tree
        }
        .getOrElse(
          throw new RuntimeException(s"Could not transform $original in ${placeableTree.tree} (${metadata.location})")
        )

      mutatedTopStatement match {
        case t: Term => MutatedCode(t, metadata)
        case _ =>
          throw new RuntimeException(
            s"Could not transform $original in ${placeableTree.tree} (${metadata.location}). Expected a term, but was ${mutatedTopStatement.getClass} at ${mutatedTopStatement.pos.startLine}:${mutatedTopStatement.pos.startColumn}"
          )
      }

    }
    // TODO: filter out mutations excluded by config
    filterAnnotationExclusions(mutations, firstReplacement, original)
  }

  private def filterAnnotationExclusions(
      mutations: NonEmptyVector[MutatedCode],
      mutationType: Mutation[?],
      original: Tree
  ): Either[IgnoredMutations, Mutations] = {
    val mutationName = "stryker4s.mutation." + mutationType.mutationName

    if (isSuppressedByAnnotation(original, mutationName))
      mutations.tupleRight(MutationExcluded).asLeft
    else
      mutations.asRight
  }

  @tailrec
  private def isSuppressedByAnnotation(original: Tree, mutationName: String): Boolean = {
    import stryker4s.extension.TreeExtensions.*
    original.parent match {
      case Some(value) =>
        value.getMods.exists(isSupressWarningsAnnotation(_, mutationName)) || isSuppressedByAnnotation(
          value,
          mutationName
        )
      case None => false
    }
  }

  private def isSupressWarningsAnnotation(mod: Mod, mutationName: String): Boolean = {
    mod match {
      case Mod.Annot(Init(Type.Name("SuppressWarnings"), _, List(List(Term.Apply(Name("Array"), params))))) =>
        params.exists {
          case Lit.String(`mutationName`) => true
          case _                          => false
        }
      case _ => false
    }
  }
}
