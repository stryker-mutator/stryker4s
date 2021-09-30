package stryker4s.mutants

import fs2.io.file.Path
import stryker4s.model._
import stryker4s.mutants.applymutants.MatchBuilder

import scala.meta.{Case, Source, _}

class RollbackHandler(matchBuilder: MatchBuilder) {
  def rollbackNonCompilingMutants(
      file: Path,
      mutationsInSource: MutationsInSource,
      mutateFile: (Path, MutationsInSource) => MutatedFile,
      compileErrors: Seq[CompilerErrMsg]
  ): MutatedFile = {
    //If there are any compiler errors (i.e. we're currently retrying the mutation with the bad ones rolled back)
    //Then we take the original tree built that didn't compile
    //And then we search inside it to translate the compile errors to mutants
    //Finally we rebuild it from scratch without those mutants
    //This is not very performant, but you only pay the cost if there actually is a compiler error
    val originalFile = mutateFile(file, mutationsInSource)

    val errorsInThisFile = compileErrors.filter(err => file.toString.endsWith(err.path))
    val nonCompilingIds = errorsToIds(errorsInThisFile, originalFile.mutatedSource, mutationsInSource.mutants)
    val (nonCompilingMutants, compilingMutants) =
      mutationsInSource.mutants.partition(mut => nonCompilingIds.contains(mut.id))

    mutateFile(file, mutationsInSource.copy(mutants = compilingMutants)).copy(nonCompilingMutants = nonCompilingMutants)
  }

  //Given compiler errors, return the mutants that caused it by searching for the matching case statement at that line
  private def errorsToIds(
      compileErrors: Seq[CompilerErrMsg],
      mutatedFile: String,
      mutants: Seq[Mutant]
  ): Seq[MutantId] = {
    val statementToMutIdMap = mutants.map { mutant =>
      matchBuilder.mutantToCase(mutant).structure -> mutant.id
    }.toMap

    val lineToMutantId: Map[Int, MutantId] = mutatedFile
      //Parsing the mutated tree again as a string is the only way to get the position info of the mutated statements
      .parse[Source]
      .getOrElse(throw new RuntimeException(s"Failed to parse $mutatedFile to remove non-compiling mutants"))
      .collect {
        case node: Case if statementToMutIdMap.contains(node.structure) =>
          val mutId = statementToMutIdMap(node.structure)
          //+1 because scalameta uses zero-indexed line numbers
          (node.pos.startLine to node.pos.endLine).map(i => i + 1 -> mutId)
      }
      .flatten
      .toMap

    compileErrors.flatMap { err =>
      lineToMutantId.get(err.line)
    }
  }

}
