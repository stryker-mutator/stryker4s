package stryker4s.mutants

import cats.data.NonEmptyVector
import cats.syntax.either.*
import cats.syntax.functor.*
import mutationtesting.Location
import stryker4s.extension.TreeExtensions.{IsEqualExtension, TransformOnceExtension}
import stryker4s.extension.mutationtype.*
import stryker4s.model.{MutantMetadata, MutatedCode, MutationExcluded, PlaceableTree}
import stryker4s.mutants.tree.{IgnoredMutations, Mutations}

import scala.annotation.tailrec
import scala.meta.*
import scala.meta.inputs.Position

trait Traverser {

  /** If the currently visiting node is a node where mutations can be placed, that node is returned, otherwise None
    */
  def canPlace(currentTree: Term, lastTopStatement: PlaceableTree): Option[Term]

  def findMutations: PartialFunction[Tree, PlaceableTree => Either[IgnoredMutations, Mutations]]

}

class TraverserImpl extends Traverser {

  def canPlace(currentTree: Term, lastTopStatement: PlaceableTree): Option[Term] =
    currentTree.parent.flatMap {
      case _: Defn.Def                                   => Some(currentTree.asInstanceOf[Term])
      case _: Term.Name                                  => None
      case t: Term.Assign                                => Some(t)
      case p if p.parent.contains(lastTopStatement.tree) => None
      case t: Term.Apply                                 => Some(t)
      case t: Term.ApplyInfix                            => Some(t)
      case t: Term.Block                                 => Some(t)
      case t: Term.If                                    => Some(t)
      case t: Term.ForYield                              => Some(t)
      case _                                             => None
    }

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
      val tree: Tree = replacement.tree.asInstanceOf[Tree]
      val metadata = MutantMetadata(original.syntax, tree.syntax, replacement.mutationName, toLocation(original.pos))
      val mutatedTopStatement = placeableTree.tree
        .transformExactlyOnce {
          case t if t.isEqual(original) =>
            tree
        }
        .getOrElse(throw new RuntimeException(s"Could not transform $original in ${placeableTree.tree}"))

      MutatedCode(mutatedTopStatement, metadata)
    }
    // TODO: filter out mutations excluded by config
    filterAnnotationExclusions(mutations, firstReplacement, original)
  }

  private def toLocation(pos: Position): Location = Location(
    start = mutationtesting.Position(line = pos.startLine + 1, column = pos.startColumn + 1),
    end = mutationtesting.Position(line = pos.endLine + 1, column = pos.endColumn + 1)
  )

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
