package stryker4jvm.mutants.language

import stryker4jvm.core.model.{MutantWithId, MutatedCode}

import java.nio.file.Path
import stryker4jvm.model.IgnoredMutationReason

trait Parser[T] {
  def apply(path: Path): T
}

trait Collector[T] {
  /*
    Differences between Stryker4s:
        Mutations is Vector[MutatedCode] instead of NonEmptyVector[MutatedCode]
   */
  def apply(tree: T): (Vector[(MutatedCode[T], IgnoredMutationReason)], Map[T, Vector[MutatedCode[T]]])
}

trait Instrumenter[T] {
  def apply(source: T, mutants: Seq[MutantWithId[T]]): T
}

/* note: making this a case class makes it 'impossible' for other languages to use this without
 * including some scala library (serialization issues) */
class LanguageMutator[T <: AST](parser: Parser[T], collector: Collector[T], instrumenter: Instrumenter[T]) {
  type Tree = T
}
