package stryker4s.mutants.applymutants

class StatementTransformer {
  // def transformSource(source: Source, foundMutants: Seq[Mutant]): SourceTransformations = {
  //   val transformedMutants: Seq[TransformedMutants] = foundMutants
  //     .groupBy(mutant => mutant.original)
  //     .map { case (original, mutants) => transformMutant(original, mutants) }
  //     .toSeq
  //     .sortBy(_.mutantStatements.map(_.id.globalId).max)

  //   SourceTransformations(source, transformedMutants)
  // }

  // /** Transforms the statement in the original tree of the FoundMutant to the given mutations
  //   */
  // def transformMutant(original: Term, registered: Seq[Mutant]): TransformedMutants = {
  //   val topStatement = original.topStatement()

  //   val transformedMutants = registered.map { mutant =>
  //     val newMutated = transformStatement(topStatement, mutant.original, mutant.mutated)
  //     mutant.copy(original = topStatement, mutated = newMutated)
  //   }.toList

  //   TransformedMutants(topStatement, transformedMutants)
  // }

  // /** Transforms the statement to the given mutation
  //   */
  // def transformStatement(topStatement: Term, toMutate: Term, mutation: Term): Term =
  //   topStatement
  //     .transform {
  //       case foundTree: Term if foundTree.isEqual(toMutate) && foundTree.pos == toMutate.pos =>
  //         mutation
  //     }
  //     .asInstanceOf[Term]
}
