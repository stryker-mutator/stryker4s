package stryker4jvm.core.model;

public final class MutantWithId<T> {
  /**
   * Unique identifier of the mutant. Can be used to switch between active mutants during runtime.
   */
  public final int id;
  /** The mutated code */
  public final MutatedCode<T> mutatedCode;

  public MutantWithId(int id, MutatedCode<T> mutatedCode) {
    this.id = id;
    this.mutatedCode = mutatedCode;
  }
}
