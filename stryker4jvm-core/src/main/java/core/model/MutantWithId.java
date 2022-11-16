package core.model;

public record MutantWithId<T>(int id, MutatedCode<T> mutatedCode) {
    // todo: this function is available in scala but may not be required here
    /*
    def toMutantResult(status: MutantStatus, testsCompleted: Option[Int] = None, description: Option[String] = None) =
    MutantResult(
      id = id.toString(),
      mutatorName = mutatedCode.metadata.mutatorName,
      replacement = mutatedCode.metadata.replacement,
      location = mutatedCode.metadata.location,
      status = status,
      description = description,
      testsCompleted = testsCompleted
    )
     */
}
