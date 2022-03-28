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
import stryker4s.config.Config

trait Traverser {

  /** If the currently visiting node is a node where mutations can be placed, that node is returned, otherwise None
    */
  def canPlace(currentTree: Tree): Option[Term]

  def findMutations: PartialFunction[Tree, PlaceableTree => Either[IgnoredMutations, Mutations]]

}

class TraverserImpl(implicit config: Config) extends Traverser {

  def canPlace(currentTree: Tree): Option[Term] = {
    val toPlace = currentTree match {
      case d: Defn.Def                                              => Some(d.body)
      case d: Defn.Val                                              => Some(d.rhs)
      case _: Term.Name                                             => None
      case t: Term.Match                                            => Some(t)
      case t: Case if t.cond.flatMap(_.find(currentTree)).isDefined => None
      case t: Term.Apply                                            => Some(t)
      case t: Term.ApplyInfix                                       => Some(t)
      case t: Term.Block                                            => Some(t)
      case t: Term.If                                               => Some(t)
      case t: Term.ForYield                                         => Some(t)
      case t: Lit                                                   => Some(t)
      case _                                                        => None
    }

    toPlace
      // Filter out all the node places that are invalid
      .filter {
        case name: Name => !name.isDefinition
        // Don't place inside `case` patterns or conditions
        case p if p.findParent[Case].exists(c => c.pat.exists(currentTree) || c.cond.exists(_.exists(currentTree))) =>
          false
        case t if t.parent.exists(_.is[Init])                              => false
        case t if t.parent.exists(p => p.is[Term] && p.isNot[Term.Select]) => false
        case ParentIsTypeLiteral()                                         => false
        case _                                                             => true
      }
      .filterNot(_.isIn[Mod.Annot])
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
    filterExclusions(mutations, firstReplacement, original)
  }

  private def filterExclusions(
      mutations: NonEmptyVector[MutatedCode],
      mutationType: Mutation[?],
      original: Tree
  ): Either[IgnoredMutations, Mutations] = {
    val mutationName = "stryker4s.mutation." + mutationType.mutationName

    if (excludedByConfig(mutationType.mutationName) || excludedByAnnotation(original, mutationName))
      mutations.tupleRight(MutationExcluded).asLeft
    else
      mutations.asRight
  }

  private def excludedByConfig(mutation: String): Boolean = config.excludedMutations.contains(mutation)

  @tailrec
  private def excludedByAnnotation(original: Tree, mutationName: String): Boolean = {
    import stryker4s.extension.TreeExtensions.*
    original.parent match {
      case Some(value) =>
        value.getMods.exists(isSupressWarningsAnnotation(_, mutationName)) || excludedByAnnotation(
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
