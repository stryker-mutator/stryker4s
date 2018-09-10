package stryker4s.model

import scala.meta.Term

object MutantCreator {
  private[this] val stream = Iterator.from(0)

  def create(original: Term, mutated: Term*): Seq[Mutant] = {
    mutated.map(mutant => Mutant(stream.next(), original, mutant))
  }
}

case class Mutant(id: Int, original: Term, mutated: Term)
