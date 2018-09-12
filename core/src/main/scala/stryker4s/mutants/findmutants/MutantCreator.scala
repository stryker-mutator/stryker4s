package stryker4s.mutants.findmutants
import stryker4s.model.Mutant

import scala.meta.Term

trait MutantCreator {
  private[this] val stream = Iterator.from(0)

  implicit class TermExtensions(original: Term) {
    def ~~>(mutated: Term*): Seq[Mutant] = {
      mutated.map(mutant => Mutant(stream.next(), original, mutant))
    }
  }
}
