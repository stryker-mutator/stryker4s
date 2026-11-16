package stryker4s.model

import cats.Order
import cats.data.NonEmptySet
import fs2.Stream
import fs2.io.file.Path
import stryker4s.extension.TreeExtensions.PrintExtension
import stryker4s.mutants.tree.MutantsWithId

import scala.meta.{Dialect, Term, Tree}

final case class MutatedFile(
    fileOrigin: Path,
    mutatedSource: Tree,
    mutants: MutantsWithId,
    splice: Option[SourceSplice] = None
) {

  def mutatedSourceText[F[_]]: Stream[F, String] =
    splice.fold(Stream(mutatedSource.text))(_.render)
}

/** Segments needed to serialize a mutated file
  *
  * @param originalText
  *   the text of the original, unmutated source
  * @param dialect
  *   the dialect to print the mutation switches with
  * @param replacements
  *   the mutation switches to splice in, each replacing a range of the original source text
  */
final case class SourceSplice(
    originalText: String,
    dialect: Dialect,
    replacements: NonEmptySet[SourceReplacement]
) {

  /** Emits the file in parts:
    *
    *   1. each original slice preceding a mutation switch
    *   2. then the rendered switch
    *   3. finally the trailing original slice.
    */
  def render[F[_]]: Stream[F, String] = {
    val switches = Stream.emits(replacements.toSortedSet.toSeq).zipWithPrevious.flatMap { case (prev, r) =>
      val sliceStart = prev.fold(0)(_.endOffset)
      Stream.emit(originalText.substring(sliceStart, r.begOffset)) ++ Stream(r.tree.syntaxFor(dialect))
    }
    switches ++ Stream.emit(originalText.substring(replacements.last.endOffset))
  }
}

/** A mutation switch that replaces the original source in the half-open range `[begOffset, endOffset)`. */
final case class SourceReplacement(begOffset: Int, endOffset: Int, tree: Term)

object SourceReplacement {
  implicit val order: Order[SourceReplacement] = Order.by(_.begOffset)
  implicit val ordering: Ordering[SourceReplacement] = order.toOrdering
}
