package stryker4s.run

import stryker4s.model.{FoundMutant, Mutant, RegisteredMutant}

class MutantRegistry {
  private val stream = Iterator.from(0)

  def registerMutant(foundMutant: FoundMutant): RegisteredMutant =
    RegisteredMutant(foundMutant.originalTree, foundMutant.mutations.map(mutation => {
      Mutant(stream.next(), foundMutant.originalTree, mutation)
    }))

}
