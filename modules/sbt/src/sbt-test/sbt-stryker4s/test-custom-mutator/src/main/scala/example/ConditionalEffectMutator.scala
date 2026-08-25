package example

import cats.data.NonEmptyVector
import stryker4s.mutatorapi.*

import scala.meta.*

/** Example custom mutator: flips cats-effect conditional effect helpers (`IO.whenA`/`IO.unlessA`,
  * `IO.raiseWhen`/`IO.raiseUnless`).
  *
  * These helpers encode side-effecting boolean guards, so flipping the guard helper tests whether
  * the suite distinguishes "run this effect when the condition is true" from "run this effect when
  * the condition is false". This is a cats-effect-specific equivalent of a conditional-boundary
  * mutator, but targeted at pure-FP effect construction rather than imperative `if` statements.
  *
  * Purely syntactic: only matches two-argument-list calls on an `IO` receiver, such as
  * `IO.whenA(flag)(effect)` or `cats.effect.IO.raiseUnless(flag)(error)`.
  */
class ConditionalEffectMutator extends CustomMutator {
  def matcher: MutationMatcher = {
    case term @ Term.Apply.After_4_6_0(
          Term.Apply.After_4_6_0(Term.Select(receiver, Term.Name(name)), conditionArgs),
          effectArgs
        ) if isIoReceiver(receiver) && isConditionalEffectHelper(name) =>
      val replacementName = flippedName(name)
      val replacement = Term.Apply(
        Term.Apply(Term.Select(receiver, Term.Name(replacementName)), conditionArgs),
        effectArgs
      )
      mutate(term, replacement, name, replacementName)
  }

  private def isIoReceiver(receiver: Term): Boolean =
    receiver match {
      case Term.Name("IO") => true
      case Term.Select(_, Term.Name("IO")) => true
      case _ => false
    }

  private def flippedName(name: String): String =
    name match {
      case "whenA" => "unlessA"
      case "unlessA" => "whenA"
      case "raiseWhen" => "raiseUnless"
      case "raiseUnless" => "raiseWhen"
    }

  private def isConditionalEffectHelper(name: String): Boolean =
    name == "whenA" || name == "unlessA" || name == "raiseWhen" || name == "raiseUnless"

  private def mutate(term: Term, replacement: Term, originalName: String, replacementName: String)(
      placeableTree: PlaceableTree
  ): Either[IgnoredMutations, Mutations] = {
    val metadata =
      MutantMetadata(s".$originalName", s".$replacementName", "ConditionalEffect", term.pos, None)
    Right(NonEmptyVector.one(MutatedCode(replacement, metadata)))
  }
}
