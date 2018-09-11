package stryker4s.mutants.findmutants
import stryker4s.model.Mutant

import scala.meta.Term

trait MutantCreator {
    private[this] val stream = Iterator.from(0)

    def create(original: Term, mutated: Term*): Seq[Mutant] = {
      mutated.map(mutant => Mutant(stream.next(), original, mutant))
    }
}
